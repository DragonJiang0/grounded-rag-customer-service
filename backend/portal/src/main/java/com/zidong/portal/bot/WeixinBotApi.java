package com.zidong.portal.bot;

import com.zidong.dao.entity.BotCredential;
import io.github.cloudsen.ai.weixin.WeixinClient;
import io.github.cloudsen.ai.weixin.WeixinClientOptions;
import io.github.cloudsen.ai.weixin.WeixinGetUpdatesResponse;
import io.github.cloudsen.ai.weixin.WeixinMessage;
import io.github.cloudsen.ai.weixin.WeixinQrLoginStartResult;
import io.github.cloudsen.ai.weixin.WeixinQrLoginStatusResult;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * BotApi 调用类：基于 wx-aibot-java-sdk 的 {@link WeixinClient} 封装底层协议调用（多会话版）。
 * <p>
 * 职责：
 * 1. 扫码登录：{@link #startQrLogin()} 获取二维码，{@link #waitForQrLogin(String, Duration)} 等待确认，
 *    {@link #completeLogin(WeixinQrLoginStatusResult, Long)} 确认后按 {@code ilink_bot_id}
 *    幂等落库——一个 app 可被多个用户扫码，每个扫码用户一个独立会话；
 * 2. 会话恢复：启动 / 新 leader 接管时 {@link #restoreAll()} 把所有有效凭证逐行恢复为独立客户端；
 * 3. 消息收发：所有方法都带 {@code credentialId}，各会话独立的 token 与长轮询互不干扰。
 * <p>
 * 注意：SDK 与底层协议均无 bot_token 自动刷新机制，token 失效（getupdates ret=-14 / ret=null）时须重新扫码。
 * <p>
 * 多实例约定：只有主节点（见 {@link BotLeaderElection}）应执行扫码登录与消息长轮询，
 * 否则重复轮询同一 token 可能被服务端视为多端挤掉会话。
 */
@Component
public class WeixinBotApi {

    private static final Logger log = LoggerFactory.getLogger(WeixinBotApi.class);

    private static final String DEFAULT_BASE_URL = "https://ilinkai.weixin.qq.com";
    /** 服务端长轮询最长挂起 35s，客户端超时需大于该值 */
    private static final Duration LONG_POLL_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final String baseUrl;
    /** 凭证共享存储：多行，每个扫码用户一行 */
    private final BotCredentialStore credentialStore;

    /** 已恢复/已登录的凭证：credentialId -> 凭证 */
    private final Map<Long, BotCredential> credentials = new ConcurrentHashMap<>();
    /** 已鉴权客户端：credentialId -> 客户端，凭证变更时重建 */
    private final Map<Long, WeixinClient> clients = new ConcurrentHashMap<>();
    /** 未鉴权客户端：仅用于扫码登录阶段，懒加载 */
    private final AtomicReference<WeixinClient> loginClient = new AtomicReference<>();

    /** Spring 注入：基座地址可由 {@code bot.base-url} 覆盖 */
    @Autowired
    public WeixinBotApi(@Value("${bot.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl,
                        BotCredentialStore credentialStore) {
        this.baseUrl = baseUrl;
        this.credentialStore = credentialStore;
    }

    // ==================== 登录 ====================

    /** 发起扫码登录，返回 qrcode（轮询令牌）与 qrcode_img_content（二维码内容，供展示扫码） */
    public WeixinQrLoginStartResult startQrLogin() {
        return loginClient().startQrLogin();
    }

    /**
     * 等待用户扫码确认。只返回状态不落库：多会话模型下落库需要二维码会话携带的
     * appId，由调用方（扫码服务或自动登录服务）调用 {@link #completeLogin} 完成。
     *
     * @throws IllegalStateException 二维码过期时抛出，需重新获取
     */
    public WeixinQrLoginStatusResult waitForQrLogin(String qrcode, Duration timeout) {
        WeixinQrLoginStatusResult status = loginClient().waitForQrLogin(qrcode, timeout);
        if (status.expired()) {
            throw new IllegalStateException("二维码已过期，请重新获取");
        }
        return status;
    }

    /**
     * 扫码确认后的落库 + 客户端构建：按 {@code ilink_bot_id} 幂等写入
     * （同一用户重复扫码更新已有行，不同用户各自成行）。
     *
     * @param status      扫码确认结果（bot_token / baseurl / ilink ids）
     * @param appId       要绑定的应用 ID，可为空（保留行上已有绑定）
     * @return 凭证行 id
     */
    public Long completeLogin(WeixinQrLoginStatusResult status, Long appId) {
        BotCredential candidate = BotCredential.builder()
                .botToken(status.botToken())
                .baseurl(status.baseUrl())
                .ilinkBotId(status.accountId())
                .ilinkUserId(status.userId())
                .appId(appId)
                .build();
        Long id = credentialStore.upsertConfirmed(candidate);
        applyCredentials(id, candidate);
        log.info("Bot 扫码登录完成并落库: credentialId={}, ilinkBotId={}, appId={}",
                id, status.accountId(), appId);
        return id;
    }

    // ==================== 凭证持有 / 恢复 ====================

    /** 取指定会话的登录凭证，未恢复时返回 null */
    public BotCredential credentials(Long credentialId) {
        return credentials.get(credentialId);
    }

    /** 当前内存中已就绪的会话数 */
    public int credentialCount() {
        return credentials.size();
    }

    /** 当前内存中已就绪的全部会话 id（解绑时逐会话清理用） */
    public java.util.Set<Long> allCredentialIds() {
        return java.util.Set.copyOf(credentials.keySet());
    }

    /** 按机器人账号 ID 发送文本：入站消息的 to_user_id 即 ilink_bot_id，用于处理线程回复 */
    public void sendTextMessageByBotId(String ilinkBotId, String toUserId, String text, String contextToken) {
        Long credentialId = findCredentialIdByBotId(ilinkBotId);
        if (credentialId == null) {
            throw new IllegalStateException("未找到该机器人对应的登录会话: ilinkBotId=" + ilinkBotId);
        }
        sendTextMessage(credentialId, toUserId, text, contextToken);
    }

    private Long findCredentialIdByBotId(String ilinkBotId) {
        if (ilinkBotId == null) {
            return null;
        }
        return credentials.entrySet().stream()
                .filter(entry -> ilinkBotId.equals(entry.getValue().getIlinkBotId()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * 恢复单个会话的客户端（幂等）：该会话已在内存中就绪时直接返回 false，
     * 不重建客户端、不打日志。巡检线程每轮按凭证行调用，只有新会话才会真正恢复。
     *
     * @return true 表示本次新恢复了该会话
     */
    public boolean restore(BotCredential row) {
        if (row == null || row.getId() == null || row.getBotToken() == null || row.getBotToken().isBlank()) {
            return false;
        }
        if (credentials.containsKey(row.getId())) {
            return false;
        }
        applyCredentials(row.getId(), row);
        log.info("已恢复 Bot 会话的登录态: credentialId={}, ilinkBotId={}", row.getId(), row.getIlinkBotId());
        return true;
    }

    /**
     * 把共享存储中全部有效凭证逐行恢复为独立客户端：启动 / 新 leader 接管时一次性恢复。
     * <p>幂等：内存里已就绪的会话直接跳过。</p>
     */
    public void restoreAll() {
        int restored = 0;
        for (BotCredential row : credentialStore.loadValid()) {
            if (restore(row)) {
                restored++;
            }
        }
        if (restored > 0) {
            log.info("已恢复 {} 个 Bot 会话的登录态", restored);
        }
    }

    /**
     * 丢弃指定会话的登录态（内存凭证 + 已鉴权客户端 + 共享存储 token）。
     * 会话过期 / 凭证失效时由监听线程调用，该会话随后需重新扫码。
     */
    public void invalidateCredentials(Long credentialId) {
        credentials.remove(credentialId);
        closeQuietly(clients.remove(credentialId));
        // 共享存储里也必须清掉：否则下一轮 restore 会把同一个失效 token 读回来，永远登录不上
        credentialStore.clearToken(credentialId);
        log.warn("已清除失效的 Bot 登录凭证: credentialId={}", credentialId);
    }

    // ==================== 消息收发（均带 credentialId） ====================

    /**
     * 单次长轮询拉取消息（服务端最长挂起 35s）。
     *
     * @param credentialId  会话 id
     * @param getUpdatesBuf 游标，首次传空串
     */
    public WeixinGetUpdatesResponse getUpdates(Long credentialId, String getUpdatesBuf) {
        WeixinGetUpdatesResponse resp = authorizedClient(credentialId).getUpdates(getUpdatesBuf);
        if (!resp.success()) {
            if (resp.ret() != null && resp.ret() == -14) {
                // 会话过期：token 已不可用，必须清掉本地凭证后重新扫码
                throw new BotSessionExpiredException("getupdates 返回 ret=-14（会话已过期，需重新扫码登录）");
            }
            if (resp.ret() == null) {
                // 响应里连 ret 都没有：token 大概率已被服务端作废，
                // 继续轮询只会反复失败，由监听线程捕获后标记凭证无效并退出该会话的监听
                throw new BotCredentialInvalidException("getupdates 失败，ret=null（凭证可能已失效，需重新扫码登录）");
            }
            throw new IllegalStateException("getupdates 失败，ret=" + resp.ret());
        }
        return resp;
    }

    /** 发送文本消息，contextToken 为来源消息携带的上下文令牌（服务端要求必带） */
    public void sendTextMessage(Long credentialId, String toUserId, String text, String contextToken) {
        authorizedClient(credentialId).sendTextMessage(toUserId, text, contextToken);
    }

    /** 发送原始消息（图片/文件/视频等由调用方组装 {@link WeixinMessage}） */
    public String sendMessage(Long credentialId, WeixinMessage message) {
        return authorizedClient(credentialId).sendMessage(message);
    }

    // ==================== 客户端生命周期 ====================

    /** 设置凭证并构建/重建该会话的已鉴权客户端（关闭旧客户端） */
    private void applyCredentials(Long credentialId, BotCredential newCredentials) {
        credentials.put(credentialId, newCredentials);
        String base = newCredentials.getBaseurl() != null ? newCredentials.getBaseurl() : baseUrl;
        WeixinClient authorized = new WeixinClient(WeixinClientOptions.builder()
                .baseUrl(base)
                .token(newCredentials.getBotToken())
                .apiTimeoutMillis(REQUEST_TIMEOUT.toMillis())
                .longPollTimeoutMillis(LONG_POLL_TIMEOUT.toMillis())
                .build());
        closeQuietly(clients.put(credentialId, authorized));
    }

    private WeixinClient authorizedClient(Long credentialId) {
        WeixinClient current = clients.get(credentialId);
        if (current == null) {
            throw new IllegalStateException("会话尚未就绪: credentialId=" + credentialId
                    + "（请先完成扫码 completeLogin 或恢复凭证）");
        }
        return current;
    }

    /** 扫码登录阶段的未鉴权客户端，懒加载复用 */
    private WeixinClient loginClient() {
        return loginClient.updateAndGet(existing -> existing != null ? existing
                : new WeixinClient(WeixinClientOptions.builder()
                        .baseUrl(baseUrl)
                        .apiTimeoutMillis(REQUEST_TIMEOUT.toMillis())
                        .build()));
    }

    @PreDestroy
    public void close() {
        clients.values().forEach(this::closeQuietly);
        clients.clear();
        credentials.clear();
        closeQuietly(loginClient.getAndSet(null));
    }

    private void closeQuietly(WeixinClient client) {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.debug("关闭 WeixinClient 失败", e);
            }
        }
    }
}
