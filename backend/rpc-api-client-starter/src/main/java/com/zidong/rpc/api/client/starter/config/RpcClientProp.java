package com.zidong.rpc.api.client.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @Author ZiDong
 * @create 2025 2025/7/3 10:16
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "rpc.client")
public class RpcClientProp {

    /**
     * 全局默认调用超时（秒），各服务可单独覆盖。
     *
     * <p>注意：服务端流的 deadline 覆盖的是**整条流**而不是单条消息，
     * 一次问答里可能包含多次大模型调用与流式输出，所以这里要给足，不能按普通一元调用的经验值配。
     */
    private long deadlineSeconds = 300L;

    private List<RpcConfig>configs;

    @Data
    static class RpcConfig{

        private String name;

        private String host;

        private int port;

        /** 该服务单独的调用超时（秒）；为空则用全局 {@link #deadlineSeconds} */
        private Long deadlineSeconds;
    }
}
