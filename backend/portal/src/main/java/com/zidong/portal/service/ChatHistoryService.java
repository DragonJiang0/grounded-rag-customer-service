package com.zidong.portal.service;

import com.alibaba.fastjson2.JSON;
import com.zidong.common.exception.BizException;
import com.zidong.dao.dto.ChatHistoryItemDto;
import com.zidong.dao.entity.ChatHistory;
import com.zidong.dao.mapper.ChatHistoryMapper;
import com.zidong.dao.support.ChatHistorySegmentMerger;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话记录的统一入口：谁要记对话只调本类，不直接碰 {@link ChatHistoryMapper}。
 *
 * <p>记录拆成两步，与「用户提问」「模型回复」两个发生时间点一一对应：
 * <ul>
 *     <li>{@link #begin}：消息进来时建会话行并落 user 消息；</li>
 *     <li>{@link #recordAssistant}：消息结束时落 assistant 消息。</li>
 * </ul>
 *
 * <p>调用时机由入口决定（阻塞用返回值、流式用 sink 终态），但落库动作只有这一份，避免各入口各写一套。
 */
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    /** 来源渠道：调试（manage 经 gRPC 的 debugPrompt 入口） */
    public static final int CHANNEL_DEBUG = 0;

    /** 来源渠道：API 访问（/api 的阻塞与流式入口） */
    public static final int CHANNEL_API = 1;

    /** 来源渠道：免登陆窗口（/window 免密直达入口） */
    public static final int CHANNEL_WINDOW = 2;

    /** 来源渠道：账号密码登录窗口 */
    public static final int CHANNEL_ACCOUNT = 3;

    /** 来源渠道：微信个人号 */
    public static final int CHANNEL_WECHAT_PERSONAL = 4;

    /** 来源渠道：飞书机器人 */
    public static final int CHANNEL_FEISHU = 5;

    /** 来源渠道：钉钉机器人 */
    public static final int CHANNEL_DINGTALK = 6;

    /** 来源渠道：企业微信机器人 */
    public static final int CHANNEL_WECOM = 7;

    /** 来源渠道：微信公众号 */
    public static final int CHANNEL_WECHAT_OA = 8;

    private static final String ROLE_USER = "user";

    private static final String ROLE_ASSISTANT = "assistant";

    /** history 字段的初始值：空 JSON 数组，保证 appendHistory 反序列化时不落 NPE */
    private static final String EMPTY_HISTORY = "[]";

    /** 分段序号从 1 开始：1 即会话的第一段 */
    private static final int SEQ_FIRST = 1;

    /** 新建分段的插入重试次数：同会话并发写满时可能撞到 (conversation_id, seq) 唯一键 */
    private static final int SEGMENT_INSERT_RETRY = 3;

    /** MySQL TEXT 列的字节上限：65535 字节，与字符集无关 */
    private static final int TEXT_MAX_BYTES = 65535;

    /** 一个字符在最坏字符集下的字节数：utf8mb4 单字符最多 4 字节，按它折算最保守 */
    private static final int MAX_BYTES_PER_CHAR = 4;

    /** 冗余：字符集换算、JSON 转义、驱动编码等差异的余量（8KB） */
    private static final int HISTORY_MARGIN_BYTES = 8 * 1024;

    /**
     * 单行 history 允许的最大字符数：(65535 - 8KB) / 4 ≈ 14335。
     *
     * <p>为什么不直接用字节数：同样的文本在 utf8mb4（≤4 字节/字符）、utf8（≤3）、latin1（1）
     * 下字节数完全不同，一旦列或连接字符集变化，按当前字符集算出来的字节数就可能偏小、
     * 写入被截断。这里统一按最坏情况 4 字节/字符折算成<b>字符数</b>再留 8KB 冗余，
     * 任何字符集下都不会写超。
     */
    private static final int MAX_HISTORY_CHARS = (TEXT_MAX_BYTES - HISTORY_MARGIN_BYTES) / MAX_BYTES_PER_CHAR;

    /** title 列 varchar(255)：超长同样会报 Data too long，入库前先截断 */
    private static final int TITLE_MAX_LENGTH = 255;

    /** 单条消息超长被裁剪后的结尾提示 */
    private static final String TRUNCATED_SUFFIX = "\n...(内容过长，已截断)";

    /** MySQL 的 Data too long 异常类名：不直接引依赖，只按类名识别 */
    private static final String MYSQL_DATA_TRUNCATION = "com.mysql.cj.jdbc.exceptions.MysqlDataTruncation";

    /** MySQL 字符串截断的 SQLState / 错误码 */
    private static final String MYSQL_DATA_TOO_LONG_SQL_STATE = "22001";

    private static final int MYSQL_DATA_TOO_LONG_ERROR_CODE = 1406;

    private static final int MYSQL_DUPLICATE_KEY_ERROR_CODE = 1062;

    private static final String DATA_TOO_LONG_MARK = "Data too long";

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryService.class);

    private final ChatHistoryMapper chatHistoryMapper;

    /**
     * 开始记录一次问答：建会话行并落 user 消息。
     *
     * @param taskId   会话标识，作为 conversationId 落库
     * @param appId    所属应用 ID，可为 null（如调试入口未指定应用时）
     * @param channel  来源渠道，见 {@link #CHANNEL_API} / {@link #CHANNEL_DEBUG}
     * @param tag      标签：来源接入实体 ID，取值规则见 {@link #normalizeTag}
     * @param question 用户原始提问
     * @return 会话实体（含自增 id），供 {@link #recordAssistant} 继续追加
     */
    public ChatHistory begin(String taskId, Long appId, Integer channel, Long tag, String question) {
        // 同一 taskId 复用一个会话：可能已被拆成多段，取 seq 最大的一段继续追加
        ChatHistory chatHistory = chatHistoryMapper.findByConversationId(taskId);
        if (chatHistory == null) {
            log.debug("会话记录不存在，新建: taskId={}, appId={}, channel={}", taskId, appId, channel);
            chatHistory = ChatHistory.builder()
                    .conversationId(taskId)
                    .appId(appId)
                    .title(limitTitle(question))
                    .channel(channel)
                    .tag(normalizeTag(channel, tag))
                    .customerDeleted(false)
                    // 第一段固定 seq = 1，后续 history 写满时新建 seq + 1 的分段行
                    .seq(SEQ_FIRST)
                    .history(EMPTY_HISTORY)
                    .createdAt(LocalDateTime.now())
                    .build();
            chatHistoryMapper.insert(chatHistory);
            log.debug("会话记录已创建: id={}, taskId={}", chatHistory.getId(), taskId);
        } else {
            log.debug("会话记录已存在，追加到最新分段: id={}, seq={}, taskId={}",
                    chatHistory.getId(), chatHistory.getSeq(), taskId);
        }
        append(chatHistory, ROLE_USER, question);
        return chatHistory;
    }

    /**
     * 落模型回复。会话为空（未开始记录）时直接跳过。
     *
     * @param chatHistory {@link #begin} 的返回值，可为 null
     * @param answer      模型最终答复
     */
    public void recordAssistant(ChatHistory chatHistory, String answer) {
        if (chatHistory == null) {
            log.debug("记录模型回复: 会话为空，跳过");
            return;
        }
        log.debug("记录模型回复: id={}, taskId={}", chatHistory.getId(), chatHistory.getConversationId());
        append(chatHistory, ROLE_ASSISTANT, answer);
    }

    /**
     * 查询免登录窗口的会话列表：按应用 + 渠道（免登陆窗口）+ 标签过滤，创建时间倒序，不分页。
     *
     * <p>标签为来源接入实体的主键 ID，免密窗口即 {@code app_access_window.id}，
     * 用于隔离同一应用下不同免密窗口的会话。
     *
     * @param appId 当前免密窗口所属应用 ID
     * @param tag   标签（免密窗口 id）
     * @return 会话记录列表
     */
    public List<ChatHistory> listWindowHistory(Long appId, Long tag) {
        // 库里按分段存，返回页面前在内存里按 seq 合并回一条会话，接口结构不变
        return ChatHistorySegmentMerger.merge(
                chatHistoryMapper.selectListByChannelAndTag(appId, CHANNEL_WINDOW, tag));
    }

    /**
     * 查询账号密码登录窗口的会话列表：按应用 + 渠道（账号密码登录窗口）+ 标签过滤，
     * 创建时间倒序，不分页。
     *
     * <p>标签为来源接入实体的主键 ID，登录窗口即 {@code app_password_window.id}，
     * 用于隔离同一应用下不同登录窗口的会话。
     *
     * @param appId 当前登录窗口所属应用 ID
     * @param tag   标签（登录窗口 id）
     * @return 会话记录列表
     */
    public List<ChatHistory> listAccountHistory(Long appId, Long tag) {
        // 同免密窗口：先取全部分段，再按 seq 合并，页面仍是一个会话一行
        return ChatHistorySegmentMerger.merge(
                chatHistoryMapper.selectListByChannelAndTag(appId, CHANNEL_ACCOUNT, tag));
    }

    /**
     * 用户侧删除免密窗口的某条会话：按「应用 + 会话标识」整段软删除。
     *
     * <p>只把 customer_deleted 置为 true，不做物理删除。
     * 应用归属只能来自当前免密窗口（windowKey 已由 WindowFilter 校验并还原），不接受调用方传入的 AppKey；
     * 两个条件都必须具备：appId 界定归属，conversationId 定位会话。
     *
     * @param appId          当前免密窗口所属应用 ID，必填
     * @param conversationId 会话标识（taskId），必填
     * @throws BizException appId 为空（未认证）或会话标识为空时抛出
     */
    public void deleteWindowHistory(Long appId, String conversationId) {
        if (appId == null) {
            throw BizException.unauthorized("未认证或认证已过期");
        }
        if (conversationId == null || conversationId.isBlank()) {
            throw BizException.badRequest("会话标识不能为空");
        }
        chatHistoryMapper.markCustomerDeletedByConversationId(appId, conversationId);
    }

    /**
     * 用户侧删除账号密码登录窗口的某条会话：按「应用 + 窗口 id + 会话标识」整段软删除。
     *
     * <p>只把 customer_deleted 置为 true，不做物理删除。
     * 应用归属与窗口隔离只能来自当前登录窗口（登录令牌已由 LoginedWindowFilter 校验并还原），
     * 不接受调用方传入；四个条件缺一不可：appId / channel / tag 界定可见范围，conversationId 定位会话。
     *
     * @param appId          当前登录窗口所属应用 ID，必填
     * @param tag            标签（登录窗口 id），必填
     * @param conversationId 会话标识（taskId），必填
     * @throws BizException 窗口身份缺失或会话标识为空时抛出
     */
    public void deleteAccountHistory(Long appId, Long tag, String conversationId) {
        if (appId == null || tag == null) {
            throw BizException.unauthorized("未认证或认证已过期");
        }
        if (conversationId == null || conversationId.isBlank()) {
            throw BizException.badRequest("会话标识不能为空");
        }
        chatHistoryMapper.markCustomerDeletedByConversationId(appId, CHANNEL_ACCOUNT, tag, conversationId);
    }

    /**
     * 统一归一化对话记录的标签：渠道决定标签含义，入口传什么都在这里收口。
     *
     * <ul>
     *     <li>调试渠道：无接入实体，固定 null；</li>
     *     <li>免密窗口渠道：{@code app_access_window.id}；</li>
     *     <li>账号密码登录窗口：{@code app_password_window.id}；</li>
     *     <li>微信个人号：{@code bot_credential.id}；</li>
     *     <li>其它渠道（如 API 访问）：沿用调用方传入的接入实体 id。</li>
     * </ul>
     *
     * @param channel 来源渠道，可为 null
     * @param tag     调用方传入的标签，可为 null
     * @return 归一化后要落库的标签
     */
    private static Long normalizeTag(Integer channel, Long tag) {
        if (channel == null) {
            return tag;
        }
        return switch (channel) {
            case CHANNEL_DEBUG -> null;
            default -> tag;
        };
    }

    /**
     * 追加一条消息。
     *
     * <p>history 是 TEXT（65535 字节）。追加前先序列化算出字符数，超过 {@link #MAX_HISTORY_CHARS}
     * 就<b>不写库</b>（不复用"写失败再回滚"的套路：单条 UPDATE 本身原子，超长会整条失败，
     * 库里原值不会变成半截），改为<b>新建一条 seq + 1 的分段行</b>只保存本条消息，
     * 本轮对话不丢；查询时由 {@link ChatHistorySegmentMerger} 按 seq 合并，页面看到的仍是一条完整会话。
     *
     * <p>同时兜住 MySQL 的 {@code Data too long}：预判用的字节数与实际列上限不一致（列被改小等）时，
     * 仍按异常识别后新建分段。
     *
     * <p>其它异常照旧向上抛，由 {@code AgentExecutor} 统一兜住，不影响已经返回给客户端的答复。
     */
    private void append(ChatHistory chatHistory, String role, String content) {
        ChatHistoryItemDto item = ChatHistoryItemDto.builder()
                .role(role)
                .content(content)
                .build();
        // 新建分段是直接整行插入的，时间戳必须在这里补上（追加到已有行时 mapper 也会兜一次）
        if (item.getCreateTime() == null || item.getCreateTime().isBlank()) {
            item.setCreateTime(ChatHistoryMapper.HISTORY_TIME_FORMATTER.format(LocalDateTime.now()));
        }

        // 永远追加到最新一段：同一会话可能已被前面的请求拆成多段
        ChatHistory segment = resolveLatestSegment(chatHistory);
        if (segment != null) {
            try {
                // 先比长度再写：超限时库里原值保持不动（单条 UPDATE 本身原子，无需备份还原），
                // 直接落到下面新建只装本条消息的分段行
                if (chatHistoryMapper.appendHistory(segment, item, MAX_HISTORY_CHARS)) {
                    adopt(chatHistory, segment);
                    return;
                }
                log.warn("history 超出字段上限，改为新建分段保存: conversationId={}, id={}, seq={}",
                        segment.getConversationId(), segment.getId(), segment.getSeq());
            } catch (RuntimeException ex) {
                // 兜底：预判长度与实际列上限不一致（如列被改小）时仍按异常识别
                if (!isDataTooLong(ex)) {
                    throw ex;
                }
                log.warn("history 超出字段上限，改为新建分段保存: conversationId={}, id={}, seq={}",
                        segment.getConversationId(), segment.getId(), segment.getSeq(), ex);
            }
        }

        ChatHistory newSegment = createSegment(chatHistory, item);
        adopt(chatHistory, newSegment);
        log.info("已新建会话分段: conversationId={}, id={}, seq={}",
                newSegment.getConversationId(), newSegment.getId(), newSegment.getSeq());
    }

    /** 取会话最新一段：内存对象可能过期，以库为准；查不到时回落到行 id */
    private ChatHistory resolveLatestSegment(ChatHistory chatHistory) {
        String conversationId = chatHistory.getConversationId();
        if (conversationId != null && !conversationId.isBlank()) {
            ChatHistory latest = chatHistoryMapper.findByConversationId(conversationId);
            if (latest != null) {
                return latest;
            }
        }
        return chatHistory.getId() == null ? null : chatHistoryMapper.selectById(chatHistory.getId());
    }

    /** 新建分段：只装本条消息，seq 取当前最大值 + 1；并发撞号时顺延序号重试 */
    private ChatHistory createSegment(ChatHistory chatHistory, ChatHistoryItemDto item) {
        int seq = nextSeq(chatHistory.getConversationId());
        for (int attempt = 1; ; attempt++) {
            ChatHistory segment = ChatHistory.builder()
                    .conversationId(chatHistory.getConversationId())
                    .appId(chatHistory.getAppId())
                    // 标题只属于第一段，合并时取第一个非空标题，分段自身不再重复写入
                    .title(null)
                    .history(JSON.toJSONString(List.of(shrink(item))))
                    .createdAt(LocalDateTime.now())
                    .channel(chatHistory.getChannel())
                    .tag(chatHistory.getTag())
                    .customerDeleted(Boolean.TRUE.equals(chatHistory.getCustomerDeleted()))
                    .seq(seq)
                    .build();
            try {
                chatHistoryMapper.insert(segment);
                return segment;
            } catch (RuntimeException ex) {
                // 同会话并发写满时可能撞到唯一键 (conversation_id, seq)
                if (attempt >= SEGMENT_INSERT_RETRY || !isDuplicateKey(ex)) {
                    throw ex;
                }
                log.warn("新建会话分段撞号，序号顺延重试: conversationId={}, seq={}",
                        chatHistory.getConversationId(), seq);
                seq++;
            }
        }
    }

    /** 下一段的序号：没有历史分段时从 1 开始 */
    private int nextSeq(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return SEQ_FIRST;
        }
        Integer maxSeq = chatHistoryMapper.selectMaxSeq(conversationId);
        return maxSeq == null ? SEQ_FIRST : maxSeq + 1;
    }

    /** 把追加目标切到最新分段，保证同一轮里 begin 之后的 recordAssistant 落在同一段上 */
    private void adopt(ChatHistory chatHistory, ChatHistory segment) {
        chatHistory.setId(segment.getId());
        chatHistory.setConversationId(segment.getConversationId());
        chatHistory.setHistory(segment.getHistory());
        chatHistory.setSeq(segment.getSeq());
    }

    /** title 列是 varchar(255)，超长会报 Data too long，先按字符数截断 */
    private static String limitTitle(String question) {
        if (question == null || question.length() <= TITLE_MAX_LENGTH) {
            return question;
        }
        return question.substring(0, TITLE_MAX_LENGTH);
    }

    /**
     * 单条消息本身就可能超过 TEXT 上限（超长答复），此时新建分段也写不进去：
     * 按字符数逐步裁剪尾部并加提示，保证本轮对话不丢，而不是整条记录写失败。
     *
     * <p>判定标准与 {@link #MAX_HISTORY_CHARS} 一致：序列化后的字符数（已按最坏字符集折算）超限才裁剪。
     */
    private static ChatHistoryItemDto shrink(ChatHistoryItemDto item) {
        String content = item.getContent() == null ? "" : item.getContent();
        item.setContent(content);
        int limit = content.length();
        while (limit > 0 && JSON.toJSONString(List.of(item)).length() > MAX_HISTORY_CHARS) {
            limit = (int) (limit * 0.8);
            // 不要从代理对中间切开，避免产生半个 emoji
            if (limit > 0 && Character.isHighSurrogate(content.charAt(limit - 1))) {
                limit--;
            }
            item.setContent(content.substring(0, limit) + TRUNCATED_SUFFIX);
        }
        return item;
    }

    /** 是否 MySQL 的「Data too long」：驱动异常类名、SQLState 22001、错误码 1406，或消息关键字 */
    private static boolean isDataTooLong(Throwable ex) {
        for (Throwable current = ex; current != null; current = nextCause(current)) {
            if (MYSQL_DATA_TRUNCATION.equals(current.getClass().getName())) {
                return true;
            }
            if (current instanceof SQLException sqlException
                    && (MYSQL_DATA_TOO_LONG_SQL_STATE.equals(sqlException.getSQLState())
                    || sqlException.getErrorCode() == MYSQL_DATA_TOO_LONG_ERROR_CODE)) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains(DATA_TOO_LONG_MARK)) {
                return true;
            }
        }
        return false;
    }

    /** 是否唯一键冲突（并发新建分段撞上 (conversation_id, seq)） */
    private static boolean isDuplicateKey(Throwable ex) {
        for (Throwable current = ex; current != null; current = nextCause(current)) {
            if (current instanceof SQLException sqlException
                    && sqlException.getErrorCode() == MYSQL_DUPLICATE_KEY_ERROR_CODE) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains("Duplicate entry")) {
                return true;
            }
        }
        return false;
    }

    /** 取下一层 cause，自引用时返回 null，避免死循环 */
    private static Throwable nextCause(Throwable current) {
        Throwable cause = current.getCause();
        return cause == current ? null : cause;
    }
}
