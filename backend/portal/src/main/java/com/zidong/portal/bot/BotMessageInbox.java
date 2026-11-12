package com.zidong.portal.bot;

import com.zidong.dao.entity.BotMessageTask;
import com.zidong.dao.mapper.BotMessageTaskMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 入站消息的收件箱：负责「幂等入库」与「跨实例抢占」。
 *
 * <p>多实例下重复处理有两个来源，这里都在 DB 层挡掉：
 * <ol>
 *     <li>多个实例同时长轮询，或 leader 切换后新实例从旧游标重拉——靠 {@code uk_dedupe_key} 唯一键；</li>
 *     <li>多个实例同时消费同一条待处理消息——靠带 {@code status} 条件的原子 {@code UPDATE} 抢占。</li>
 * </ol>
 *
 * <p>取代了原先 {@code BotMessageListener} 里的进程内 {@code AtomicBoolean}：
 * 它只能在单个 JVM 内串行，多实例各跑各的，等于没有限流。</p>
 */
@Service
@RequiredArgsConstructor
public class BotMessageInbox {

    private static final Logger log = LoggerFactory.getLogger(BotMessageInbox.class);

    /** 幂等键前缀：有微信消息 ID 时用它，最可靠 */
    private static final String DEDUPE_PREFIX_MESSAGE_ID = "m:";

    /** 幂等键前缀：没有消息 ID 时退化为内容哈希 */
    private static final String DEDUPE_PREFIX_HASH = "h:";

    private final BotMessageTaskMapper botMessageTaskMapper;
    private final BotProperties botProperties;

    /**
     * 幂等入库。
     *
     * @return true 表示新消息（可以处理）；false 表示已收过（重复投递，直接丢弃）
     */
    public boolean saveIfAbsent(BotApiHelper.BotMessage inbound) {
        LocalDateTime now = LocalDateTime.now();
        BotMessageTask task = BotMessageTask.builder()
                .dedupeKey(dedupeKey(inbound))
                .messageId(inbound.messageId())
                .fromUserId(inbound.fromUserId())
                .toUserId(inbound.toUserId())
                .messageType(inbound.messageType())
                .messageState(inbound.messageState())
                .contextToken(inbound.contextToken())
                .content(inbound.text())
                .status(BotMessageTask.STATUS_PENDING)
                .retryCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            return botMessageTaskMapper.insert(task) > 0;
        } catch (DuplicateKeyException e) {
            log.debug("消息已入库，跳过重复投递: dedupeKey={}", task.getDedupeKey());
            return false;
        }
    }

    /**
     * 抢占一条待处理消息（含失败未达上限的重试）。
     *
     * @param owner 抢占者实例 ID
     * @return 抢占到的消息，没有待处理的返回 {@code null}
     */
    public BotMessageTask claim(String owner) {
        BotMessageTask candidate = botMessageTaskMapper.findOnePending(botProperties.getDispatcher().getMaxRetry());
        if (candidate == null) {
            return null;
        }
        int expectedStatus = candidate.getStatus() == null
                ? BotMessageTask.STATUS_PENDING : candidate.getStatus();
        if (botMessageTaskMapper.claim(candidate.getId(), expectedStatus, owner) <= 0) {
            // 被其它实例抢先一步，下一轮再取
            return null;
        }
        candidate.setStatus(BotMessageTask.STATUS_RUNNING);
        candidate.setOwner(owner);
        return candidate;
    }

    /** 标记处理成功 */
    public void markDone(Long id) {
        botMessageTaskMapper.markDone(id);
    }

    /** 标记处理失败：次数 +1，未达上限则回落到待重试 */
    public void markFailed(Long id, String errorMsg, String owner) {
        String safeError = errorMsg == null ? null : (errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg);
        botMessageTaskMapper.markFailed(id, safeError, owner);
    }

    /**
     * 重置卡死任务：处理中但长时间无进展（持有者崩溃/被摘除），由新 leader 重新处理。
     *
     * @return 重置条数
     */
    public int resetStuck() {
        return botMessageTaskMapper.resetStuck(botProperties.getDispatcher().getStuckSeconds());
    }

    /**
     * 解绑时重置全部处理中任务：回到待处理。
     *
     * @return 重置条数
     */
    public int resetRunning() {
        return botMessageTaskMapper.resetRunning();
    }

    /** 还原成消息视图，交给业务 handler（字段与入站时保持一致，避免 handler 拿到 null） */
    public static BotApiHelper.BotMessage toBotMessage(BotMessageTask task) {
        return new BotApiHelper.BotMessage(task.getMessageId(), task.getFromUserId(), task.getToUserId(),
                task.getMessageType(), task.getMessageState(), task.getContextToken(), task.getContent());
    }

    /**
     * 幂等键：优先用微信侧 message_id；缺失时用「用户 + 上下文令牌 + 内容」的哈希兜底，
     * 保证同一条消息在任何实例上算出同一个键（唯一索引不允许多个 NULL，不能直接拿 message_id 做键）。
     * <p>用 MD5（UUID.nameUUIDFromBytes）而不是 {@code hashCode()}：后者只有 32 位，
     * 消息量上来后碰撞概率不可忽略，一旦碰撞就会把不同消息误判成重复而丢消息。</p>
     */
    private static String dedupeKey(BotApiHelper.BotMessage inbound) {
        if (inbound.messageId() != null) {
            return DEDUPE_PREFIX_MESSAGE_ID + inbound.messageId();
        }
        String raw = inbound.fromUserId() + "|" + inbound.contextToken() + "|" + inbound.text();
        return DEDUPE_PREFIX_HASH + UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
    }
}
