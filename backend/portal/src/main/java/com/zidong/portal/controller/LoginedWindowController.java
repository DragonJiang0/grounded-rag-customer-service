package com.zidong.portal.controller;

import com.zidong.common.sse.EmitterSink;
import com.zidong.dao.dto.ChangePasswordDto;
import com.zidong.dao.dto.ChatDto;
import com.zidong.dao.dto.DeleteWindowHistoryDto;
import com.zidong.dao.entity.AppPasswordWindow;
import com.zidong.dao.entity.ChatHistory;
import com.zidong.portal.agent.agent.ChatSession;
import com.zidong.portal.agent.service.AgentService;
import com.zidong.portal.security.SecurityUtils;
import com.zidong.portal.service.ChatHistoryService;
import com.zidong.portal.service.OpenLoginedWindowService;
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
 * /loginedwindow 账号密码登录窗口对外接口：先经 /open/loginedwindow/login 登录拿到令牌，
 * 之后携带令牌（Authorization: Bearer）进入对话。
 *
 * <p>与 /window 免密窗口一一对应，差别仅在身份来源：本前缀由 {@code LoginedWindowFilter}
 * 按 {@code app_password_window.token} 校验并还原窗口（见 PortalSecurityConfig），
 * 窗口信息取自 {@link AppPasswordWindow}，对话记录渠道为「账号密码登录窗口」。
 */
@RestController
@RequestMapping("/loginedwindow")
@RequiredArgsConstructor
public class LoginedWindowController extends BaseController {

    private final AgentService agentService;

    private final ChatHistoryService chatHistoryService;

    private final OpenLoginedWindowService openLoginedWindowService;

    /** 修改当前登录窗口的密码 */
    @PostMapping("/changePassword")
    public void changePassword(@Valid @RequestBody ChangePasswordDto changePasswordDto) {
        // 密码不打印到日志
        requestLog();
        openLoginedWindowService.changePassword(changePasswordDto);
    }

    /** 查询登录窗口信息（取自 app_password_window） */
    @GetMapping("/info")
    public AppPasswordWindow info() {
        requestLog();
        return SecurityUtils.currentAppPasswordWindow();
    }

    /**
     * 查询账号密码登录窗口的历史会话列表：身份已由 LoginedWindowFilter 按登录令牌还原，
     * 应用归属取自当前窗口，窗口 id 作为标签用于隔离各窗口的会话。
     *
     * <p>只取「账号密码登录窗口」渠道（channel=3）且标签等于当前窗口 id 的记录，创建时间倒序，不分页。
     */
    @GetMapping("/history")
    public List<ChatHistory> history() {
        requestLog();
        AppPasswordWindow appPasswordWindow = SecurityUtils.currentAppPasswordWindow();
        return chatHistoryService.listAccountHistory(appPasswordWindow.getAppId(), appPasswordWindow.getId());
    }

    /**
     * 删除历史会话（软删除）：仅把该记录的 customer_deleted 置为 true，列表随即不再返回。
     *
     * <p>入参只有会话标识：应用归属与窗口隔离<b>只能</b>取当前登录窗口（登录令牌已由 LoginedWindowFilter
     * 校验并还原为 AppPasswordWindow），不接受调用方传入，避免越权删除。
     * 再按「应用 + 渠道（账号密码登录窗口）+ 窗口 id + 会话标识」整段置为已删除，条件缺一不可。
     */
    @PostMapping("/history/delete")
    public void deleteHistory(@Valid @RequestBody DeleteWindowHistoryDto deleteWindowHistoryDto) {
        requestLog(deleteWindowHistoryDto);
        AppPasswordWindow appPasswordWindow = SecurityUtils.currentAppPasswordWindow();
        chatHistoryService.deleteAccountHistory(
                appPasswordWindow.getAppId(),
                appPasswordWindow.getId(),
                deleteWindowHistoryDto.getConversationId());
    }

    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@Valid @RequestBody ChatDto chatDto) {
        requestLog(chatDto);
        validate(chatDto);
        EmitterSink emitterSink = EmitterSink.create(60*1000);

        AppPasswordWindow appPasswordWindow = SecurityUtils.currentAppPasswordWindow();

        // taskId 由页面生成并保证唯一，后端直接沿用：不做存在性校验，也无需回传
        String taskId = chatDto.getTaskId();
        log.debug("chat/stream 会话标识 taskId={}", taskId);
        ChatSession chatSession = new ChatSession(taskId);

        // Agent 执行是阻塞调用，放虚拟线程；sink 抛出的运行时异常统一在这一层兜住
        Thread.startVirtualThread(() -> {
            long startNanos = System.nanoTime();
            try {
                agentService.run(chatDto.getMessage(), appPasswordWindow, chatSession, emitterSink, ChatHistoryService.CHANNEL_ACCOUNT);
            } catch (Exception ex) {
                log.error("任务失败", ex);
                emitterSink.fail(ex);
            } finally {
                logAnswerCost("/loginedwindow/chat/stream", taskId, startNanos);
                SecurityContextHolder.clearContext();
            }
        });


        return emitterSink.emitter();

    }
}
