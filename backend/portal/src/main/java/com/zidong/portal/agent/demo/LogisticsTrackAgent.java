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
import com.zidong.portal.agent.agent.AppId;
import com.zidong.portal.agent.agent.ChatMemoryKey;
import com.zidong.portal.agent.agent.ChatSession;
import com.zidong.portal.agent.service.LLMService;

/**
 * 物流跟踪 Agent。
 *
 * <p>参数提取规则由本 Agent 自己维护：从用户输入里用 LLM 抽物流单号，
 * 抽不到就追问，不依赖入口统一解析参数。</p>
 *
 * <p>提取、查询、作答逻辑在 {@link AbstractLogisticsTrackAgent}；
 * 这里只剩「入口差异」一处：作答用内置格式化提示词，
 * 会话与记忆归属取自黑板里的 {@link AppId} 与 {@link ChatSession}（各入口统一绑定）。</p>
 */
@Agent(description = "物流跟踪：根据物流单号查询快递配送进度与预计送达时间")
public class LogisticsTrackAgent extends AbstractLogisticsTrackAgent {

    public LogisticsTrackAgent(DemoLogisticsMapper demoLogisticsMapper, LLMService llmService) {
        super(demoLogisticsMapper, llmService);
    }

    @AchievesGoal(description = "回答物流相关问题")
    @Action(description = "根据物流单号查询物流轨迹")
    public AgentResult track(UserInput userInput, Ai ai,
                             @Provided MessageSink sink,
                             @Provided AppId appId,
                             @Provided ChatSession chatSession) {
        // 作答提示词传 null 用内置格式化提示词；记忆按 (appId, taskId) 隔离，与 DefaultAgent 同一约定
        return doTrack(userInput.getContent(), ai, sink, null,
                optionsOf(chatSession.taskId()),
                new ChatMemoryKey(appId.appId(), chatSession.taskId()));
    }
}
