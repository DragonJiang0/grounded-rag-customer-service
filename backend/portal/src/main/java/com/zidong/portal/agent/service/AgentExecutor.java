package com.zidong.portal.agent.service;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import com.zidong.common.sse.EmitterSink;
import com.zidong.common.sse.MessageSink;
import com.zidong.dao.entity.ChatHistory;
import com.zidong.portal.agent.agent.AgentResult;
import com.zidong.portal.service.ChatHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent 执行引擎：黑板绑定、进程创建、结果取回、对话记录、收尾推送只有这一份实现。
 *
 * <p>本类只负责「执行」：Agent 由入口服务（{@link AgentService} / {@link InlinePromptAgentService}）
 * 选定后传进来，这里不关心 Agent 是怎么选出来的。
 */
@Component
public class AgentExecutor {

    private final Logger log = LoggerFactory.getLogger(AgentExecutor.class);

    private final AgentPlatform agentPlatform;

    /** 对话记录的唯一入口，user / assistant 都经它落库 */
    private final ChatHistoryService chatHistoryService;

    public AgentExecutor(AgentPlatform agentPlatform, ChatHistoryService chatHistoryService) {
        this.agentPlatform = agentPlatform;
        this.chatHistoryService = chatHistoryService;
    }

    /**
     * 执行指定 Agent：黑板绑定、进程创建、结果取回、对话记录、收尾推送只有这一份实现。
     *
     * <p>对话记录也在这里收口：本方法是全项目所有对话入口（/api 阻塞、/api 流式、gRPC 调试）的唯一
     * 汇聚点，且执行是同步的——返回时结果一定在手上，因此 block 与 stream 共用同一处记录，
     * 不需要各自实现，也不依赖 sink 终态回调。
     *
     * @param agent         已选定的 Agent 元数据
     * @param message       用户输入
     * @param extraBindings 该入口特有的黑板绑定（各入口的唯一差异）
     * @param sink          过程推送通道，可为 null
     * @param taskId        会话标识，作为对话记录的 conversationId；为空则不记录
     * @param appId         所属应用 ID，随会话行一起落库，可为 null
     * @param channel       来源渠道，见 {@link ChatHistoryService#CHANNEL_API} / {@link ChatHistoryService#CHANNEL_DEBUG}
     * @param tag           对话记录标签：账号密码登录窗口取窗口 id，微信个人号取 bot_credential.id，
     *                      调试与免密窗口渠道为 null
     * @return Agent 的最终结果
     */
    public AgentResult execute(Agent agent, String message, Map<String, Object> extraBindings,
                               MessageSink sink, String taskId, Long appId, Integer channel, Long tag) {

        // 消息进来时先落用户提问：记录失败不能影响对话，内部已兜住
        ChatHistory chatHistory = recordQuestion(taskId, appId, channel, tag, message);

        if (sink != null) sink.status("开始执行");

        AgentResult result = runProcess(agent, message, extraBindings, sink);

        // 消息结束时落模型回复：同样收口在这里，覆盖 block / stream / gRPC
        recordAnswer(chatHistory, result);

        if (sink != null) sink.done(result.output());
        return result;
    }

    /**
     * 执行 Agent，但不落对话记录（调试专用）。
     *
     * <p>与 {@link #execute(Agent, String, Map, MessageSink, String, Long, Integer, Long)} 的差别
     * 只有「不记录」这一处：不写 user / assistant 历史，只负责执行、取回结果并完成收尾推送。
     * 黑板绑定与进程创建仍然复用同一份实现，避免两条链路行为漂移。</p>
     *
     * @param agent         已选定的 Agent 元数据
     * @param message       用户输入
     * @param extraBindings 该入口特有的黑板绑定
     * @param sink          过程推送通道，可为 null
     * @return Agent 的最终结果
     */
    public AgentResult execute(Agent agent, String message, Map<String, Object> extraBindings, MessageSink sink) {
        if (sink != null) sink.status("开始执行");

        AgentResult result = runProcess(agent, message, extraBindings, sink);

        if (sink != null) sink.done(result.output());
        return result;
    }

    /**
     * 只做执行：黑板绑定、进程创建、跑一遍 planner、按类型取回结果。
     *
     * <p>过程状态与收尾推送留给调用方，因为「什么时候算开始、结果要不要再加工后才 done」
     * 两个入口并不相同；对话记录同理，不在这里出现。</p>
     *
     * @return Agent 的最终结果
     */
    private AgentResult runProcess(Agent agent, String message, Map<String, Object> extraBindings, MessageSink sink) {
        UserInput userInput = new UserInput(message);

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("it", userInput);
        bindings.put("userInput", userInput);
        bindings.putAll(extraBindings);


        // MessageSink 是 action 的前置条件，黑板里必须始终有一个：
        // 没有真实连接时用 noop 占位，否则 planner 找不到计划，进程会 STUCK
        bindings.put(MessageSink.SINK, sink != null ? sink : EmitterSink.noop());

        // 不设 contextId：会话记忆由项目自己管理（langchain4j + ChatMemoryKey / ChatHistoryService），
        // 不使用 embabel 的 Context 机制；设了反而会去 ContextRepository 查不存在的 Context，
        // 每次执行刷一条 "Context ... not found for agent process ..." 的 WARN
        ProcessOptions options = ProcessOptions.DEFAULT;
        AgentProcess process = agentPlatform.createAgentProcess(agent, options, bindings);
        process.run();

        // 不能用 lastResult()：它返回黑板里「最后放入的对象」，本例里就是绑进去的入参 UserInput，
        // 不保证是 @AchievesGoal 动作的输出；resultOfType 按类型取，拿到的才是 answer 的返回值。
        return process.resultOfType(AgentResult.class);
    }

    /**
     * 消息进来时记录用户提问。
     *
     * <p>没有会话标识（taskId 为空）时无法归属会话，直接跳过；记录失败只告警，不打断对话。
     */
    private ChatHistory recordQuestion(String taskId, Long appId, Integer channel, Long tag, String message) {
        if (taskId == null || taskId.isBlank()) {
            log.debug("对话记录: taskId 为空，跳过用户提问落库");
            return null;
        }
        log.debug("对话记录: 准备落库用户提问, taskId={}, appId={}, channel={}", taskId, appId, channel);
        try {
            return chatHistoryService.begin(taskId, appId, channel, tag, message);
        } catch (Exception ex) {
            log.warn("记录用户提问失败, taskId={}", taskId, ex);
            return null;
        }
    }

    /**
     * 消息结束时记录模型回复。
     *
     * <p>会话为空（未开始记录）或无结果时跳过；记录失败只告警，不影响已经推给客户端的答复。
     */
    private void recordAnswer(ChatHistory chatHistory, AgentResult result) {
        if (chatHistory == null || result == null) {
            log.debug("对话记录: 会话为空或无结果，跳过模型回复落库");
            return;
        }
        try {
            chatHistoryService.recordAssistant(chatHistory, result.output());
        } catch (Exception ex) {
            log.warn("记录模型回复失败, id={}", chatHistory.getId(), ex);
        }
    }
}
