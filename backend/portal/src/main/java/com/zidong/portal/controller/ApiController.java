package com.zidong.portal.controller;

import com.zidong.common.sse.EmitterSink;
import com.zidong.common.sse.MessageSink;
import com.zidong.portal.agent.agent.ChatSession;
import com.zidong.portal.agent.service.AgentService;
import com.zidong.dao.dto.ChatDto;
import com.zidong.dao.dto.ChatResult;
import com.zidong.dao.entity.AppApiAccess;
import com.zidong.portal.security.SecurityUtils;
import com.zidong.portal.service.ChatHistoryService;
import com.zidong.portal.service.ChatMemoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** /api 对外接口 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController extends BaseController {



    private final AgentService agentService;

    private final ChatMemoryService chatMemoryService;

    @PostMapping("/chat/block")
    public ChatResult chatBlock(@Valid @RequestBody ChatDto chatDto) {
        AppApiAccess appApiAccess = SecurityUtils.currentAppApiAccess();

        // 带了 taskId 则校验会话确实存在（无效标识直接回错，不进入 Agent 执行）；没带则生成一个新的
        String taskId = chatMemoryService.resolveTaskId(appApiAccess.getAppId(), chatDto.getTaskId());
        ChatSession chatSession = new ChatSession(taskId);

        long startNanos = System.nanoTime();
        try {
            String message = agentService.run(chatDto.getMessage(), appApiAccess, chatSession, ChatHistoryService.CHANNEL_API).output();
            return new ChatResult(taskId, message);
        } finally {
            logAnswerCost("/api/chat/block", taskId, startNanos);
        }
    }

    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@Valid @RequestBody ChatDto chatDto) {
        EmitterSink emitterSink = EmitterSink.create(60*1000);

        AppApiAccess appApiAccess = SecurityUtils.currentAppApiAccess();

        // 与 /chat/block 同一套校验：taskId 无效时在开场前同步抛错（400），不进入流式过程
        String taskId = chatMemoryService.resolveTaskId(appApiAccess.getAppId(), chatDto.getTaskId());
        emitterSink.send(MessageSink.TASK_ID, taskId);
        log.debug("chat/stream 会话标识 taskId={}", taskId);
        ChatSession chatSession = new ChatSession(taskId);

        // Agent 执行是阻塞调用，放虚拟线程；sink 抛出的运行时异常统一在这一层兜住
        Thread.startVirtualThread(() -> {
            long startNanos = System.nanoTime();
            try {
                agentService.run(chatDto.getMessage(),appApiAccess, chatSession, emitterSink, ChatHistoryService.CHANNEL_API);
            } catch (Exception ex) {
                log.error("任务失败", ex);
                emitterSink.fail(ex);
            } finally {
                logAnswerCost("/api/chat/stream", taskId, startNanos);
                SecurityContextHolder.clearContext();
            }
        });


        return emitterSink.emitter();
    }
}
