package com.zidong.portal.agent.demo;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Provided;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;
import com.zidong.common.sse.MessageSink;
import com.zidong.dao.mapper.DemoLogisticsMapper;
import com.zidong.portal.agent.agent.AgentResult;
import com.zidong.portal.agent.agent.ChatMemoryKey;
import com.zidong.portal.agent.agent.InlinePrompt;
import com.zidong.portal.agent.service.ChatOptions;
import com.zidong.portal.agent.service.LLMService;

/**
 * 物流跟踪调试代理：作答提示词由调用方逐次传入（gRPC 调试入口），替换内置格式化提示词。
 *
 * <p>提取、查询、作答逻辑全部复用 {@link AbstractLogisticsTrackAgent}，
 * 这里只剩「作答提示词由调用方带过来」这一处差异：调用方提示词决定查询结果整理成什么样，
 * 未传或为空时用内置格式化提示词，行为与普通 Agent 一致。</p>
 */
@Agent(description = "物流跟踪调试：作答提示词由调用方传入", scan = false)
public class DebugLogisticsTrackAgent extends AbstractLogisticsTrackAgent {

    public DebugLogisticsTrackAgent(DemoLogisticsMapper demoLogisticsMapper, LLMService llmService) {
        super(demoLogisticsMapper, llmService);
    }

    @AchievesGoal(description = "按调用方提示词整理查询结果后回答物流相关问题")
    @Action(description = "根据物流单号查询物流轨迹（提示词决定回复格式）")
    public AgentResult track(UserInput userInput, Ai ai,
                             @Provided MessageSink sink,
                             @Provided InlinePrompt inlinePrompt,
                             @Provided ChatOptions chatOptions) {
        // 对话参数与记忆键的组装与 InlinePromptAgent 同一约定：参数随请求带进来，appId 来自 InlinePrompt
        return doTrack(userInput.getContent(), ai, sink, inlinePrompt.template(),
                chatOptions,
                new ChatMemoryKey(inlinePrompt.appId(), chatOptions.taskId()));
    }
}
