package com.zidong.portal.bot;

import com.zidong.dao.entity.BotMessageTask;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息消费调度：从 {@code bot_message} 抢任务并交给 {@link BotMessageHandler}。
 *
 * <p>与监听分离后，收消息仍然单例（微信长轮询的硬约束），但处理过程具备
 * 「不丢消息 + 可重试 + 并发可控」的能力：
 * <ul>
 *     <li>抢占：{@link BotMessageInbox#claim(String)} 用原子 UPDATE 保证一条消息只会被一个实例处理；</li>
 *     <li>并发：{@code bot.dispatcher.maxConcurrent} 控制本实例同时处理的条数（默认 1，保持原来的串行语义）；</li>
 *     <li>失败：记录原因并把状态回落为待重试，超过 {@code maxRetry} 后不再抢占；</li>
 *     <li>卡死恢复：成为 leader 时以及运行期间定期把长时间无进展的任务重置回待处理。</li>
 * </ul>
 *
 * <p>现阶段默认 {@code leaderOnly=true}：回复用户要走已登录的微信客户端，而只有 leader 持有登录态。
 * 后续若把发送能力抽成共享服务（或接入 MQ + 统一发送端），把该配置置为 false 即可让多实例并行消费。</p>
 */
@Component
@RequiredArgsConstructor
public class BotMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(BotMessageDispatcher.class);

    /** 卡死任务扫描的最小间隔：与 stuckSeconds 挂钩，避免无意义的全表扫描 */
    private static final long MIN_RESET_INTERVAL_MILLIS = 60_000;

    private final BotApiHelper botApiHelper;
    private final BotMessageInbox inbox;
    private final BotLeaderElection leaderElection;
    private final BotMessageHandler handler;
    private final BotProperties botProperties;

    /** 本实例正在处理的消息数 */
    private final AtomicInteger activeCount = new AtomicInteger(0);

    private volatile boolean running = true;
    private volatile Thread dispatchThread;
    private volatile long lastStuckResetAtMillis = 0L;

    /** 启动消费循环 */
    @PostConstruct
    public void start() {
        dispatchThread = Thread.ofVirtual().name("bot-message-dispatcher").start(this::dispatchLoop);
        log.info("Bot 消息消费调度已启动（leaderOnly={}, maxConcurrent={}）",
                botProperties.getDispatcher().isLeaderOnly(), botProperties.getDispatcher().getMaxConcurrent());
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (dispatchThread != null) {
            dispatchThread.interrupt();
        }
        log.info("Bot 消息消费调度已停止");
    }

    private void dispatchLoop() {
        while (running) {
            try {
                if (!canConsume()) {
                    sleep(botProperties.getDispatcher().getPollIntervalMillis());
                    continue;
                }
                maybeResetStuck();
                if (activeCount.get() >= botProperties.getDispatcher().getMaxConcurrent()) {
                    // 并发已满，短暂等待后重试
                    sleep(200);
                    continue;
                }
                BotMessageTask task = inbox.claim(leaderElection.instanceId());
                if (task == null) {
                    sleep(botProperties.getDispatcher().getPollIntervalMillis());
                    continue;
                }
                activeCount.incrementAndGet();
                Thread.ofVirtual().name("bot-message-task-" + task.getId()).start(() -> run(task));
            } catch (Exception e) {
                log.warn("扫描待处理消息异常", e);
                sleep(botProperties.getDispatcher().getPollIntervalMillis());
            }
        }
    }

    private void run(BotMessageTask task) {
        BotApiHelper.BotMessage inbound = BotMessageInbox.toBotMessage(task);
        try {
            handler.handle(inbound);
            inbox.markDone(task.getId());
            log.info("消息处理完成: id={}, from={}", task.getId(), task.getFromUserId());
        } catch (Exception e) {
            log.error("消息处理失败: id={}, from={}", task.getId(), task.getFromUserId(), e);
            inbox.markFailed(task.getId(), String.valueOf(e.getMessage()), leaderElection.instanceId());
            replyErrorQuietly(inbound, e);
        } finally {
            activeCount.decrementAndGet();
        }
    }

    /** 只有 leader 能消费时，非 leader 实例空转等待（leader 掉线后自动接管） */
    private boolean canConsume() {
        return !botProperties.getDispatcher().isLeaderOnly() || leaderElection.isLeader();
    }

    /** 定期把卡死的任务重置回待处理：leader 刚接手时立即执行一次 */
    private void maybeResetStuck() {
        long now = System.currentTimeMillis();
        if (now - lastStuckResetAtMillis < MIN_RESET_INTERVAL_MILLIS) {
            return;
        }
        lastStuckResetAtMillis = now;
        int reset = inbox.resetStuck();
        if (reset > 0) {
            log.info("重置卡死的消息任务 {} 条", reset);
        }
    }

    /** 处理异常时告知用户（按该消息所属的机器人会话回复）；回复失败不能影响任务状态流转 */
    private void replyErrorQuietly(BotApiHelper.BotMessage inbound, Exception cause) {
        try {
            botApiHelper.replyTextByBot(inbound, "处理过程中出现异常，请稍后重试。");
        } catch (Exception e) {
            log.warn("异常回复发送失败（可能未持有该会话的登录态）", e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // 中断只用于打断等待，是否继续由 running 决定
        }
    }
}
