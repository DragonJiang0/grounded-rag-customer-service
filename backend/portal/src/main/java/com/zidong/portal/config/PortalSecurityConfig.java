package com.zidong.portal.config;

import com.alibaba.fastjson.JSON;
import com.zidong.portal.filter.ApiFilter;
import com.zidong.portal.filter.LoginedWindowFilter;
import com.zidong.portal.filter.WindowFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** portal 模块 Spring Security 配置 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class PortalSecurityConfig {

    /** 全局白名单：无需认证，且完全绕过各前缀认证过滤器 */
    private static final String[] WHITELIST = {
            "/error", "/static/**",
            // 供管理台查询 Agent 定义，不涉及业务数据，无需 appKey；
            // 账号密码登录窗口的开放接口（登录 / 退出），对应 OpenLoginedWindowController
            "/open/loginedwindow/login", "/open/loginedwindow/logout"
    };

    private final ApiFilter apiFilter;
    private final WindowFilter windowFilter;
    private final LoginedWindowFilter loginedWindowFilter;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    /** 全局白名单链，最先匹配 */
    @Bean
    @Order(0)
    public SecurityFilterChain whitelistChain(HttpSecurity http) throws Exception {
        http.securityMatcher(WHITELIST)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /** /api 前缀：全部接口都需要认证，只经过 ApiFilter */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher( "/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(apiFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** /window 前缀：需携带 windowKey（Authorization: Bearer），由 WindowFilter 校验并还原身份 */
    @Bean
    @Order(2)
    public SecurityFilterChain windowFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher( "/window/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(windowFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();

    }

    /**
     * /loginedwindow 前缀：需携带登录令牌（Authorization: Bearer），
     * 由 LoginedWindowFilter 按 app_password_window.token 校验并还原身份。
     */
    @Bean
    @Order(3)
    public SecurityFilterChain loginedWindowFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher( "/loginedwindow/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(loginedWindowFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 兜底链，必须放在最后 */
    @Bean
    @Order(4)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * 未认证统一返回 401 JSON。
     * 不配置时 Spring Security 默认走 Http403ForbiddenEntryPoint，会返回 403 且响应体为空。
     * <p>必须是 static：静态 @Bean 方法不需要配置类实例就能创建，
     * 否则「构造函数注入它」与「它由本类实例方法创建」会互相等待，形成循环依赖。
     */
    @Bean
    public static AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", HttpStatus.UNAUTHORIZED.value());
            body.put("message", "未登录或登录已过期");
            body.put("path", request.getRequestURI());
            response.getWriter().write(JSON.toJSONString(body));
        };
    }

    /** 关闭 ApiFilter 的 Servlet 自动注册 */
    @Bean
    public FilterRegistrationBean<ApiFilter> apiFilterRegistration(ApiFilter filter) {
        FilterRegistrationBean<ApiFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
