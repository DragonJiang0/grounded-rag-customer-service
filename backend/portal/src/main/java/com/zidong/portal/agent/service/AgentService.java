package com.zidong.portal.agent.service;

import com.embabel.agent.api.annotation.support.AgentMetadataReader;
import com.embabel.agent.core.Agent;
import com.zidong.common.sse.MessageSink;
import com.zidong.dao.entity.App;
import com.zidong.dao.entity.AppAccessWindow;
import com.zidong.dao.entity.AppApiAccess;
import com.zidong.dao.entity.AppPasswordWindow;
import com.zidong.dao.mapper.AppAgentMapper;
import com.zidong.dao.mapper.AppMapper;
import com.zidong.portal.agent.AgentSelector;
import com.zidong.portal.agent.agent.AgentResult;
import com.zidong.portal.agent.agent.AppId;
import com.zidong.portal.agent.agent.ChatSession;
import com.zidong.portal.agent.agent.DefaultAgent;
import com.zidong.portal.service.ChatHistoryService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认入口的 Agent 服务：/api 阻塞与流式、免密窗口、账号密码窗口都走这里。
 *
 * <p>这些入口的黑板绑定完全一致（提示词都由 Agent 自己按 appId 去数据库取），
 * 差别只有「应用归属从哪个接入实体来」，因此各自只把自己的实体映射成 {@link AppId}，
 * Agent 侧只认 {@link AppId}，同一条对话链路被多套入口复用。
 *
 * <p>本类负责两件事：准备本入口的黑板绑定、根据 message 选定要执行的 Agent；
 * 执行细节（进程创建、结果取回、对话记录、收尾推送）全部委托 {@link AgentExecutor}。
 * 外部提示词入口（gRPC 调试）是另一个服务，见 {@link InlinePromptAgentService}。
 */
@Service
@RequiredArgsConstructor
public class AgentService {

    private final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final AgentExecutor agentExecutor;

    /**
     * 多 Agent 选择器：{@code portal.ai.laya.enabled=true} 时是 laya 决策模型，
     * false 时是大模型排序；两者互斥注册。用 Provider 取值以兼容「都不装配」的极端情况。
     */
    private final ObjectProvider<AgentSelector> agentSelectorProvider;

    /** 应用配置：多 Agent 开关按应用维度读 */
    private final AppMapper appMapper;

    /** 应用绑定的 Agent 配置：选中后要复核是否在该应用的 app_agent 范围内 */
    private final AppAgentMapper appAgentMapper;

    private final AgentMetadataReader agentMetadataReader;

    /** 默认 Agent 实例：scan=false，框架不扫描，需要在这里解析成元数据 */
    private final DefaultAgent defaultAgent;

    /** 默认 Agent 元数据，启动时解析一次 */
    private Agent defaultAgentMeta;

    @PostConstruct
    void init() {
        this.defaultAgentMeta = (Agent) agentMetadataReader.createAgentMetadata(defaultAgent);
        if (defaultAgentMeta == null) {
            throw new IllegalStateException("DefaultAgent 未被识别为 Agent");
        }
    }

    public AgentResult run(String message, AppApiAccess appApiAccess, ChatSession chatSession, Integer channel) {
        return run(message, appApiAccess, chatSession, null, channel);
    }

    /**
     * 默认入口：提示词由 Agent 按 appId 从数据库取。
     *
     * @param chatSession 会话上下文，作为黑板绑定向下传递给 action（键名与 action 参数名一致）
     */
    public AgentResult run(String message, AppApiAccess appApiAccess, ChatSession chatSession, MessageSink sink, Integer channel) {
        Map<String, Object> bindings = new HashMap<>();
        // 接入实体只在这里映射成 agent 需要的最小契约，Agent 不再依赖 AppApiAccess
        bindings.put("appId", new AppId(appApiAccess.getAppId()));
        bindings.put("chatSession", chatSession);
        // /api 的阻塞与流式入口同属 API 访问渠道，tag 取 API 接入 id
        return execute(message, bindings, sink,
                chatSession.taskId(), appApiAccess.getAppId(), channel, appApiAccess.getId());
    }

    /**
     * 免密窗口入口：与 API 入口共用默认 Agent，提示词同样按 appId 从数据库取，
     * 唯一差别是应用归属来自 {@link AppAccessWindow}。
     * <p>对话记录的 tag 取免密窗口 id（{@code app_access_window.id}），
     * 用于区分同一应用下不同免密窗口产生的会话。
     *
     * @param chatSession 会话上下文，携带 taskId 供记忆与对话记录使用（不可为空）
     */
    public AgentResult run(String message, AppAccessWindow appAccessWindow, ChatSession chatSession, MessageSink sink, Integer channel) {
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("appId", new AppId(appAccessWindow.getAppId()));
        bindings.put("chatSession", chatSession);
        return execute(message, bindings, sink,
                chatSession.taskId(), appAccessWindow.getAppId(), channel, appAccessWindow.getId());
    }

    /**
     * 账号密码登录窗口入口：与免密窗口入口完全一致，仅应用归属来自 {@link AppPasswordWindow}。
     * <p>对话记录的 tag 取登录窗口 id，供历史会话按窗口隔离查询时使用。
     *
     * @param chatSession 会话上下文，携带 taskId 供记忆与对话记录使用（不可为空）
     */
    public AgentResult run(String message, AppPasswordWindow appPasswordWindow, ChatSession chatSession, MessageSink sink, Integer channel) {
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("appId", new AppId(appPasswordWindow.getAppId()));
        bindings.put("chatSession", chatSession);
        return execute(message, bindings, sink,
                chatSession.taskId(), appPasswordWindow.getAppId(), channel, appPasswordWindow.getId());
    }

    /**
     * 微信个人号（Bot）入口：应用归属来自机器人绑定关系，而非接入实体表。
     *
     * <p>与其它入口唯一差别是 {@code appId} 由调用方（扫码绑定时记录）直接给出，
     * 不再从 {@code AppApiAccess} 等实体映射。tag 取 {@code bot_credential.id}——
     * 同一应用可被多个微信号绑定，按凭证隔离会话才能区分是哪个号在接待。</p>
     *
     * @param chatSession    会话上下文，taskId 建议用「bot- + fromUserId」，保证同一用户多轮记忆连续
     * @param botCredentialId 本次回复所用机器人的 bot_credential.id，作为对话记录标签
     */
    public AgentResult runForBot(String message, Long appId, Long botCredentialId, ChatSession chatSession) {
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("appId", new AppId(appId));
        bindings.put("chatSession", chatSession);
        return execute(message, bindings, null,
                chatSession.taskId(), appId, ChatHistoryService.CHANNEL_WECHAT_PERSONAL, botCredentialId);
    }

    /**
     * 选定 Agent 后交给执行引擎：入口之间唯一的差异是「绑定什么」，选 Agent 的策略收在这里一处。
     */
    private AgentResult execute(String message, Map<String, Object> bindings, MessageSink sink,
                                String taskId, Long appId, Integer channel, Long tag) {
        return agentExecutor.execute(selectAgent(message, appId), message, bindings, sink, taskId, appId, channel, tag);
    }

    /**
     * 选定本次对话要执行的 Agent：根据 message 选中。
     *
     * <p>多 Agent 开关关闭（默认）时固定走默认 Agent，与历史行为完全一致，不产生额外模型调用；
     * 开启时交给 {@link AgentSelector} 判断用户意图，命中且超过阈值才切 Agent：
     * {@code portal.ai.laya.enabled=true} 走 laya 决策模型，false 走大模型排序
     * （false 时 laya 模型不加载，不占堆内存）。
     *
     * <p>以下情况一律回落到默认 Agent，保证永远有 Agent 可执行：
     * 候选为空、所有 Agent 都没超过阈值（{@code select} 返回空）、
     * 选中的 Agent 不在该应用的 {@code app_agent} 配置里（排序只看 Agent 自身描述，选出来也可能未被该应用启用）。
     *
     * @param message 用户输入，既是要回答的内容也是排序依据
     * @param appId   所属应用 ID，决定是否启用多 Agent
     * @return 本次要执行的 Agent 元数据
     */
    private Agent selectAgent(String message, Long appId) {
        if (!multiAgentEnabled(appId)) {
            return defaultAgentMeta;
        }
        AgentSelector selector = agentSelectorProvider.getIfAvailable();
        if (selector == null) {
            log.info("未装配 Agent 选择器，回落默认 Agent");
            return defaultAgentMeta;
        }
        // 历史留待接入：按 tag 取该会话最近 N 轮，转成 List<ChatTurn> 后调用
        // selector.select(message, history)。当前先只按当前这句话判断。
        Agent selected = selector.select(message).orElse(null);
        if (selected == null) {
            log.info("多 Agent 未选出可执行的 Agent，回落默认 Agent");
            return defaultAgentMeta;
        }
        // 排序只按 Agent 自身的 name/description，可能选中该应用没有绑定的 Agent，需按 app_agent 复核
        if (!agentConfigured(appId, selected.getName())) {
            log.info("多 Agent 选中的 Agent 不在应用配置中，回落默认 Agent, appId={}, selected={}", appId, selected.getName());
            return defaultAgentMeta;
        }
        log.info("多 Agent 选定 Agent: {}", selected.getName());
        return selected;
    }

    /**
     * 选中的 Agent 是否在该应用（{@code app_agent}）配置的范围内。
     *
     * <p>读配置失败按「未配置」处理并回落默认 Agent：选择环节不能因为读库异常把对话打挂。
     */
    private boolean agentConfigured(Long appId, String agentName) {
        try {
            List<String> configured = appAgentMapper.listAgentNamesByAppId(appId);
            return configured.contains(agentName);
        } catch (Exception ex) {
            log.warn("读取应用 Agent 绑定失败，按未配置处理, appId={}", appId, ex);
            return false;
        }
    }

    /**
     * 多 Agent 开关：按应用维度读 {@code app.multi_agent_enabled}。
     *
     * <p>appId 为空、应用不存在（含删除后仍有请求打进来）、字段未配置或读库失败时一律按「关闭」处理：
     * 该开关只决定走不走多 Agent，读不到时退回原有单 Agent 链路，不能让对话直接失败。
     *
     * @param appId 所属应用 ID，可为 null
     * @return true 启用多 Agent
     */
    private boolean multiAgentEnabled(Long appId) {
        if (appId == null) {
            return false;
        }
        try {
            App app = appMapper.selectById(appId);
            return app != null && Boolean.TRUE.equals(app.getMultiAgentEnabled());
        } catch (Exception ex) {
            log.warn("读取应用多 Agent 开关失败，按关闭处理, appId={}", appId, ex);
            return false;
        }
    }
}
