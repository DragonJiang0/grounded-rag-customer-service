package com.zidong.portal.agent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Provided;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;
import com.zidong.common.sse.MessageSink;
import com.zidong.portal.agent.service.ChatOptions;
import com.zidong.portal.agent.service.LLMService;
import com.zidong.dao.mapper.AppCorpusGroupMapper;
import com.zidong.dao.mapper.CorpusMapper;

/**
 * 外部提示词代理：提示词由调用方逐次传入（gRPC 调试入口），不读数据库配置。
 *
 * <p>命中与作答逻辑全部复用 {@link AbstractDefaultAgent}，这里只剩「提示词由调用方带过来」这一处差异。
 */
@Agent(description = "外部提示词代理", scan = false)
public class InlinePromptAgent extends AbstractDefaultAgent {

    public InlinePromptAgent(AppCorpusGroupMapper appCorpusGroupMapper,
                             CorpusMapper corpusMapper,
                             LLMService llmService) {
        super(appCorpusGroupMapper, corpusMapper, llmService);
    }

    @Action
    public HitQuestion hitQuestion(UserInput userInput, Ai ai, @Provided MessageSink sink, @Provided InlinePrompt inlinePrompt) {
        return doHitQuestion(userInput.getContent(), ai, sink, inlinePrompt.appId());
    }

    @AchievesGoal(description = "用调用方给定的提示词回答用户的问题")
    @Action
    public AgentResult answer(HitQuestion hitQuestion, @Provided MessageSink sink, @Provided  InlinePrompt inlinePrompt,@Provided ChatOptions chatOptions ) {
        // 记忆按 (appId, taskId) 隔离：复合键带上 appId，删除应用时才能按应用整片清理会话记忆
        return doAnswer(hitQuestion, sink, inlinePrompt.template(), chatOptions,
                new ChatMemoryKey(inlinePrompt.appId(), chatOptions.taskId()));
    }
}
