package com.zidong.portal;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

/** portal 模块启动类 */
@SpringBootApplication(scanBasePackages = "com.zidong", exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan("com.zidong")
@MapperScan("com.zidong.dao.mapper")
@EnableScheduling
public class PortalApplication {

    private static final Logger log = LoggerFactory.getLogger(PortalApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(PortalApplication.class, args);
        Environment env = context.getEnvironment();
        String port = env.getProperty("server.port", "9090");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        log.info("====================================================");
        log.info("  {} 启动成功！访问地址: http://localhost:{}{}", env.getProperty("spring.application.name", "portal"), port, contextPath);
        log.info("====================================================");
    }
}
