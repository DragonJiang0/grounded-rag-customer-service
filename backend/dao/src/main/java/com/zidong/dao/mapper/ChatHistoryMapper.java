package com.zidong.dao.mapper;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.dto.ChatHistoryItemDto;
import com.zidong.dao.dto.ChatHistoryPageQueryDto;
import com.zidong.dao.entity.ChatHistory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天记录。
 *
 * <p>一个会话（conversation_id）可能因为 history 超出 TEXT 上限被拆成多行，
 * 行内用 {@code seq} 表示先后：1 为第一段，之后依次递增。所有「按会话」的读写
 * 都要么落在最新一段（追加入库），要么按 seq 合并（列表查询）。
 */
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    /** history 中 createtime 的纯文本格式 */
    DateTimeFormatter HISTORY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");



    /**
     * 按会话标识取最新一段记录（seq 最大的一行）。
     *
     * <p>一个 taskId（conversationId）可能对应多行分段：history 写满后新建 seq + 1 的行，
     * 追加永远落在最新一段上，所以这里按 seq 倒序取第一条。
     */
    default ChatHistory findByConversationId(String conversationId) {
        return selectOne(Wrappers.<ChatHistory>lambdaQuery()
                .eq(ChatHistory::getConversationId, conversationId)
                .orderByDesc(ChatHistory::getSeq)
                .orderByDesc(ChatHistory::getId)
                .last("LIMIT 1"));
    }

    /** 当前会话已用到的最大分段序号；没有记录时返回 null */
    default Integer selectMaxSeq(String conversationId) {
        List<Object> seqs = selectObjs(Wrappers.<ChatHistory>lambdaQuery()
                .select(ChatHistory::getSeq)
                .eq(ChatHistory::getConversationId, conversationId)
                .orderByDesc(ChatHistory::getSeq)
                .last("LIMIT 1"));
        if (seqs == null || seqs.isEmpty() || seqs.get(0) == null) {
            return null;
        }
        return ((Number) seqs.get(0)).intValue();
    }

    /**
     * 向 history 追加一条消息：
     * 先把当前实体的 history 反序列化为 DTO 数组，追加新 DTO 后再序列化，最后回写 history 字段。
     *
     * <p>序列化结果超过 {@code maxChars} 时<b>不落库</b>，直接返回 false：单条 UPDATE 虽然原子
     * （失败时行内容不会变），但让库去承担一次必然失败的写入，既刷错误日志、又依赖
     * sql_mode=严格；非严格模式下会被静默截断，反而留下半截 JSON。调用方拿到 false 后
     * 另起一条只装本条消息的分段行即可。
     *
     * <p>这里只比字符数，不自己算字节：TEXT 的 65535 是<b>字节</b>上限，而同样的文本在不同
     * 字符集下字节数不同（utf8mb4 ≤4 字节/字符，utf8 ≤3，latin1 为 1）。折算成字符数的
     * 工作（按最坏字符集 + 冗余）由调用方统一给出，避免这里按某一字符集算错。
     *
     * @param chatHistory 当前聊天记录实体（需含 id 与 history）
     * @param item        待追加的消息，createtime 为空时自动填充当前时间
     * @param maxChars    单行 history 允许的最大字符数，≤ 0 表示不限制
     * @return true 已写入；false 超限未写入，调用方应改为新建分段
     */
    default boolean appendHistory(ChatHistory chatHistory, ChatHistoryItemDto item, int maxChars) {
        List<ChatHistoryItemDto> items = new ArrayList<>();
        // 以库中最新 history 为基准追加：实体内存值可能已过期（本轮 insert 后又被追加过），
        // 直接用内存值重算会把之前写入的消息覆盖掉（如已写入的 user 被 assistant 覆盖）
        String history = chatHistory.getHistory();
        if (chatHistory.getId() != null) {
            ChatHistory latest = selectById(chatHistory.getId());
            if (latest != null) {
                history = latest.getHistory();
            }
        }
        if (history != null && !history.isBlank()) {
            List<ChatHistoryItemDto> parsed = JSON.parseArray(history, ChatHistoryItemDto.class);
            if (parsed != null) {
                items.addAll(parsed);
            }
        }

        if (item.getCreateTime() == null || item.getCreateTime().isBlank()) {
            item.setCreateTime(LocalDateTime.now().format(HISTORY_TIME_FORMATTER));
        }
        items.add(item);

        String newHistory = JSON.toJSONString(items);
        // 先比长度再写：超限就放弃这次追加，库里原值保持不动（无需"备份 + 还原"）
        if (maxChars > 0 && newHistory.length() > maxChars) {
            return false;
        }

        LambdaUpdateWrapper<ChatHistory> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(ChatHistory::getId, chatHistory.getId())
                .set(ChatHistory::getHistory, newHistory);
        update(updateWrapper);
        // 回写实体，保证同一流程内后续 append 拿到的是最新值，而不是过期的内存值
        chatHistory.setHistory(newHistory);
        return true;
    }

    /**
     * 按应用 + 渠道 + 标签查询聊天记录，不分页，创建时间倒序（最近活跃的会话在前）。
     *
     * <p>标签统一承载「来源接入实体 ID」：API 访问为接入 id、免密窗口为 {@code app_access_window.id}、
     * 账号密码登录窗口 / 微信个人号据此隔离同一应用下不同接入实体的会话；
     * 调试渠道的标签为 null。tag 传 null 时表示不按标签过滤。
     * 已由用户侧删除（customer_deleted = true）的记录不再返回。
     *
     * <p>返回的是「分段行」而不是会话：同一个 conversationId 可能出现多行（seq 递增），
     * 需要由调用方交给 {@code ChatHistorySegmentMerger} 按 seq 合并成一条会话后返回页面。
     *
     * @param appId   应用 ID
     * @param channel 来源渠道，见 {@code ChatHistoryService.CHANNEL_*}
     * @param tag     标签（来源接入实体 ID，如登录窗口 id）；为 null 时不按标签过滤
     */
    default List<ChatHistory> selectListByChannelAndTag(Long appId, Integer channel, Long tag) {
        return selectList(Wrappers.<ChatHistory>lambdaQuery()
                .eq(ChatHistory::getAppId, appId)
                .eq(ChatHistory::getChannel, channel)
                .eq(tag != null, ChatHistory::getTag, tag)
                .eq(ChatHistory::getCustomerDeleted, false)
                .orderByDesc(ChatHistory::getCreatedAt)
                .orderByDesc(ChatHistory::getId));
    }

    /**
     * 用户侧删除某入口下的会话（软删除）：把该会话的<b>所有分段</b>都置为 customer_deleted = true。
     *
     * <p>按「应用 + 渠道 + 标签 + 会话标识」定位，四个条件缺一不可：应用 / 渠道 / 标签保证只能删除
     * 当前入口可见的会话，会话标识（conversationId）直接整段覆盖该会话的所有分段，
     * 不再先用单行 id 反查，避免拿到分段 id 时漏删。不物理删除数据。
     *
     * @param appId          应用 ID
     * @param channel        来源渠道
     * @param tag            标签（来源接入实体 ID，如登录窗口 id）
     * @param conversationId 会话标识
     * @return 受影响行数，0 表示会话不存在或不属于当前入口
     */
    default int markCustomerDeletedByConversationId(Long appId, Integer channel, Long tag, String conversationId) {
        return update(null, Wrappers.<ChatHistory>lambdaUpdate()
                .set(ChatHistory::getCustomerDeleted, true)
                .eq(ChatHistory::getAppId, appId)
                .eq(ChatHistory::getChannel, channel)
                .eq(ChatHistory::getTag, tag)
                .eq(ChatHistory::getConversationId, conversationId));
    }

    /**
     * 按应用 + 会话标识把该会话的<b>所有分段</b>都置为 customer_deleted = true。
     *
     * <p>会话标识即 conversationId（taskId），一个会话可能对应多行分段，按标识整段置删除；
     * 带上应用条件，保证只能删除归属该应用的会话，不物理删除数据。
     *
     * @param appId          应用 ID
     * @param conversationId 会话标识
     * @return 受影响行数，0 表示会话不存在或不属于该应用
     */
    default int markCustomerDeletedByConversationId(Long appId, String conversationId) {
        return update(null, Wrappers.<ChatHistory>lambdaUpdate()
                .set(ChatHistory::getCustomerDeleted, true)
                .eq(ChatHistory::getAppId, appId)
                .eq(ChatHistory::getConversationId, conversationId));
    }

    /** 清空某应用的全部聊天记录：删除应用时共用 */
    default int deleteByAppId(Long appId) {
        return delete(Wrappers.<ChatHistory>lambdaQuery()
                .eq(ChatHistory::getAppId, appId));
    }

    /**
     * 分页查询命中的会话总数（按 conversation_id 去重，一个会话算一条）。
     *
     * <p>库里按分段存，页面按会话展示，所以分页要落在「会话」而不是「行」上，
     * 否则同一会话的分段会被拆到不同页、total 也会虚高。
     *
     * @param appId 应用 ID（必填，来自 URL）
     * @param query 查询条件：渠道、开始 / 结束时间，均可为空表示不限
     */
    @Select("<script>"
            + "SELECT COUNT(DISTINCT conversation_id) FROM chat_history WHERE app_id = #{appId}"
            + "<if test='query.channel != null'> AND channel = #{query.channel}</if>"
            + "<if test='query.startTime != null'> AND created_at &gt;= #{query.startTime}</if>"
            + "<if test='query.endTime != null'> AND created_at &lt;= #{query.endTime}</if>"
            + "</script>")
    long countConversationByQuery(@Param("appId") Long appId, @Param("query") ChatHistoryPageQueryDto query);

    /**
     * 按会话分页：先取当前页的 conversation_id（最近的会话在前）。
     *
     * @param appId  应用 ID
     * @param query  查询条件：渠道、开始 / 结束时间，均可为空表示不限
     * @param offset 跳过条数
     * @param size   每页条数
     * @return 当前页的会话标识列表，顺序即页面展示顺序
     */
    @Select("<script>"
            + "SELECT conversation_id FROM chat_history WHERE app_id = #{appId}"
            + "<if test='query.channel != null'> AND channel = #{query.channel}</if>"
            + "<if test='query.startTime != null'> AND created_at &gt;= #{query.startTime}</if>"
            + "<if test='query.endTime != null'> AND created_at &lt;= #{query.endTime}</if>"
            + " GROUP BY conversation_id ORDER BY MAX(id) DESC LIMIT #{offset}, #{size}"
            + "</script>")
    List<String> selectConversationIdsByQuery(@Param("appId") Long appId,
                                              @Param("query") ChatHistoryPageQueryDto query,
                                              @Param("offset") long offset,
                                              @Param("size") long size);

    /** 按会话标识批量取全部分段行，供内存合并使用 */
    default List<ChatHistory> selectSegmentsByConversationIds(Long appId, List<String> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<ChatHistory>lambdaQuery()
                .eq(ChatHistory::getAppId, appId)
                .in(ChatHistory::getConversationId, conversationIds)
                .orderByAsc(ChatHistory::getConversationId)
                .orderByAsc(ChatHistory::getSeq)
                .orderByAsc(ChatHistory::getId));
    }
}
