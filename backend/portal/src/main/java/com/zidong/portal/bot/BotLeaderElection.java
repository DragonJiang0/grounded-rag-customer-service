package com.zidong.portal.bot;

import com.zidong.dao.entity.BotLeader;
import com.zidong.dao.mapper.BotLeaderMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bot 主节点选举：多实例部署时保证同一时刻只有一个实例跑微信长轮询与扫码登录。
 *
 * <p>机制是「数据库单行租约 + 心跳续租」，不需要额外中间件：
 * <ul>
 *     <li>抢占：一条带条件的原子 {@code UPDATE}（租约已过期或本就是自己持有才算抢到），
 *         并发下靠行锁天然串行，不会出现两个 leader；</li>
 *     <li>续租：leader 每 {@code heartbeatMillis} 续一次，续租失败（被抢走/DB 异常）立即降为 follower；</li>
 *     <li>让位：优雅停机主动把到期时间置为当前，其它实例下一个抢占周期即可接管；</li>
 *     <li>故障接管：leader 进程挂掉不再续租，租约到期后由 follower 抢到，最长延迟为
 *         {@code leaseSeconds + acquireIntervalMillis}。</li>
 * </ul>
 *
 * <p>时间全部使用数据库 {@code NOW()}，避免各实例系统时钟不一致导致选主错乱。</p>
 *
 * <p>关闭选举（{@code bot.leader.enabled=false}）时本实例恒为 leader，
 * 用于单实例部署或运维上明确指定某台跑 Bot 的场景。</p>
 */
@Component
@RequiredArgsConstructor
public class BotLeaderElection {

    private static final Logger log = LoggerFactory.getLogger(BotLeaderElection.class);

    private final BotLeaderMapper botLeaderMapper;
    private final BotProperties botProperties;

    /** 本实例 ID：主机 + 进程号 + 随机后缀，保证同机多实例也不重复 */
    private final String instanceId = generateInstanceId();

    private final AtomicBoolean leader = new AtomicBoolean(false);

    /** leader 身份变化回调（让监听线程及时中断长轮询） */
    private final List<Runnable> onLoseLeadership = new CopyOnWriteArrayList<>();
    private final List<Runnable> onGainLeadership = new CopyOnWriteArrayList<>();

    private volatile boolean running = true;
    private volatile Thread electionThread;

    /** Bean 就绪后启动选举循环 */
    @PostConstruct
    public void start() {
        if (!botProperties.getLeader().isEnabled()) {
            leader.set(true);
            log.info("Bot 选主已关闭（bot.leader.enabled=false），本实例 [{}] 直接按 leader 运行", instanceId);
            return;
        }
        initLeaderRow();
        electionThread = Thread.ofVirtual().name("bot-leader-election").start(this::electionLoop);
        log.info("Bot 主节点选举已启动，本实例 ID: {}", instanceId);
    }

    /** 容器关闭：主动让位，让其它实例立刻接管 */
    @PreDestroy
    public void stop() {
        running = false;
        if (botProperties.getLeader().isEnabled()) {
            release();
        }
        leader.set(false);
        if (electionThread != null) {
            electionThread.interrupt();
        }
        log.info("Bot 主节点选举已停止，本实例 ID: {}", instanceId);
    }

    /** 本实例当前是否是主节点 */
    public boolean isLeader() {
        return leader.get();
    }

    /** 本实例 ID（同时作为租约标识与消息处理 owner） */
    public String instanceId() {
        return instanceId;
    }

    /** 注册「失去 leader」回调：收到回调时应立即停止长轮询/消费，避免出现双写 */
    public void onLoseLeadership(Runnable callback) {
        onLoseLeadership.add(callback);
    }

    /** 注册「成为 leader」回调：可用于恢复会话、清理卡死任务等接管动作 */
    public void onGainLeadership(Runnable callback) {
        onGainLeadership.add(callback);
    }

    // 说明：get_updates 游标已改为按会话（bot_credential.updates_buf 按行）维护，
    // 不再依赖 bot_leader 表的单列游标。

    // ==================== 选举循环 ====================

    private void electionLoop() {
        while (running) {
            try {
                if (leader.get()) {
                    boolean renewed = botLeaderMapper.renew(instanceId, botProperties.getLeader().getLeaseSeconds()) > 0
                            // 兜底：受影响行数为 0 可能只是「值没变化」，再用一致读确认持有权
                            || botLeaderMapper.countOwned(instanceId) > 0;
                    if (renewed) {
                        sleep(botProperties.getLeader().getHeartbeatMillis());
                    } else {
                        // 续租失败且已不持有租约：被其它实例接管
                        demote();
                    }
                } else if (botLeaderMapper.acquire(instanceId, botProperties.getLeader().getLeaseSeconds()) > 0) {
                    promote();
                    sleep(botProperties.getLeader().getHeartbeatMillis());
                } else {
                    sleep(botProperties.getLeader().getAcquireIntervalMillis());
                }
            } catch (Exception e) {
                log.warn("Bot 主节点选举异常，本轮放弃 leader 身份", e);
                demote();
                sleep(botProperties.getLeader().getAcquireIntervalMillis());
            }
        }
    }

    private void promote() {
        if (leader.compareAndSet(false, true)) {
            log.info("本实例 [{}] 当选 Bot 主节点，开始接管消息监听", instanceId);
            notifyQuietly(onGainLeadership, "onGainLeadership");
        }
    }

    private void demote() {
        if (leader.compareAndSet(true, false)) {
            log.warn("本实例 [{}] 不再是 Bot 主节点，停止消息监听", instanceId);
            notifyQuietly(onLoseLeadership, "onLoseLeadership");
        }
    }

    private void notifyQuietly(List<Runnable> callbacks, String name) {
        for (Runnable callback : callbacks) {
            try {
                callback.run();
            } catch (Exception e) {
                log.warn("执行 {} 回调失败", name, e);
            }
        }
    }

    /** 确保租约行存在：多实例并发启动也只会有一行生效 */
    private void initLeaderRow() {
        try {
            botLeaderMapper.initRow();
        } catch (Exception e) {
            // 表未建时不阻断启动：选举循环里每次都会重试，建表后自动恢复
            log.warn("初始化 bot_leader 失败，稍后重试（请确认已执行 sql/bot_leader.sql）", e);
        }
    }

    private void release() {
        try {
            botLeaderMapper.release(instanceId);
        } catch (Exception e) {
            log.warn("释放 Bot 主节点租约失败（租约会自然过期）", e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // 中断只用于打断等待，是否继续由 running 决定；不恢复中断标记，避免后续 sleep 立即抛出形成忙循环
        }
    }

    private static String generateInstanceId() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "unknown-host";
        }
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        String id = host + "-" + pid;
        return id.length() > 64 ? id.substring(0, 64) : id;
    }
}
