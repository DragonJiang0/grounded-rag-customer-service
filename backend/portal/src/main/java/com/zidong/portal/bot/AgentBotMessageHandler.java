package com.zidong.portal.bot;

import com.zidong.dao.entity.BotCredential;
import com.zidong.portal.agent.agent.AgentResult;
import com.zidong.portal.agent.agent.ChatSession;
import com.zidong.portal.agent.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 真实业务处理器：把 Bot 入站消息按绑定的应用路由到 embabel（多会话版）。
 *
 * <p>一个 app 可被多个用户扫码绑定：绑定关系按行存 {@code bot_credential}，
 * 入站消息的 {@code toUserId} 即机器人 {@code ilink_bot_id}，据此找到对应凭证行拿 appId，
 * 交给 {@link AgentService#runForBot} 走与其它渠道完全相同的 Agent 选择 + 执行 + 对话记录链路，
 * 最终答复经该机器人自己的会话客户端回给用户。</p>
 *
 * <p>用 {@code @Primary} 覆盖默认的 {@link PlaceholderBotMessageHandler}，
 * 不删除占位实现，便于需要时回退。</p>
 */
@Service
@Primary
@RequiredArgsConstructor
public class AgentBotMessageHandler implements BotMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentBotMessageHandler.class);

    private final BotApiHelper botApiHelper;
    private final AgentService agentService;
    private final BotCredentialStore credentialStore;

    @Override
    public void handle(BotApiHelper.BotMessage inbound) {
        // 一次取出凭证：后续既要用 app_id 路由，也要用凭证 id 作为对话记录标签
        BotCredential credential = loadCredential(inbound.toUserId());
        Long appId = credential == null ? null : credential.getAppId();
        if (appId == null) {
            log.warn("机器人未绑定应用，无法路由消息: ilinkBotId={}, from={}",
                    inbound.toUserId(), inbound.fromUserId());
            botApiHelper.replyTextByBot(inbound, "当前机器人尚未绑定应用，请先在管理台完成绑定。");
            return;
        }

        // taskId 以用户为粒度：同一用户的多轮对话共享记忆与对话记录
        String taskId = "bot-" + inbound.fromUserId();

        AgentResult result = agentService.runForBot(inbound.text(), appId, credential.getId(), new ChatSession(taskId));
        botApiHelper.replyTextByBot(inbound, result.output());
    }

    /** 按机器人 ID 读凭证：读不到或读失败按「未绑定」处理，不打断消息回复 */
    private BotCredential loadCredential(String ilinkBotId) {
        return credentialStore.findByBotId(ilinkBotId);
    }
}
