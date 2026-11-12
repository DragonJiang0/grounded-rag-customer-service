package com.zidong.portal.controller;

import com.zidong.common.sse.EmitterSink;
import com.zidong.dao.dto.ChatDto;
import com.zidong.dao.dto.DeleteWindowHistoryDto;
import com.zidong.dao.dto.WindowChatDto;
import com.zidong.dao.entity.AppAccessWindow;
import com.zidong.dao.entity.AppApiAccess;
import com.zidong.dao.entity.ChatHistory;
import com.zidong.portal.agent.agent.ChatSession;
import com.zidong.portal.agent.service.AgentService;
import com.zidong.portal.security.SecurityUtils;
import com.zidong.portal.service.AccessWindowService;
import com.zidong.portal.service.ChatHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * /window 免密窗口对外接口：携带 window_key 直接进入对话，无需登录。
 *
 * <p>该前缀不匹配 /api、/logined，落到 portal 的兜底放行链（permitAll），
 * 因此不做认证过滤，window_key 由 {@link AccessWindowService} 校验。
 */
@RestController
@RequestMapping("/window")
@RequiredArgsConstructor
public class AccessWindowController extends BaseController {

    private final AgentService agentService;

    private final ChatHistoryService chatHistoryService;

    /** 查询窗口信息 */
    @GetMapping("/info")
    public AppAccessWindow info() {
        requestLog();
        return SecurityUtils.currentAppAccessWindow();
    }

    /**
     * 查询免登录窗口的历史会话列表：身份已由 WindowFilter 按 windowKey 还原，
     * 应用归属取自当前窗口，窗口 id 作为标签用于隔离各窗口的会话。
     *
     * <p>只取「免登陆窗口」渠道（channel=2）且标签等于当前窗口 id 的记录，创建时间倒序，不分页。
     */
    @GetMapping("/history")
    public List<ChatHistory> history() {
        requestLog();
        AppAccessWindow appAccessWindow = SecurityUtils.currentAppAccessWindow();
        return chatHistoryService.listWindowHistory(appAccessWindow.getAppId(), appAccessWindow.getId());
    }

    /**
     * 删除历史会话（软删除）：仅把该记录的 customer_deleted 置为 true，列表随即不再返回。
     *
     * <p>入参只有会话标识：应用归属<b>只能</b>取当前免密窗口（windowKey 已由 WindowFilter 校验并还原为
     * AppAccessWindow），不接受调用方传入的 AppKey，避免拿别人的凭证删本应用的数据。
     * 再按「应用 + 会话标识」把该会话的所有分段整段置为已删除，两个条件缺一不可。
     */
    @PostMapping("/history/delete")
    public void deleteHistory(@Valid @RequestBody DeleteWindowHistoryDto deleteWindowHistoryDto) {
        requestLog(deleteWindowHistoryDto);
        AppAccessWindow appAccessWindow = SecurityUtils.currentAppAccessWindow();
        chatHistoryService.deleteWindowHistory(
                appAccessWindow.getAppId(),
                deleteWindowHistoryDto.getConversationId());
    }


    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@Valid @RequestBody ChatDto chatDto) {
        requestLog(chatDto);
        validate(chatDto);
        EmitterSink emitterSink = EmitterSink.create(60*1000);

        AppAccessWindow appAccessWindow = SecurityUtils.currentAppAccessWindow();

        // taskId 由页面生成并保证唯一，后端直接沿用：不做存在性校验，也无需回传
        String taskId = chatDto.getTaskId();
        log.debug("chat/stream 会话标识 taskId={}", taskId);
        ChatSession chatSession = new ChatSession(taskId);

        // Agent 执行是阻塞调用，放虚拟线程；sink 抛出的运行时异常统一在这一层兜住
        Thread.startVirtualThread(() -> {
            long startNanos = System.nanoTime();
            try {
                agentService.run(chatDto.getMessage(), appAccessWindow, chatSession, emitterSink, ChatHistoryService.CHANNEL_WINDOW);
            } catch (Exception ex) {
                log.error("任务失败", ex);
                emitterSink.fail(ex);
            } finally {
                logAnswerCost("/window/chat/stream", taskId, startNanos);
                SecurityContextHolder.clearContext();
            }
        });


        return emitterSink.emitter();

}
}
