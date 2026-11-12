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
import com.zidong.dao.entity.AppChatOptions;
import com.zidong.dao.mapper.AppChatOptionsMapper;
import com.zidong.dao.mapper.AppCorpusGroupMapper;
import com.zidong.dao.mapper.CorpusMapper;
import com.zidong.portal.service.AppPromptService;

/**
 * 默认代理：提示词按 appId 从 app_prompt 表读取。
 *
 * <p>命中与作答逻辑全部复用 {@link AbstractDefaultAgent}，这里只剩「提示词从数据库来」这一处差异。
 */
@Agent(description = "默认代理", scan = false)
public class DefaultAgent extends AbstractDefaultAgent {

    private final AppPromptService appPromptService;

    private final AppChatOptionsMapper appChatOptionsMapper;

    public DefaultAgent(AppCorpusGroupMapper appCorpusGroupMapper,
                        CorpusMapper corpusMapper,
                        AppPromptService appPromptService,
                        LLMService llmService, AppChatOptionsMapper appChatOptionsMapper) {
        super(appCorpusGroupMapper, corpusMapper, llmService);
        this.appPromptService = appPromptService;
        this.appChatOptionsMapper = appChatOptionsMapper;
    }

    @Action
    public HitQuestion hitQuestion(UserInput userInput, Ai ai, @Provided MessageSink sink, @Provided AppId appId) {
        return doHitQuestion(userInput.getContent(), ai, sink, appId.appId());
    }

    @AchievesGoal(description = "回答用户的问题")
    @Action
    public AgentResult answer(HitQuestion hitQuestion, @Provided MessageSink sink,
                              @Provided AppId appId,
                              @Provided ChatSession chatSession) {

        AppChatOptions chatOptions = appChatOptionsMapper.findByAppId(appId.appId());
        // 记忆按 taskId 隔离：同一应用下不同会话各自一份历史，未带 taskId 时退化为无记忆；
        // 复合键里再带上 appId，删除应用时才能按应用整片清理会话记忆
        return doAnswer(hitQuestion, sink, appPromptService.template(appId.appId()),
                ChatOptions.of(chatSession.taskId(), chatOptions),
                new ChatMemoryKey(appId.appId(), chatSession.taskId()));
    }
}
