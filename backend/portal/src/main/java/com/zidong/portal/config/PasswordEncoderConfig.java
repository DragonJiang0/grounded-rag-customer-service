package com.zidong.portal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码校验器配置：账号密码登录窗口的口令以 BCrypt 密文存储，登录时比对。
 *
 * <p>单独成类是为了打断循环依赖：本 Bean 被 {@code OpenLoginedWindowService} 依赖，
 * 而 {@link PortalSecurityConfig} 又通过构造函数注入了 LoginedWindowFilter
 * → OpenLoginedWindowService。若把本 Bean 放在 {@link PortalSecurityConfig} 里，会形成
 * 「造 config 需要 filter → 造 filter 需要 service → 造 service 需要 passwordEncoder
 * → 造 passwordEncoder 需要 config 实例」的环。
 *
 * <p>本类不依赖任何 filter / service，Spring 可直接实例化本类并产出本 Bean，
 * 从而让创建顺序变为：passwordEncoder → service → filter → config，环即被打断。
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
