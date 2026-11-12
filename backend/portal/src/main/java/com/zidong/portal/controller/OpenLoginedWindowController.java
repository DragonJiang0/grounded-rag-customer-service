package com.zidong.portal.controller;

import com.zidong.dao.dto.LoginRequest;
import com.zidong.portal.service.OpenLoginedWindowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 「账号密码登录窗口」的开放接口（无需认证）：账号密码登录 / 退出登录。
 *
 * <p>对应表 {@code app_password_window}：登录校验该表的账号密码，登录成功签发登录令牌。
 *
 * <p>两个接口都位于 /open 前缀下，且已加入全局白名单（见 PortalSecurityConfig），
 * 不经过任何认证过滤器；登录令牌由业务侧（{@link OpenLoginedWindowService}）自行校验。
 */
@RestController
@RequestMapping("/open/loginedwindow")
@RequiredArgsConstructor
public class OpenLoginedWindowController extends BaseController {

    private final OpenLoginedWindowService openLoginedWindowService;

    /**
     * 账号密码登录。
     *
     * @param loginRequest 登录请求，含 AppKey、登录账号与密码
     * @return 登录令牌（明文返回，前端据此后置到 Authorization 头）
     */
    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginRequest loginRequest) {
        requestLog(loginRequest.getUsername());
        return openLoginedWindowService.login(loginRequest);
    }

    /** 退出登录：销毁当前令牌对应的登录态（令牌取自 Authorization 头）。 */
    @PostMapping("/logout")
    public void logout() {
        requestLog();
        openLoginedWindowService.logout();
    }
}
