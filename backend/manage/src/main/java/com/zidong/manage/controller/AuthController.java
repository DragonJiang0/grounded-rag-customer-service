package com.zidong.manage.controller;

import com.zidong.dao.dto.LoginRequest;
import com.zidong.dao.vo.LoginResponse;
import com.zidong.manage.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthService authService;

    // 唯一无需认证的接口（见 SecurityConfig）
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        requestLog(request.getUsername());
        validate(request);
        return authService.login(request);
    }

    /** 登出：销毁当前令牌对应的登录态（需携带有效令牌，见 SecurityConfig） */
    @PostMapping("/logout")
    public void logout() {
        authService.logout();
    }
}
