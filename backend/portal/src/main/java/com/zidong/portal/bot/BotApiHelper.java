package com.zidong.portal.bot;

import com.zidong.dao.entity.BotCredential;
import io.github.cloudsen.ai.weixin.WeixinGetUpdatesResponse;
import io.github.cloudsen.ai.weixin.WeixinMessage;
import io.github.cloudsen.ai.weixin.WeixinQrLoginStartResult;
import io.github.cloudsen.ai.weixin.WeixinQrLoginStatusResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

/**
 * 微信 iLink Bot API 门面（基于 wx-aibot-java-sdk，底层协议调用委托给 {@link WeixinBotApi}）。
 * <p>
 * 覆盖完整的机器人生命周期（多会话版：一个 app 可被多个用户扫码，每个扫码用户一个会话，
 * 所有收发方法都带 {@code credentialId}）：
 * 1. {@link #fetchQrcode()} 获取登录二维码；
 * 2. {@link #waitForConfirm(String, Duration)} 轮询扫码状态，
 *    {@link #completeLogin(QrcodeStatus, Long)} 确认后按 {@code ilink_bot_id} 幂等落库并构建客户端；
 * 3. {@link #pollUserMessages(Long, String)} 单次长轮询（自动返回新游标），
 *    {@link #listenUserMessages(Long)} 在其上提供无限流（用于兼容旧用法）；
 * 4. {@link #replyText(Long, BotMessage, String)} 回复文本（自动回带 context_token）。
 * <p>
 * 启动 / 新 leader 接管时由 {@link #restoreAll()} 从共享存储逐行恢复全部有效会话，免重复扫码。
 */
@Component
public class BotApiHelper {

    private final WeixinBotApi botApi;

    /** Spring 注入容器中的 WeixinBotApi 单例，全局复用同一底层客户端集合 */
    @Autowired
    public BotApiHelper(WeixinBotApi botApi) {
        this.botApi = botApi;
    }

    // ==================== 1. 登录：获取二维码 ====================

    /**
     * 获取机器人登录二维码。
     *
     * @return qrcode（轮询状态用的令牌）与 qrcode_img_content（二维码图片内容，供展示扫码）
     */
    public BotQrcode fetchQrcode() {
        WeixinQrLoginStartResult qr = botApi.startQrLogin();
        return new BotQrcode(qr.qrcode(), qr.qrcodeUrl());
    }

    // ==================== 2. 等待扫码确认 ====================

    /**
     * 每秒轮询扫码状态，直到用户确认登录或超过 timeout。只返回状态不落库，
     * 确认后由调用方携带 appId 调 {@link #completeLogin} 完成绑定。
     */
    public QrcodeStatus waitForConfirm(String qrcode, Duration timeout) {
        WeixinQrLoginStatusResult status = botApi.waitForQrLogin(qrcode, timeout);
        return new QrcodeStatus(status.status(), status.botToken(),
                status.accountId(), status.userId(), status.baseUrl());
    }

    /**
     * 扫码确认后的绑定：按 {@code ilink_bot_id} 幂等落库（同一用户重复扫码更新已有行，
     * 不同用户各自成行）并构建该会话的已鉴权客户端。
     *
     * @return 凭证行 id
     */
    public Long completeLogin(QrcodeStatus status, Long appId) {
        return botApi.completeLogin(new WeixinQrLoginStatusResult(
                status.status(), status.botToken(), status.ilinkBotId(),
                status.baseurl(), status.ilinkUserId()), appId);
    }

    // ==================== 会话就绪与恢复 ====================

    /** 当前内存中已就绪的会话数 */
    public int credentialCount() {
        return botApi.credentialCount();
    }

    /** 当前内存中已就绪的全部会话 id（解绑时逐会话清理用） */
    public java.util.Set<Long> allCredentialIds() {
        return botApi.allCredentialIds();
    }

    /** 指定会话是否已就绪（内存持有凭证），未就绪时返回 false */
    public boolean credentialsReady(Long credentialId) {
        return botApi.credentials(credentialId) != null;
    }

    /**
     * 恢复单个会话的客户端（幂等）：巡检线程每轮按凭证行调用，已就绪的会话不会被重建。
     *
     * @return true 表示本次新恢复了该会话
     */
    public boolean restore(BotCredential row) {
        return botApi.restore(row);
    }

    /**
     * 从共享存储恢复全部有效会话，跳过扫码流程。
     * <p>多实例下，新主节点通过它从 DB 逐行读取上一个 leader 写入的凭证直接接管。</p>
     */
    public void restoreAll() {
        botApi.restoreAll();
    }

    /**
     * 丢弃指定会话的登录态（内存凭证 + 客户端 + 共享存储 token）。
     * 会话过期时由监听线程调用，随后该会话需重新扫码。
     */
    public void invalidateCredentials(Long credentialId) {
        botApi.invalidateCredentials(credentialId);
    }

    // ==================== 3. 长轮询收消息 ====================

    /**
     * 持续长轮询拉取消息，自动维护 get_updates_buf 游标。
     * 返回无限流，仅透出用户消息（message_type == 1），用 limit / takeWhile 控制结束。
     * <p>建议改用 {@link #pollUserMessages(Long, String)} 自行控制生命周期。</p>
     */
    public Stream<BotMessage> listenUserMessages(Long credentialId) {
        return Stream.generate(() -> pollUserMessages(credentialId, ""))
                .flatMap(page -> page.messages().stream());
    }

    /**
     * 单次长轮询（服务端最长挂起 35s），返回更新后的游标与本批消息。
     * <p>监听线程持有该会话的游标并按需持久化（落 {@code bot_credential.updates_buf}）；
     * 新主节点接管时按行读取上次进度，避免从空游标重拉历史消息。</p>
     */
    public UpdatesPage pollUserMessages(Long credentialId, String getUpdatesBuf) {
        WeixinGetUpdatesResponse resp = getUpdates(credentialId, getUpdatesBuf);
        String nextBuf = resp.getUpdatesBuf() != null ? resp.getUpdatesBuf() : getUpdatesBuf;
        List<BotMessage> msgs = resp.msgs() == null
                ? List.of()
                : resp.msgs().stream()
                    .filter(msg -> msg.messageType() != null
                            && msg.messageType() == WeixinMessage.MESSAGE_TYPE_USER)
                    .map(BotMessage::from)
                    .toList();
        return new UpdatesPage(nextBuf, msgs);
    }

    /** 单次长轮询拉取（服务端最长挂起 35s），返回原始 SDK 响应：保留供测试或特殊场景使用 */
    public WeixinGetUpdatesResponse getUpdates(Long credentialId, String getUpdatesBuf) {
        return botApi.getUpdates(credentialId, getUpdatesBuf);
    }

    // ==================== 4. 回复消息 ====================

    /**
     * 回复用户消息（按会话指定客户端），自动回带来源消息的 context_token（服务端要求必带）。
     *
     * @param credentialId 处理该消息的会话 id
     */
    public void replyText(Long credentialId, BotMessage inbound, String text) {
        sendText(credentialId, inbound.fromUserId(), inbound.contextToken(), text);
    }

    public void sendText(Long credentialId, String toUserId, String contextToken, String text) {
        botApi.sendTextMessage(credentialId, toUserId, text, contextToken);
    }

    /**
     * 回复用户消息（按入站消息的接收方机器人路由客户端），自动回带 context_token。
     * <p>多会话模型下处理线程只持有消息：入站消息的 {@code toUserId} 即机器人 ilink_bot_id，
     * 据此找到对应会话的客户端回复。</p>
     */
    public void replyTextByBot(BotMessage inbound, String text) {
        botApi.sendTextMessageByBotId(inbound.toUserId(), inbound.fromUserId(), text, inbound.contextToken());
    }

    // ==================== DTO ====================

    public record BotQrcode(
            String qrcode,
            String qrcodeImgContent) {
    }

    /** 单次长轮询的一页结果：新的游标与本批消息 */
    public record UpdatesPage(
            String nextBuf,
            List<BotMessage> messages) {
    }

    public record QrcodeStatus(
            String status,
            String botToken,
            String ilinkBotId,
            String ilinkUserId,
            String baseurl) {
    }

    /** 入站消息视图：从 SDK {@link WeixinMessage} 提取业务侧关心的字段 */
    public record BotMessage(
            Long messageId,
            String fromUserId,
            String toUserId,
            Integer messageType,
            Integer messageState,
            String contextToken,
            String text) {

        static BotMessage from(WeixinMessage msg) {
            return new BotMessage(msg.messageId(), msg.fromUserId(), msg.toUserId(),
                    msg.messageType(), msg.messageState(), msg.contextToken(), firstText(msg));
        }

        /** 取第一个文本项内容，等价于 JS 的 item_list?.[0]?.text_item?.text */
        private static String firstText(WeixinMessage msg) {
            if (msg.itemList() == null || msg.itemList().isEmpty()) {
                return null;
            }
            WeixinMessage.Item first = msg.itemList().get(0);
            return first.textItem() != null ? first.textItem().text() : null;
        }
    }
}
