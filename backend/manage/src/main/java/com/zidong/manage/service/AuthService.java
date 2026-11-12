package com.zidong.manage.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.common.exception.BizException;
import com.zidong.dao.dto.LoginRequest;
import com.zidong.dao.entity.SysLoginSession;
import com.zidong.dao.vo.LoginResponse;
import com.zidong.dao.entity.SysUser;
import com.zidong.dao.mapper.SysUserMapper;
import com.zidong.manage.security.AuthSessionService;
import com.zidong.manage.security.JwtTokenProvider;
import com.zidong.manage.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** 登录认证 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int STATUS_ENABLED = 1;

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthSessionService authSessionService;

    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.getUsername().trim()));

        // 用户不存在与密码错误给同一个提示，避免暴露账号是否存在
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != STATUS_ENABLED) {
            throw BizException.forbidden("账号已停用，请联系管理员");
        }

        SysLoginSession session = authSessionService.createSession(user.getId(), user.getUsername());
        String token = tokenProvider.generateSessionToken(session.getToken());
        return new LoginResponse(token, "Bearer");
    }

    /** 登出 */
    public void logout() {
        authSessionService.invalidate(SecurityUtils.currentUser().getToken());
    }

}
