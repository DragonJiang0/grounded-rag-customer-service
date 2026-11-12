package com.zidong.portal.filter;

import com.zidong.dao.entity.AppPasswordWindow;
import com.zidong.portal.service.OpenLoginedWindowService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * /loginedwindow 前缀专用过滤器：该前缀全部接口都需要认证。
 *
 * <p>拦截由 {@code authorizeHttpRequests(...).anyRequest().authenticated()} 负责，
 * 本过滤器只负责从 Authorization 头解析登录令牌（登录接口签发的 token）并写入
 * SecurityContext，把 {@link AppPasswordWindow} 作为 principal；
 * 没有令牌或令牌无效（不存在 / 过期 / 账号停用）时保持匿名，交给授权规则返回 401。
 */
@Component
@RequiredArgsConstructor
public class LoginedWindowFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginedWindowFilter.class);

    /** 令牌请求头 */
    private static final String HEADER_NAME = "Authorization";
    /** 令牌前缀 */
    private static final String TOKEN_PREFIX = "Bearer ";

    private final OpenLoginedWindowService openLoginedWindowService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        // 拿到令牌即封装 Authentication 继续往下走；没拿到就保持匿名
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(request, token);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 校验登录令牌通过则封装 Authentication 放入 SecurityContext；
     * 校验不通过时不写 SecurityContext（保持匿名），由授权规则统一处理。
     */
    private void authenticate(HttpServletRequest request, String token) {
        AppPasswordWindow appPasswordWindow = load(request, token);
        if (appPasswordWindow == null) {
            log.debug("账号密码登录窗口令牌无效，按未认证处理");
            return;
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(appPasswordWindow, token, List.of());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    /** 按令牌校验并查询登录窗口，无效返回 null */
    protected AppPasswordWindow load(HttpServletRequest request, String token) {
        return openLoginedWindowService.requireByToken(token);
    }

    /** 从 Authorization 头解析 Bearer 令牌，未携带返回 null */
    private static String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_NAME);
        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            return null;
        }
        String token = header.substring(TOKEN_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
