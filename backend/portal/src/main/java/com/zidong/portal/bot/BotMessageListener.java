package com.zidong.portal.bot;

import com.zidong.dao.entity.BotCredential;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信 Bot 消息入口（多会话版，仅做收消息 + 幂等入库，业务处理交由 {@link BotMessageDispatcher}）。
 *
 * <p>一个 app 可被多个用户扫码绑定：每个有效凭证（{@code bot_credential} 一行）由独立的
 * 虚拟线程长轮询收消息，游标按行持久化到 {@code bot_credential.updates_buf}。
 * 巡检线程周期同步凭证列表：新扫码的会话自动开始轮询，失效/解绑的会话自动停止。</p>
 *
 * <p>多实例下：只有主节点（{@link BotLeaderElection#isLeader()}）才长轮询与入站，
 * 其余实例空转等待接管。主节点故障后，新 leader 按行接着上次游标继续拉，
 * 不会丢消息也不会从空游标重拉历史。</p>
 */
@Component
@RequiredArgsConstructor
public class BotMessageListener {

    private static final Logger log = LoggerFactory.getLogger(BotMessageListener.class);

    /** 等待成为 leader 的轮询间隔（毫秒） */
    private static final long WAIT_LEADER_MILLIS = 1_000;

    /** 等待登录态就绪的轮询间隔（毫秒） */
    private static final long CREDENTIALS_POLL_MILLIS = 3_000;

    /** 凭证列表巡检间隔（毫秒）：新扫码的会话最多延迟该时间开始收消息 */
    private static final long WATCH_INTERVAL_MILLIS = 5_000;

    /** 拉取消息异常后的重试退避时间（毫秒） */
    private static final long RETRY_BACKOFF_MILLIS = 3_000;

    private final BotApiHelper botApiHelper;
    private final BotLeaderElection leaderElection;
    private final BotMessageInbox inbox;
    private final BotLoginService botLoginService;
    private final BotCredentialStore credentialStore;

    private volatile boolean running = true;
    private volatile Thread watchThread;

    /** 在跑的长轮询线程：credentialId -> 线程 */
    private final Map<Long, Thread> workers = new ConcurrentHashMap<>();

    /** 是否正卡在「等待凭证」的休眠中：绑定成功后据此唤醒巡检 */
    private volatile boolean waitingCredentials = false;

    @PostConstruct
    public void start() {
        // 失去 leader 时立刻打断全部长轮询，重新成为 leader 后由巡检拉起
        leaderElection.onLoseLeadership(this::interruptAllWorkers);
        watchThread = Thread.ofVirtual().name("bot-credential-watcher").start(this::watchLoop);
        log.info("Bot 消息监听已启动（多会话巡检模式）");
    }

    @PreDestroy
    public void stop() {
        running = false;
        interruptWatcher();
        interruptAllWorkers();
        log.info("Bot 消息监听已停止");
    }

    // ==================== 巡检循环 ====================

    /**
     * 巡检循环：leader 期间周期比对「共享存储里的有效凭证」与「在跑的 worker」，
     * 新凭证拉起长轮询线程、失效/解绑的凭证打断退出。
     */
    private void watchLoop() {
        while (running) {
            try {
                if (!leaderElection.isLeader()) {
                    interruptAllWorkers();
                    sleep(WAIT_LEADER_MILLIS);
                    continue;
                }
                if (botApiHelper.credentialCount() == 0 && !credentialStore.hasValid()) {
                    waitingCredentials = true;
                    // 确实没有可用凭证：由主节点自动发起扫码（非主节点在这里直接返回，不会抢着登录）
                    botLoginService.loginIfNecessary();
                    log.info("未获取到任何 Bot 登录凭证，等待扫码（{}ms 后重试）", CREDENTIALS_POLL_MILLIS);
                    sleep(CREDENTIALS_POLL_MILLIS);
                    continue;
                }
                waitingCredentials = false;
                syncWorkers();
                sleep(WATCH_INTERVAL_MILLIS);
            } catch (Exception e) {
                if (!running) {
                    return;
                }
                log.warn("凭证巡检异常，{}ms 后重试", WATCH_INTERVAL_MILLIS, e);
                sleep(WATCH_INTERVAL_MILLIS);
            }
        }
    }

    /**
     * 每轮只查一次库：逐行按需恢复客户端（幂等，已就绪的会话不重建），
     * 再为没有长轮询线程的会话拉起线程；失效/解绑的凭证打断其线程退出。
     */
    private void syncWorkers() {
        List<BotCredential> validList = credentialStore.loadValid();
        for (BotCredential credential : validList) {
            Long id = credential.getId();
            if (id == null) {
                continue;
            }
            // 恢复会话客户端：内存里已就绪时内部直接跳过，不会重复建连接
            botApiHelper.restore(credential);
            if (workers.containsKey(id)) {
                continue;
            }
            String persistedBuf = credential.getUpdatesBuf();
            Thread thread = Thread.ofVirtual()
                    .name("bot-message-listener-" + id)
                    .start(() -> pollLoop(id, persistedBuf));
            workers.put(id, thread);
            log.info("Bot 会话长轮询已启动: credentialId={}, ilinkBotId={}, appId={}",
                    id, credential.getIlinkBotId(), credential.getAppId());
        }
        // 有效列表里已经没有的凭证：打断其线程并移出记录（线程在 finally 里自然收尾）
        workers.keySet().removeIf(id -> {
            boolean stillValid = validList.stream().anyMatch(c -> id.equals(c.getId()));
            if (!stillValid) {
                Thread worker = workers.get(id);
                if (worker != null) {
                    worker.interrupt();
                }
                log.info("Bot 会话长轮询已停止（凭证失效或已解绑）: credentialId={}", id);
            }
            return !stillValid;
        });
    }

    // ==================== 单会话长轮询 ====================

    /**
     * 单个凭证的长轮询循环：游标独立维护并按行持久化（仅 leader 写入），
     * 失去 leader / 凭证失效 / 会话过期时退出，由巡检线程重新评估。
     */
    private void pollLoop(Long credentialId, String persistedBuf) {
        String updatesBuf = persistedBuf == null ? "" : persistedBuf;
        try {
            while (running && leaderElection.isLeader() && workers.containsKey(credentialId)) {
                try {
                    BotApiHelper.UpdatesPage page = botApiHelper.pollUserMessages(credentialId, updatesBuf);
                    String nextBuf = page.nextBuf();
                    if (nextBuf != null && !nextBuf.equals(updatesBuf)) {
                        updatesBuf = nextBuf;
                        credentialStore.saveUpdatesBuf(credentialId, nextBuf);
                    }
                    for (BotApiHelper.BotMessage msg : page.messages()) {
                        handleMessage(credentialId, msg);
                    }
                } catch (BotSessionExpiredException e) {
                    // token 已失效：清掉内存与共享存储里的凭证，本线程退出，等待重新扫码；
                    // 只清内存不够——下次 restore 还会把旧 token 读回来，形成无限重试。
                    log.warn("Bot 会话已过期，清除凭证并等待重新登录: credentialId={}", credentialId, e);
                    botApiHelper.invalidateCredentials(credentialId);
                    return;
                } catch (BotCredentialInvalidException e) {
                    // 凭证已不可用（如 ret=null）：标记无效 + 清凭证 + 退出本会话的监听线程。
                    // invalid=1 后启动/恢复都会跳过该 token；重新扫码成功写入新 token 时会重置。
                    log.warn("Bot 凭证已失效（{}），标记无效并移除该会话的监听: credentialId={}",
                            e.getMessage(), credentialId);
                    credentialStore.markInvalid(credentialId);
                    botApiHelper.invalidateCredentials(credentialId);
                    return;
                } catch (Exception e) {
                    if (!running) {
                        return;
                    }
                    // 中断（失去 leader / 解绑 / 停机）会以 InterruptedIOException 包装出现，
                    // 由这里统一按「短暂异常」处理；循环条件会在下一轮判断是否退出
                    log.warn("拉取用户消息异常: credentialId={}，{}ms 后重试",
                            credentialId, RETRY_BACKOFF_MILLIS, e);
                    sleep(RETRY_BACKOFF_MILLIS);
                }
            }
        } finally {
            workers.remove(credentialId);
        }
    }

    /** 单条入站处理：仅做空文本过滤 + 幂等入库，不在这里跑业务 */
    private void handleMessage(Long credentialId, BotApiHelper.BotMessage inbound) {
        if (inbound.text() == null || inbound.text().isBlank()) {
            return;
        }
        boolean inserted = inbox.saveIfAbsent(inbound);
        if (!inserted) {
            log.debug("消息已入库过，跳过重复投递: from={}, messageId={}",
                    inbound.fromUserId(), inbound.messageId());
        } else {
            log.info("入站消息已入库: credentialId={}, from={}, messageId={}, text={}",
                    credentialId, inbound.fromUserId(), inbound.messageId(), inbound.text());
        }
    }

    // ==================== 对外控制 ====================

    /**
     * 确保巡检线程在跑：绑定时由 {@link BotQrcodeService} 调用。
     * <p>幂等：线程还活着就什么都不做；异常退出（未到 {@link #stop()}）时重新拉起。</p>
     */
    public synchronized void ensureRunning() {
        if (!running) {
            return;
        }
        if (watchThread == null || !watchThread.isAlive()) {
            watchThread = Thread.ofVirtual().name("bot-credential-watcher").start(this::watchLoop);
            log.info("Bot 凭证巡检线程已拉起（绑定时确保监听就绪）");
        }
    }

    /**
     * 唤醒等待凭证的巡检线程：扫码确认后凭证刚写入，尽快为新会话启动长轮询。
     * <p>只在「等待凭证」时打断，正常巡检不受影响。</p>
     */
    public void wakeUp() {
        if (waitingCredentials) {
            interruptWatcher();
        }
    }

    /**
     * 打断全部长轮询与巡检：解绑后凭证已清除，继续用旧 token 拉取只会拿到「未登录」异常。
     * <p>打断后巡检线程会重新比对凭证列表，回到等待扫码状态。</p>
     */
    public void interruptNow() {
        interruptWatcher();
        interruptAllWorkers();
    }

    private void interruptAllWorkers() {
        workers.values().forEach(Thread::interrupt);
    }

    private void interruptWatcher() {
        if (watchThread != null) {
            watchThread.interrupt();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // 中断只用于打断阻塞，是否继续由 running 与 isLeader 决定
        }
    }
}
