package com.zidong.portal.bot;

import com.zidong.dao.entity.BotLeader;
import com.zidong.dao.mapper.BotLeaderMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理台扫码绑定 / 解绑（多会话版）：把「页面点一下」翻译成主节点上的扫码流程。
 *
 * <p>一个 app 可被多个用户扫码绑定：每次扫码确认后按 {@code ilink_bot_id} 幂等写入
 * {@code bot_credential}（同一用户重复扫码更新已有行，不同用户各自成行），
 * appId 由创建二维码时传入、随会话在确认环节落库。</p>
 *
 * <p>为什么只能在主节点做：
 * <ul>
 *     <li>同一 bot 账号重复扫码会让旧 token 失效，多实例各自扫码等于互相踢下线；</li>
 *     <li>绑定成功后要立刻开始收消息，而消息监听只有主节点在跑（见 {@link BotMessageListener}）。</li>
 * </ul>
 *
 * <p>二维码只在本进程内维护：{@code qrcode -> 会话状态}。
 * 页面轮询状态时可能打到任意实例，因此「是否已登录」一律读共享存储 {@code bot_credential}，
 * 不依赖当前实例的内存。</p>
 */
@Service
@RequiredArgsConstructor
public class BotQrcodeService {

    /** 等待扫码：二维码已生成，用户还没确认 */
    public static final String STATUS_WAITING = "WAITING";

    /** 已确认：凭证已写入共享存储，监听实例准备开始收消息 */
    public static final String STATUS_CONFIRMED = "CONFIRMED";

    /** 二维码过期：需要重新生成 */
    public static final String STATUS_EXPIRED = "EXPIRED";

    /** 等待超时：超过配置的扫码时限仍未确认 */
    public static final String STATUS_TIMEOUT = "TIMEOUT";

    /** 未知令牌：进程重启或二维码不是本实例生成 */
    public static final String STATUS_UNKNOWN = "UNKNOWN";

    /** 结束的会话保留时长：便于页面拿到最终状态后再清理 */
    private static final long SESSION_RETAIN_MILLIS = 10 * 60 * 1000L;

    private static final Logger log = LoggerFactory.getLogger(BotQrcodeService.class);

    private final BotApiHelper botApiHelper;
    private final BotLeaderElection leaderElection;
    private final BotProperties botProperties;
    private final BotMessageInbox inbox;
    private final BotMessageListener listener;
    private final BotCredentialStore credentialStore;
    private final BotLeaderMapper botLeaderMapper;

    /** 进程内的扫码会话：外部轮询令牌 -> 状态 */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /** 生成二维码串行化：避免连点/并发请求刷出多个二维码，互相顶掉 */
    private final Object createLock = new Object();

    // ==================== 创建二维码 ====================

    /**
     * 生成登录二维码：每次生成新码，旧的待扫码会话随即失效（刷新语义）。
     *
     * @param appId 要绑定的应用 ID：扫码成功后该机器人收到的消息按此应用触发 embabel 回答
     * @throws BotNotLeaderException 本实例不是主节点
     */
    public BotQrcode create(Long appId) {
        requireLeader();
        synchronized (createLock) {
            // 先取码、再作废旧码：取码失败不该把仍在用的旧码也废掉
            BotApiHelper.BotQrcode qrcode = botApiHelper.fetchQrcode();
            supersedeWaitingSessions();
            purgeEndedSessions();

            long expireAtMillis = System.currentTimeMillis()
                    + botProperties.getLogin().getScanTimeoutSeconds() * 1000L;
            Session session = new Session(qrcode.qrcode(), qrcode.qrcodeImgContent(),
                    expireAtMillis, appId);
            sessions.put(session.qrcode, session);

            // 扫码确认是阻塞轮询，放到虚拟线程里跑，接口立刻返回二维码给页面展示
            Thread.ofVirtual().name("bot-qr-wait-" + session.qrcode).start(() -> awaitConfirm(session));

            // 绑定的同时确保「处理监听的实例」在跑：主节点的巡检线程未启动/已退出时幂等拉起
            listener.ensureRunning();

            log.info("已生成 Bot 登录二维码: qrcode={}, appId={}, expireAt={}",
                    session.qrcode, appId, expireAtMillis);
            return session.toQrcode();
        }
    }

    // ==================== 查询状态 ====================

    /**
     * 查询扫码状态：任意实例都可调用。
     *
     * @param qrcode 创建二维码时返回的令牌
     */
    public QrcodeStatus status(String qrcode) {
        Session session = qrcode == null ? null : sessions.get(qrcode);
        String status = session == null ? STATUS_UNKNOWN : session.effectiveStatus();

        // 「是否已登录」以共享存储里是否存在有效凭证为准（登录态才是绑定的最终依据）
        boolean loggedIn = botApiHelper.credentialCount() > 0 || credentialStore.hasValid();

        // 进程重启 / 请求落到 follower 时拿不到会话，但共享存储已登录：按已确认返回，
        // 别让前端因 UNKNOWN 一直轮询
        if (STATUS_UNKNOWN.equals(status) && loggedIn) {
            status = STATUS_CONFIRMED;
        }

        return new QrcodeStatus(status, loggedIn,
                session == null ? null : session.ilinkBotId,
                session == null ? null : session.ilinkUserId,
                currentLeaderInstanceId());
    }

    // ==================== 解绑 ====================

    /**
     * 解绑全部机器人：清除所有会话凭证 + 把处理中的消息任务重置为待处理。
     *
     * <p>多会话模型下这是「全部解绑」：管理台如需按单个机器人解绑，
     * 需要协议带上凭证 id 后改用 {@link BotCredentialStore#clearToken(Long)}。</p>
     *
     * @return 被重置的任务条数
     * @throws BotNotLeaderException 本实例不是主节点
     */
    public int reset() {
        requireLeader();
        // 逐会话清内存 + 共享存储 token，避免下一轮 restore 把旧 token 读回来
        for (Long credentialId : botApiHelper.allCredentialIds()) {
            botApiHelper.invalidateCredentials(credentialId);
        }
        sessions.clear();
        int resetTasks = inbox.resetRunning();
        // 立即打断仍在进行的长轮询，回到「等待扫码」状态，别拿已清除的凭证继续拉
        listener.interruptNow();
        listener.ensureRunning();
        log.warn("Bot 已解绑（全部会话）：登录凭证已清除，重置处理中任务 {} 条", resetTasks);
        return resetTasks;
    }

    // ==================== 内部实现 ====================

    /** 阻塞等待扫码结果，结束后把状态写回会话并完成绑定落库 */
    private void awaitConfirm(Session session) {
        long timeoutSeconds = botProperties.getLogin().getScanTimeoutSeconds();
        try {
            BotApiHelper.QrcodeStatus result = botApiHelper.waitForConfirm(
                    session.qrcode, Duration.ofSeconds(timeoutSeconds));

            if (result.status() != null && "confirmed".equalsIgnoreCase(result.status())) {
                // 确认后按 ilink_bot_id 幂等落库：appId 来自二维码会话
                Long credentialId = botApiHelper.completeLogin(result, session.appId);
                session.status = STATUS_CONFIRMED;
                session.ilinkBotId = result.ilinkBotId();
                session.ilinkUserId = result.ilinkUserId();
                // 凭证刚就绪：立刻唤醒监听巡检，为新会话启动长轮询，不必等下一轮巡检（默认 5s）
                listener.wakeUp();
                log.info("Bot 扫码绑定成功: credentialId={}, ilinkBotId={}, ilinkUserId={}, appId={}",
                        credentialId, result.ilinkBotId(), result.ilinkUserId(), session.appId);
            } else {
                session.status = STATUS_EXPIRED;
                log.warn("Bot 扫码未确认，二维码状态: {}", result.status());
            }
        } catch (IllegalStateException e) {
            // SDK 在二维码过期时抛 IllegalStateException（见 WeixinBotApi#waitForQrLogin）
            session.status = STATUS_EXPIRED;
            log.warn("Bot 登录二维码已过期: qrcode={}", session.qrcode);
        } catch (Exception e) {
            session.status = STATUS_TIMEOUT;
            log.warn("Bot 扫码等待失败（超时或网络异常）: qrcode={}", session.qrcode, e);
        }
    }

    /** 新二维码生成后，把仍在等待的旧会话标记为过期（前端轮询旧令牌会立即得到 EXPIRED） */
    private void supersedeWaitingSessions() {
        sessions.values().stream()
                .filter(session -> STATUS_WAITING.equals(session.status))
                .forEach(session -> {
                    session.status = STATUS_EXPIRED;
                    log.info("旧二维码已被新码替代: qrcode={}", session.qrcode);
                });
    }

    /** 清理已结束且超过保留时长的会话，避免进程内 Map 无限增长 */
    private void purgeEndedSessions() {
        long now = System.currentTimeMillis();
        sessions.values().removeIf(session ->
                !STATUS_WAITING.equals(session.status)
                        && session.expireAtMillis + SESSION_RETAIN_MILLIS < now);
    }

    private void requireLeader() {
        if (leaderElection.isLeader()) {
            return;
        }
        throw new BotNotLeaderException(currentLeaderInstanceId());
    }

    /** 从租约表取当前主节点：查不到时返回 null，由调用方按「未知」处理 */
    private String currentLeaderInstanceId() {
        try {
            BotLeader leader = botLeaderMapper.selectLeader();
            return leader == null || !StringUtils.hasText(leader.getInstanceId()) ? null : leader.getInstanceId();
        } catch (Exception e) {
            log.warn("查询当前 Bot 主节点失败", e);
            return null;
        }
    }

    /** 一次扫码绑定的会话状态 */
    private static final class Session {

        private final String qrcode;
        private final String imgContent;
        private final long expireAtMillis;

        /** 创建二维码时携带：确认后随凭证一起落库 */
        private final Long appId;

        /** 会话状态，由等待线程写、轮询线程读 */
        private volatile String status = STATUS_WAITING;

        /** 确认成功后回填，供状态查询展示 */
        private volatile String ilinkBotId;
        private volatile String ilinkUserId;

        private Session(String qrcode, String imgContent, long expireAtMillis, Long appId) {
            this.qrcode = qrcode;
            this.imgContent = imgContent;
            this.expireAtMillis = expireAtMillis;
            this.appId = appId;
        }

        /** 对外状态：仍待扫码但已过有效期的，直接按过期返回，不用等后台线程兜底 */
        private String effectiveStatus() {
            if (STATUS_WAITING.equals(status) && System.currentTimeMillis() > expireAtMillis) {
                return STATUS_EXPIRED;
            }
            return status;
        }

        private BotQrcode toQrcode() {
            return new BotQrcode(qrcode, imgContent, expireAtMillis);
        }
    }

    /** 创建二维码的结果 */
    public record BotQrcode(String qrcode, String imgContent, long expireAtMillis) {
    }

    /** 扫码状态查询结果 */
    public record QrcodeStatus(String status, boolean isLoggedIn,
                               String ilinkBotId, String ilinkUserId,
                               String leaderInstanceId) {
    }
}
