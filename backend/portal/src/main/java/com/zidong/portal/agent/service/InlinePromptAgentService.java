package com.zidong.portal.agent.service;

import com.embabel.agent.api.annotation.support.AgentMetadataReader;
import com.embabel.agent.core.Agent;
import com.zidong.common.sse.MessageSink;
import com.zidong.portal.agent.agent.AgentResult;
import com.zidong.portal.agent.agent.InlinePrompt;
import com.zidong.portal.agent.agent.InlinePromptAgent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 外部提示词入口的 Agent 服务：提示词与对话参数随请求带进来，不读数据库配置。
 *
 * <p>该入口目前只有 manage 经 gRPC 的调试渠道使用，因此不参与多 Agent 选择——调试要求结果可复现，
 * 恒走内置外部提示词 Agent；执行流程委托 {@link AgentExecutor}，
 * 与默认入口共用同一套引擎（黑板绑定、进程创建、落库、收尾）。
 */
@Service
@RequiredArgsConstructor
public class InlinePromptAgentService {

    private final AgentExecutor agentExecutor;

    private final AgentMetadataReader agentMetadataReader;

    /** 外部提示词 Agent 实例：scan=false，框架不扫描，需要在这里解析成元数据 */
    private final InlinePromptAgent inlinePromptAgent;

    /** 外部提示词 Agent 元数据，启动时解析一次 */
    private Agent inlinePromptAgentMeta;

    @PostConstruct
    void init() {
        this.inlinePromptAgentMeta = (Agent) agentMetadataReader.createAgentMetadata(inlinePromptAgent);
        if (inlinePromptAgentMeta == null) {
            throw new IllegalStateException("InlinePromptAgent 未被识别为 Agent");
        }
    }

    /**
     * 外部提示词入口：提示词与对话参数都随本次请求带进来，不读数据库配置。
     *
     * @param chatOptions 本次请求的对话参数，直接向下传递，不做包装
     * @param tag         对话记录标签；该入口目前只有调试渠道使用，没有接入实体，固定传 null
     * @return Agent 的最终结果
     */
    public AgentResult run(String message, InlinePrompt inlinePrompt, ChatOptions chatOptions, MessageSink sink, Integer channel, Long tag) {
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("inlinePrompt", inlinePrompt);
        bindings.put("chatOptions", chatOptions);
        return agentExecutor.execute(inlinePromptAgentMeta, message, bindings, sink,
                chatOptions.taskId(), inlinePrompt.appId(), channel, tag);
    }
}
