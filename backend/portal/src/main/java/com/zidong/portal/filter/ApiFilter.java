package com.zidong.portal.filter;

import com.zidong.dao.entity.AppApiAccess;
import com.zidong.dao.mapper.AppApiAccessMapper;
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
import java.time.LocalDateTime;
import java.util.List;

/**
 * /api 前缀专用过滤器：/api/** 全部接口都需要认证。
 * <p>拦截由 {@code authorizeHttpRequests(...).anyRequest().authenticated()} 负责，
 * 本过滤器只负责从 Authorization 头解析令牌并写入 SecurityContext；
 * 没有令牌时保持匿名，交给授权规则返回 401。
 */
@Component
@RequiredArgsConstructor
public class ApiFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiFilter.class);

    /** 令牌请求头 */
    private static final String HEADER_NAME = "Authorization";
    /** 令牌前缀 */
    private static final String TOKEN_PREFIX = "Bearer ";

    private final AppApiAccessMapper appApiAccessMapper;

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
     * 校验 Access Key 通过则封装 Authentication 放入 SecurityContext；
     * 校验不通过时不写 SecurityContext（保持匿名），由授权规则统一处理。
     */
    private void authenticate(HttpServletRequest request, String token) {
        AppApiAccess appApiAccess = loadApiKey(request, token);
        if (appApiAccess == null) {
            log.debug("API Key 不存在，按未认证处理：{}", token);
            return;
        }

        // expireAt 为 null 表示长期有效，否则必须晚于当前时间
        LocalDateTime expireAt = appApiAccess.getExpireAt();
        if (expireAt != null && !expireAt.isAfter(LocalDateTime.now())) {
            log.debug("API Key 已过期，按未认证处理：expireAt={}", expireAt);
            return;
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(appApiAccess, token, List.of());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    /** 按令牌查询接入信息，查不到返回 null */
    protected AppApiAccess loadApiKey(HttpServletRequest request, String token) {
        return appApiAccessMapper.findByApiKey(token);
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
