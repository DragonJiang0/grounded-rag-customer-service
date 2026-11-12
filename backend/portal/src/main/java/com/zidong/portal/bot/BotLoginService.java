package com.zidong.portal.bot;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bot 扫码登录：凭证缺失或 token 失效时，由主节点单点发起扫码。
 *
 * <p>为什么必须收敛到主节点：同一个 bot 账号重复扫码会让旧 token 失效，
 * 多实例各自扫码的结果就是互相踢下线。这里用
 * {@link BotLeaderElection#isLeader()} + 进程内 {@link AtomicBoolean} 双重约束，
 * 保证同一时刻只有一个扫码流程；跨实例由主节点身份保证。</p>
 *
 * <p>登录成功后凭证由 {@link WeixinBotApi} 经 {@link BotCredentialStore} 写入共享存储，
 * 其它实例与新接管的 leader 直接复用，无需再次扫码。</p>
 *
 * <p>二维码只输出到日志：{@code qrcodeImgContent} 是供展示的二维码内容/链接，
 * 运维从日志取出来扫码即可。若后续接入管理端，可关闭 {@code bot.login.auto}，
 * 由页面调用 {@link BotApiHelper#fetchQrcode()} 自行展示。</p>
 */
@Service
@RequiredArgsConstructor
public class BotLoginService {

    private static final Logger log = LoggerFactory.getLogger(BotLoginService.class);

    private final BotApiHelper botApiHelper;
    private final BotLeaderElection leaderElection;
    private final BotProperties botProperties;

    /** 扫码流程互斥：避免同一实例并发发起多个二维码 */
    private final AtomicBoolean loggingIn = new AtomicBoolean(false);

    /** 上次发起扫码的时间，用于失败后的最小重试间隔 */
    private volatile long lastAttemptAtMillis = 0L;

    /**
     * 需要时发起扫码登录（幂等调用，监听巡检线程在没有任何有效凭证时调用）。
     * <p>以下情况直接返回：未开启自动登录、本实例不是主节点、已有任一可用会话、
     * 正在扫码中、距离上次尝试不足重试间隔。</p>
     */
    public void loginIfNecessary() {
        if (!botProperties.getLogin().isAuto()
                || !leaderElection.isLeader()
                || botApiHelper.credentialCount() > 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastAttemptAtMillis < botProperties.getLogin().getRetryIntervalMillis()) {
            return;
        }
        if (!loggingIn.compareAndSet(false, true)) {
            return;
        }
        lastAttemptAtMillis = now;
        Thread.ofVirtual().name("bot-qr-login").start(this::login);
    }

    private void login() {
        long timeoutSeconds = botProperties.getLogin().getScanTimeoutSeconds();
        try {
            BotApiHelper.BotQrcode qrcode = botApiHelper.fetchQrcode();
            log.warn("====================================================");
            log.warn("  Bot 尚未登录，请用微信扫码（{} 秒内有效）", timeoutSeconds);
            log.warn("  二维码内容: {}", qrcode.qrcodeImgContent());
            log.warn("  二维码令牌: {}", qrcode.qrcode());
            log.warn("====================================================");

            BotApiHelper.QrcodeStatus status = botApiHelper.waitForConfirm(qrcode.qrcode(), Duration.ofSeconds(timeoutSeconds));
            if (status.status() != null && "confirmed".equalsIgnoreCase(status.status())) {
                // 自动登录场景无管理台上下文：appId 为空，按 ilink_bot_id 幂等落库
                botApiHelper.completeLogin(status, null);
                log.info("扫码登录成功: ilinkBotId={}, ilinkUserId={}", status.ilinkBotId(), status.ilinkUserId());
            } else {
                log.warn("扫码未确认，二维码状态: {}", status.status());
            }
        } catch (Exception e) {
            log.warn("扫码登录未完成（超时或二维码过期），{}ms 后重试: {}",
                    botProperties.getLogin().getRetryIntervalMillis(), e.getMessage());
        } finally {
            loggingIn.set(false);
        }
    }
}
