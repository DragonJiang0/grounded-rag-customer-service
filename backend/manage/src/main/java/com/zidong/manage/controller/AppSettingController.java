package com.zidong.manage.controller;

import com.zidong.common.result.PageResult;
import com.zidong.common.sse.EmitterSink;
import com.zidong.common.sse.MessageSink;
import com.zidong.dao.dto.AddPasswordWindowDto;
import com.zidong.dao.dto.ChatHistoryPageQueryDto;
import com.zidong.dao.dto.DebugAppPromptDto;
import com.zidong.dao.dto.SaveAppChatMemoryDto;
import com.zidong.dao.dto.SaveAppChatOptionsDto;
import com.zidong.dao.dto.SaveAppPromptDto;
import com.zidong.dao.dto.UpdateBotCredentialScannerDto;
import com.zidong.dao.entity.ChatHistory;
import com.zidong.dao.vo.AppSettingVO;
import com.zidong.dao.vo.BotCredentialVO;
import com.zidong.dao.vo.PasswordWindowVO;
import com.zidong.manage.service.AppSettingService;
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

/** 应用设置：Agent 提示词等 */
@RestController
@RequestMapping("/app/setting")
@RequiredArgsConstructor
public class AppSettingController extends BaseController {

    /** 调试 SSE 连接超时时间：5 分钟 */
    private static final long DEBUG_TIMEOUT_MILLIS = 5 * 60 * 1000L;

    private final AppSettingService appSettingService;

    private final PortalRpcService portalRpcService;

    /** 应用设置详情：目前只有提示词 */
    @GetMapping("/info/{appId}")
    public AppSettingVO info(@PathVariable Long appId) {
        requestLog(appId);
        return appSettingService.info(appId);
    }

    /**
     * 保存应用提示词。
     */
    @PostMapping("/savePrompt")
    public void savePrompt(@RequestBody SaveAppPromptDto request) {
        requestLog(request);
        validate(request);
        appSettingService.savePrompt(request);
    }

    /**
     * 保存应用模型对话参数。
     *
     * <p>按 appId 更新采样温度、nucleus 采样与最大输出 token。
     */
    @PostMapping("/saveChatOpts")
    public void saveChatOpts(@RequestBody SaveAppChatOptionsDto request) {
        requestLog(request);
        validate(request);
        appSettingService.saveChatOpts(request);
    }

    /**
     * 保存应用对话配置（会话记忆开关与轮数）。
     *
     * <p>按 appId 只更新对话记忆相关字段，采样温度、nucleus 采样与最大输出 token 不受影响。
     */
    @PostMapping("/saveChatMemory")
    public void saveChatMemory(@RequestBody SaveAppChatMemoryDto request) {
        requestLog(request);
        validate(request);
        appSettingService.saveChatMemory(request);
    }

    /**
     * 删除应用提示词。
     *
     * <p>删除后该应用回到「未配置提示词」状态，命中答案时直接返回标准答案。
     */
    @PostMapping("/deletePrompt/{appId}")
    public void deletePrompt(@PathVariable Long appId) {
        requestLog(appId);
        appSettingService.deletePrompt(appId);
    }

    /**
     * 新增账号密码登录窗口。
     *
     * <p>只填窗口名称与登录账号，密码由服务端初始化为默认口令（1234llm）后以密文落库，
     * 返回新增窗口的记录 ID。
     */
    @PostMapping("/createPasswordWindow/{appId}")
    public Long createPasswordWindow(@PathVariable Long appId,
                                     @RequestBody AddPasswordWindowDto request) {
        requestLog(appId, request);
        validate(request);
        return appSettingService.createPasswordWindow(appId, request);
    }

    /**
     * 删除账号密码登录窗口。
     */
    @PostMapping("/deletePasswordWindow/{id}")
    public void deletePasswordWindow(@PathVariable Long id) {
        requestLog(id);
        appSettingService.deletePasswordWindow(id);
    }

    /**
     * 重置账号密码窗口的登录密码。
     *
     * <p>重置为初始默认口令（1234llm），不改变窗口的启用/禁用状态。
     */
    @PostMapping("/resetPasswordWindowPassword/{id}")
    public void resetPasswordWindowPassword(@PathVariable Long id) {
        requestLog(id);
        appSettingService.resetPasswordWindowPassword(id);
    }

    /**
     * 账号密码窗口列表，不分页，按创建时间倒序。
     */
    @GetMapping("/passwordWindowList/{appId}")
    public List<PasswordWindowVO> passwordWindowList(@PathVariable Long appId) {
        requestLog(appId);
        return appSettingService.passwordWindowList(appId);
    }

    /**
     * 切换账号密码窗口的启用/禁用状态：调用一次即反转（启用变禁用、禁用变启用）。
     */
    @PostMapping("/togglePasswordWindowStatus/{id}")
    public void togglePasswordWindowStatus(@PathVariable Long id) {
        requestLog(id);
        appSettingService.togglePasswordWindowStatus(id);
    }

    /**
     * 微信个人号（Bot 登录凭证）列表，不分页，最新绑定的在前。
     *
     * <p>用于渲染「微信个人号」渠道的已接入列表，只返回展示字段（不含 token）；
     * status 为 1 表示已登录可用，0 表示未登录或凭证已失效（需重新扫码）。
     */
    @GetMapping("/botCredentialList/{appId}")
    public List<BotCredentialVO> botCredentialList(@PathVariable Long appId) {
        requestLog(appId);
        return appSettingService.botCredentialList(appId);
    }

    /**
     * 更新微信个人号的扫码人（人工维护的展示名称）。
     *
     * <p>凭证 ID 走 URL，请求体只带 scannerName；传空串表示清空，凭证不存在返回 404。
     */
    @PostMapping("/updateBotCredentialScanner/{botCredentialId}")
    public void updateBotCredentialScanner(@PathVariable Long botCredentialId,
                                           @RequestBody UpdateBotCredentialScannerDto request) {
        requestLog(botCredentialId, request);
        validate(request);
        appSettingService.updateBotCredentialScanner(botCredentialId, request);
    }

    /**
     * 分页查询聊天记录。
     *
     * <p>appId 走 URL，渠道与时间范围放在请求体；未传的筛选条件表示不限。
     * 返回的每条会话带 tagName：免登陆窗口为窗口名称（app_access_window.name），
     * 账号密码登录窗口为登录账号（app_password_window.username），
     * 微信个人号为扫码人（bot_credential.scanner_name），其它渠道为空。
     */
    @PostMapping("/chatHistory/{appId}")
    public PageResult<ChatHistory> chatHistory(@PathVariable Long appId,
                                               @RequestBody ChatHistoryPageQueryDto request) {
        requestLog(appId, request);
        validate(request);
        return appSettingService.chatHistoryPage(appId, request);
    }

    /**
     * 提示词调试：调 portal 的 gRPC 服务端流，把调试过程的事件原样转发到 SSE。
     *
     * <p>事件名（status / message / done / error）与 portal 的流式问答完全一致，
     * 前端复用同一套解析逻辑即可，manage 侧不再自己做模拟。
     *
     * <p>提示词校验与保存接口一致：必须同时包含 {question} 与 {answer} 占位符。
     */
    @PostMapping("/debug")
    public SseEmitter debug(@RequestBody DebugAppPromptDto request) {
        requestLog(request);
        validate(request);
        // 与保存接口同一套占位符校验

        // 参数封装成 gRPC 请求，一次性传递下去
        DebugPromptRequest.Builder builder = DebugPromptRequest.newBuilder()
                .setPrompt(request.getPrompt())
                .setQuestion(request.getQuestion())
                .setTaskId(request.getTaskId())
                .setMemoryEnabled(request.isMemoryEnabled())
                .setMemoryRounds(request.getMemoryRounds())
                .setAppId( request.getAppId());


        // ops 逐项封装：为 null 表示不覆盖，不设置该字段（proto 用 optional 记录是否传值）
        DebugAppPromptDto.Ops ops = request.getOps();
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

        DebugPromptRequest debugPromptRequest = builder.build();

        EmitterSink sink = EmitterSink.create(DEBUG_TIMEOUT_MILLIS);

        // gRPC 是阻塞式消费，放虚拟线程；异常统一在这一层兜住并收尾
        Thread.startVirtualThread(() -> {
            try {
                sink.status("开始调试");
                Iterator<DebugPromptEvent> events =
                        portalRpcService.debugPrompt(debugPromptRequest);
                while (events.hasNext()) {
                    forward(events.next(), sink);
                }
            } catch (Exception ex) {
                logger.error("提示词调试失败", ex);
                sink.fail(ex);
            }
        });

        return sink.emitter();
    }

    /**
     * 把 gRPC 流上的事件转发到 SSE：事件名一一对应、data 原样透传。
     *
     * <p>done 是正常终态，转发后由 EmitterSink 结束连接；error 同样是终态
     * （服务端随后必以 onError 结束流），所以转发后主动收尾，避免连接一直挂到超时。
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
            default -> logger.warn("提示词调试收到未知事件类型：{}", event.getEvent());
        }
    }

}
