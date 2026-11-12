package com.zidong.manage.controller;

import com.zidong.common.sse.EmitterSink;
import com.zidong.common.sse.MessageSink;
import com.zidong.dao.dto.DebugAgentPromptDto;
import com.zidong.dao.dto.SaveAgentPromptDto;
import com.zidong.dao.vo.AgentSettingVO;
import com.zidong.dao.vo.AgentVO;
import com.zidong.manage.service.AgentService;
import com.zidong.rpc.api.client.starter.service.PortalRpcService;
import com.zidong.rpc.service.DebugPromptEvent;
import com.zidong.rpc.service.DebugPromptRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Iterator;
import java.util.List;

/** Agent 设置 */
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController extends BaseController {

    /** 调试 SSE 连接超时时间：5 分钟，与应用调试保持一致 */
    private static final long DEBUG_TIMEOUT_MILLIS = 5 * 60 * 1000L;

    private final AgentService agentService;

    private final PortalRpcService portalRpcService;

    /**
     * 查询 portal 上已注册的 Agent 列表。
     *
     * <p>数据经 gRPC 取自 portal 的 AgentPlatform 注册表，只返回名称、描述、
     * goal 与 action 的名称描述，用于管理台展示与后续绑定选择。</p>
     */
    @GetMapping("/list")
    public List<AgentVO> list() {
        requestLog();
        return agentService.list();
    }

    /** Agent 调试设置详情：目前只有提示词，未配置时 content 为空串 */
    @GetMapping("/setting/info/{agentName}")
    public AgentSettingVO settingInfo(@PathVariable String agentName) {
        requestLog(agentName);
        return agentService.settingInfo(agentName);
    }

    /** 保存 Agent 调试提示词：同一 Agent 重复保存会覆盖更新 */
    @PostMapping("/setting/savePrompt")
    public void savePrompt(@RequestBody SaveAgentPromptDto request) {
        requestLog(request);
        validate(request);
        agentService.savePrompt(request);
    }

    /** 删除 Agent 调试提示词：删除后回到「未配置提示词」状态，重复删除不会报错 */
    @PostMapping("/setting/deletePrompt/{agentName}")
    public void deletePrompt(@PathVariable String agentName) {
        requestLog(agentName);
        agentService.deletePrompt(agentName);
    }

    /**
     * Agent 调试对话（SSE 流）：POST /agent/debug
     *
     * <p>与 {@code AppSettingController#debug} 同一套事件协议（status / message / done / error），
     * 区别只在于 gRPC 请求带 agentName：portal 侧按名称执行指定 Agent，
     * 不走应用语料命中链路。</p>
     */
    @PostMapping("/debug")
    public SseEmitter debug(@RequestBody DebugAgentPromptDto request) {
        requestLog(request);
        validate(request);

        DebugPromptRequest.Builder builder = DebugPromptRequest.newBuilder()
                .setAgentName(request.getAgentName())
                .setPrompt(request.getPrompt() == null ? "" : request.getPrompt())
                .setQuestion(request.getQuestion())
                .setTaskId(request.getTaskId())
                .setMemoryEnabled(request.isMemoryEnabled())
                .setMemoryRounds(request.getMemoryRounds());

        // ops 逐项封装：为 null 表示不覆盖，不设置该字段（proto 用 optional 记录是否传值）
        DebugAgentPromptDto.Ops ops = request.getOps();
        if (ops != null) {
            if (ops.getTemperature() != null) {
                builder.setTemperature(ops.getTemperature().doubleValue());
            }
            if (ops.getTopP() != null) {
                builder.setTopP(ops.getTopP().doubleValue());
            }
            if (ops.getMaxOutputTokens() != null) {
                builder.setMaxOutputTokens(ops.getMaxOutputTokens());
            }
        }

        EmitterSink sink = EmitterSink.create(DEBUG_TIMEOUT_MILLIS);

        // gRPC 是阻塞式消费，放虚拟线程；异常统一在这一层兜住并收尾
        Thread.startVirtualThread(() -> {
            try {
                sink.status("开始调试");
                Iterator<DebugPromptEvent> events =
                        portalRpcService.debugPrompt(builder.build());
                while (events.hasNext()) {
                    forward(events.next(), sink);
                }
            } catch (Exception ex) {
                logger.error("Agent 调试失败", ex);
                sink.fail(ex);
            }
        });

        return sink.emitter();
    }

    /**
     * 把 gRPC 流上的事件转发到 SSE：事件名一一对应、data 原样透传。
     * 与 AppSettingController#forward 同一套语义。
     */
    private void forward(DebugPromptEvent event, MessageSink sink) {
        String data = event.getData();
        switch (event.getEvent()) {
            case MessageSink.STATUS -> sink.status(data);
            case MessageSink.MESSAGE -> sink.message(data);
            case MessageSink.DONE -> sink.done(data);
            case MessageSink.ERROR -> {
                sink.error(data);
                sink.complete();
            }
            default -> logger.warn("Agent 调试收到未知事件类型：{}", event.getEvent());
        }
    }
}
