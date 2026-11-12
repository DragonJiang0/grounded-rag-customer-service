package com.zidong.portal.agent.service;

import com.embabel.agent.api.annotation.support.AgentMetadataReader;
import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.zidong.common.sse.MessageSink;
import com.zidong.portal.agent.agent.AgentResult;
import com.zidong.portal.agent.agent.InlinePrompt;
import com.zidong.portal.agent.demo.DebugLogisticsTrackAgent;
import com.zidong.portal.agent.demo.DebugOrderQueryAgent;
import com.zidong.portal.agent.demo.LogisticsTrackAgent;
import com.zidong.portal.agent.demo.OrderQueryAgent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent 调试入口的服务：按名称执行平台上已注册的任意 Agent。
 *
 * <p>与 {@link InlinePromptAgentService}（恒走外部提示词 Agent）不同，本入口由调用方
 * 指定要调试的 Agent（如订单查询、物流跟踪等 demo Agent），
 * 执行细节（黑板绑定、进程创建、落库、收尾推送）同样委托 {@link AgentExecutor}。</p>
 *
 * <p>有调试变体的 Agent（demo Agent）不直接执行本体，而是换成对应的 Debug 变体：
 * 调用方仍传普通 Agent 的名称，本服务经 {@link #findDebugAgent(String)} 换成 Debug 变体的元数据。
 * 调用方传入的提示词用作「作答提示词」：查询结果作为事实材料，按提示词整理成最终回复；
 * 未传提示词时用 Agent 内置的格式化提示词，行为与普通 Agent 一致。</p>
 *
 * <p>调用方传入的提示词对不声明该参数的 Agent 不生效：黑板里多余绑定会被 planner 忽略，
 * 只有 action 声明 {@code @Provided} 才会被消费。</p>
 */
@Service
@RequiredArgsConstructor
public class AgentDebugService {

    private static final Logger log = LoggerFactory.getLogger(AgentDebugService.class);

    private final AgentPlatform agentPlatform;

    private final AgentExecutor agentExecutor;

    private final AgentMetadataReader agentMetadataReader;

    /** 物流跟踪的调试变体：scan=false，框架不扫描，需要在这里解析成元数据 */
    private final DebugLogisticsTrackAgent debugLogisticsTrackAgent;

    /** 订单查询的调试变体：scan=false，框架不扫描，需要在这里解析成元数据 */
    private final DebugOrderQueryAgent debugOrderQueryAgent;

    /**
     * 调试变体注册表：key 为调用方传的普通 Agent 名称，value 为对应调试变体的元数据。
     * 查询入口是 {@link #findDebugAgent(String)}，在 {@link #findAgent(String)} 里替换执行。
     */
    private final Map<String, Agent> debugAgents = new HashMap<>();

    /**
     * 初始化调试变体注册表：把每个 Debug 变体解析成元数据，按「普通 Agent 名称」登记。
     * 启动即解析一次，运行期查询不再反射，登记失败直接阻止启动（配置错误尽早暴露）。
     *
     * <hr>
     *
     * <b>如何新增自定义 Agent 的调试变体（后续开发照此三步走）：</b>
     *
     * <ol>
     *   <li>正常编写业务 Agent（如 {@code FooAgent}），加 {@code @Agent} 注解，让平台扫描注册——
     *       正式链路（多 Agent 选择、列表下发）看到的仍是它；</li>
     *   <li>参照 {@link com.zidong.portal.agent.demo.DebugLogisticsTrackAgent} 写调试变体
     *       {@code DebugFooAgent}：继承业务 Agent 的抽象基类（或复用同一份 doXxx 逻辑），
     *       {@code @Agent(scan = false)} 不让平台扫描，action 里声明
     *       {@code @Provided InlinePrompt inlinePrompt}，把 {@code inlinePrompt.template()}
     *       作为作答提示词传给 doXxx——查询结果按提示词整理成最终回复，
     *       未传提示词时用内置提示词，行为与普通 Agent 完全一致；</li>
     *   <li>在下面 {@link #init()} 里追加一行注册，key 必须用「普通 Agent 的类名」——
     *       即 gRPC 调用方传的 agentName：{@code registerDebugAgent(debugFooAgent, FooAgent.class.getSimpleName());}</li>
     * </ol>
     *
     * <p>注册之后无需改动其他任何代码：调试入口传 {@code FooAgent} 会自动执行 {@code DebugFooAgent}，
     * 且调用方的提示词生效；列表接口仍只展示被平台扫描的 {@code FooAgent}。</p>
     */
    @PostConstruct
    void init() {
        registerDebugAgent(debugLogisticsTrackAgent, LogisticsTrackAgent.class.getSimpleName());
        registerDebugAgent(debugOrderQueryAgent, OrderQueryAgent.class.getSimpleName());
        // 新增调试变体时在这里追加注册行，格式：
        // registerDebugAgent(debugFooAgent, FooAgent.class.getSimpleName());
    }

    /** 把调试变体解析成元数据并按普通 Agent 的名称登记，登记失败直接阻止启动 */
    private void registerDebugAgent(Object agentBean, String name) {
        Agent meta = (Agent) agentMetadataReader.createAgentMetadata(agentBean);
        if (meta == null) {
            throw new IllegalStateException(name + " 的调试变体未被识别为 Agent");
        }
        debugAgents.put(name, meta);
    }

    /**
     * 按名称执行 Agent（纯调试，不落对话记录）。
     *
     * <p>只把执行必需的入参交给 {@link AgentExecutor}：用户输入、对话参数、推送通道；
     * appId / channel / tag 一律不传，taskId 也传 null——执行器据此跳过 user 与 assistant 的落库，
     * 调试过程不污染 {@code chat_history}。</p>
     *
     * <p>{@code chatOptions} 本身仍作为黑板绑定向下传递（其中携带的 taskId 可继续被 Agent
     * 用作会话记忆键），落库与否只由本方法传出的 taskId 决定。</p>
     *
     * @param message     用户输入
     * @param agentName   Agent 名称（运行时唯一标识），必须已注册
     * @param chatOptions 本次调试的对话参数，作为黑板绑定向下传递（Agent 按需消费）
     * @param sink        过程推送通道
     * @return Agent 的最终结果
     * @throws IllegalArgumentException 名称未注册时抛出，由调用方转成 error 事件
     */
    public AgentResult run(String message, InlinePrompt inlinePrompt, String agentName, ChatOptions chatOptions, MessageSink sink) {
        Agent agent = findAgent(agentName);
        log.info("Agent 调试: agent={}, taskId={}", agentName, chatOptions.taskId());

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("chatOptions", chatOptions);
        // 提示词随黑板带下去：调试变体（demo Agent 的 Debug 版）声明 @Provided InlinePrompt 消费它，
        // 其他 Agent 不声明该参数，planner 会忽略这条多余绑定
        if (inlinePrompt != null) {
            bindings.put("inlinePrompt", inlinePrompt);
        }
        // 走不落库的执行重载：只有执行与推送，没有 user / assistant 的历史记录
        return agentExecutor.execute(agent, message, bindings, sink);
    }

    /**
     * 按名称执行 Agent。
     *
     * @param message     用户输入
     * @param agentName   Agent 名称（运行时唯一标识），必须已注册
     * @param chatOptions 本次请求的对话参数，作为黑板绑定向下传递（Agent 按需消费）
     * @param sink        过程推送通道
     * @param channel     对话记录渠道（调试渠道）
     * @param tag         对话记录标签
     * @return Agent 的最终结果
     * @throws IllegalArgumentException 名称未注册时抛出，由调用方转成 error 事件
     */
    public AgentResult run(String message, String agentName, ChatOptions chatOptions,
                           MessageSink sink, Integer channel, Long tag) {
        Agent agent = findAgent(agentName);
        log.info("Agent 调试: agent={}, taskId={}", agentName, chatOptions.taskId());

        // 该入口没有应用归属：appId 传 null，对话记录只按会话（taskId）归属
        return agentExecutor.execute(agent, message, Map.of("chatOptions", chatOptions),
                sink, chatOptions.taskId(), null, channel, tag);
    }

    /**
     * 按 agentName 直接查询对应的 Debug 变体元数据。
     *
     * <p>key 是「普通 Agent 的类名」（与 gRPC 调用方传的 agentName 一致），
     * 命中即说明该 Agent 配置了调试变体；未配置返回 null，由调用方决定回落行为。</p>
     *
     * <p>新增调试变体后无需改这里，只要在 {@link #init()} 里注册一行即可被查到。</p>
     *
     * @param agentName 普通 Agent 的名称（类名）
     * @return 对应 Debug 变体的元数据；该 Agent 没有调试变体时为 null
     */
    Agent findDebugAgent(String agentName) {
        return debugAgents.get(agentName);
    }

    /**
     * 按名称找要执行的 Agent：有调试变体的先换成调试变体，
     * 其余的从平台注册表找，找不到视为调用方传错参数。
     */
    private Agent findAgent(String agentName) {
        Agent debugAgent = findDebugAgent(agentName);
        if (debugAgent != null) {
            return debugAgent;
        }
        return agentPlatform.agents().stream()
                .filter(a -> agentName.equals(a.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到名为 " + agentName + " 的 Agent"));
    }
}
