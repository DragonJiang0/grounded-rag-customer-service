package com.zidong.portal.service;

import com.zidong.common.exception.BizException;
import com.zidong.dao.dto.ChangePasswordDto;
import com.zidong.dao.dto.LoginRequest;
import com.zidong.dao.entity.App;
import com.zidong.dao.entity.AppPasswordWindow;
import com.zidong.dao.mapper.AppMapper;
import com.zidong.dao.mapper.AppPasswordWindowMapper;
import com.zidong.portal.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * 「账号密码登录窗口」的开放接口业务：登录签发登录令牌、退出清空登录令牌。
 *
 * <p>登录态直接承载在 {@code app_password_window} 上：登录成功写入 {@code token} 与
 * {@code token_expire_at}，退出时按令牌清空。该表既是账号密码来源，也是登录会话载体。
 *
 * <p>登录账号按应用隔离，请求需携带 {@code appKey}：先由 AppKey 定位应用，再按
 * 「应用 + 账号」定位登录窗口。
 */
@Service
@RequiredArgsConstructor
public class OpenLoginedWindowService {

    /** 登录令牌有效期：7 天 */
    private static final Duration TOKEN_TTL = Duration.ofDays(7);

    /** 令牌请求头（与各过滤器保持一致） */
    private static final String HEADER_NAME = "Authorization";
    /** 令牌前缀 */
    private static final String TOKEN_PREFIX = "Bearer ";

    private final AppMapper appMapper;
    private final AppPasswordWindowMapper passwordWindowMapper;
    private final PasswordEncoder passwordEncoder;
    private final HttpServletRequest request;

    /**
     * 账号密码登录：先按 AppKey 定位应用，再按「应用 + 账号」定位登录窗口，
     * 校验通过后签发登录令牌并刷新最后使用时间。
     *
     * @param loginRequest 登录请求，含 AppKey、登录账号与明文密码
     * @return 新签发的登录令牌
     * @throws BizException 缺少 AppKey（400）、AppKey 无效（401）、账号或密码错误（401）、账号被禁用（403）
     */
    public String login(LoginRequest loginRequest) {
        String appKey = StringUtils.trimToNull(loginRequest.getAppKey());
        if (appKey == null) {
            throw BizException.badRequest("缺少应用 AppKey");
        }
        // 账号按应用隔离，先由 AppKey 找到应用，再用 appId 定位登录窗口
        App app = appMapper.findByAppKey(appKey);
        if (app == null) {
            throw BizException.unauthorized("应用不存在或 AppKey 无效");
        }

        String username = loginRequest.getUsername().trim();
        AppPasswordWindow window = passwordWindowMapper.findByAppIdAndUsername(app.getId(), username);

        // 账号不存在与密码错误给同一提示，避免暴露账号是否存在
        if (window == null || !passwordEncoder.matches(loginRequest.getPassword(), window.getPassword())) {
            throw BizException.unauthorized("账号或密码错误");
        }
        if (!Objects.equals(window.getStatus(), AppPasswordWindow.STATUS_ENABLED)) {
            throw BizException.forbidden("账号已停用，请联系管理员");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        passwordWindowMapper.updateLoginToken(window.getId(), token,
                now.plus(TOKEN_TTL.toMillis(), ChronoUnit.MILLIS));
        // 登录即视为一次使用，刷新最后使用时间
        passwordWindowMapper.touchLastUsed(window.getId());
        return token;
    }

    /**
     * 按登录令牌校验并返回登录窗口，同时刷新最后使用时间。
     *
     * <p>供 {@code /loginedwindow/**} 的认证过滤器使用：令牌为空、不存在、已过期或账号已停用
     * 一律返回 null（保持匿名），由授权规则统一返回 401，不在此处抛业务异常。
     *
     * @param token Authorization 头中的 Bearer 令牌
     * @return 有效登录窗口；无效返回 null
     */
    public AppPasswordWindow requireByToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        AppPasswordWindow window = passwordWindowMapper.findByToken(token);
        if (window == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        // 令牌过期或无有效登录态（未签发 / 已退出）均视为无效
        if (window.getTokenExpireAt() == null || !window.getTokenExpireAt().isAfter(now)) {
            return null;
        }
        if (!Objects.equals(window.getStatus(), AppPasswordWindow.STATUS_ENABLED)) {
            return null;
        }
        // 使用一次即刷新最后使用时间
        passwordWindowMapper.touchLastUsed(window.getId());
        window.setLastUsedAt(now);
        return window;
    }

    /**
     * 修改当前登录窗口的登录密码：原密码需与库中 BCrypt 密文匹配，新密码加密后保存。
     *
     * <p>当前窗口由登录过滤器按令牌还原，只允许修改自己账号的密码。
     *
     * @param changePasswordDto 含原密码与新密码
     * @throws BizException 账号不存在（404）、原密码错误或新密码与原密码相同（400）
     */
    public void changePassword(ChangePasswordDto changePasswordDto) {
        AppPasswordWindow window = SecurityUtils.currentAppPasswordWindow();
        AppPasswordWindow current = passwordWindowMapper.selectById(window.getId());
        if (current == null) {
            throw BizException.notFound("账号不存在");
        }
        if (!passwordEncoder.matches(changePasswordDto.getOldPassword(), current.getPassword())) {
            throw BizException.badRequest("原密码错误");
        }
        if (passwordEncoder.matches(changePasswordDto.getNewPassword(), current.getPassword())) {
            throw BizException.badRequest("新密码不能与原密码相同");
        }
        passwordWindowMapper.updatePassword(current.getId(),
                passwordEncoder.encode(changePasswordDto.getNewPassword()));
    }

    /**
     * 退出登录：按请求头中的令牌清空对应登录态。
     *
     * <p>未携带令牌时按成功处理（幂等），不抛异常。
     */
    public void logout() {
        String token = resolveToken();
        if (StringUtils.isNotBlank(token)) {
            passwordWindowMapper.clearLoginToken(token);
        }
    }

    /** 从 Authorization 头解析 Bearer 令牌，未携带返回 null */
    private String resolveToken() {
        String header = request.getHeader(HEADER_NAME);
        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            return null;
        }
        String token = header.substring(TOKEN_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
