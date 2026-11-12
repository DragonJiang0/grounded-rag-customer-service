package com.zidong.portal.bot;

import com.zidong.dao.mapper.BotMessageTaskMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * {@code bot_message} 已完成记录的定期清理：每天执行一次，删除处理完成超过保留天数的行。
 *
 * <p>表里一行消息走完状态机后长期留存在 {@code status = 2}（已完成），
 * 只增不减会无限膨胀，因此按 {@code updated_at}（完成时间）做保留期清理。</p>
 *
 * <p>删除条件本身是幂等的：重复执行（多实例、重试、手动触发）只会删到 0 行，
 * 不需要额外的锁或执行记录。</p>
 */
@Component
@RequiredArgsConstructor
public class BotMessageCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(BotMessageCleanupJob.class);

    /** 已完成记录保留天数：超过该天数的记录会被删除 */
    private static final int RETENTION_DAYS = 7;

    private final BotMessageTaskMapper botMessageTaskMapper;

    /** 每天 03:00 清理一次已完成的 Bot 消息记录 */
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Shanghai")
    public void cleanDoneMessages() {
        try {
            int deleted = botMessageTaskMapper.deleteDoneBefore(RETENTION_DAYS);
            if (deleted > 0) {
                log.info("已清理完成超过 {} 天的 Bot 消息记录: {} 条", RETENTION_DAYS, deleted);
            }
        } catch (Exception e) {
            // 吞掉异常，避免单次失败影响后续调度
            log.error("清理 Bot 消息记录失败，等待下次调度重试", e);
        }
    }
}
