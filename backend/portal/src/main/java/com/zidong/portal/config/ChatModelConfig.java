package com.zidong.portal.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义 OpenAI 兼容模型（langchain4j）。
 *
 * <p>api-key / base-url / 模型名来自 {@link OpenAiCustomProperties}（即 application.yml
 * 中的 {@code portal.ai.chat} 段），与 embabel 的模型配置各自独立。</p>
 *
 * <p>因为指向的是 OpenAI 兼容服务（当前为 SiliconFlow），必须显式设置 {@code baseUrl}，
 * 否则 langchain4j 会走 OpenAI 官方地址而 401。</p>
 */
@Configuration
@EnableConfigurationProperties(OpenAiCustomProperties.class)
public class ChatModelConfig {

    private final OpenAiCustomProperties properties;

    public ChatModelConfig(OpenAiCustomProperties properties) {
        this.properties = properties;
    }

    @Bean
    public OpenAiChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .modelName(properties.getModel())
                // 打印每次调用的输入（请求体 messages）与输出（响应体内容），走 INFO 级别；
                // 是否开启由 portal.ai.chat.log-enabled 决定
                .logRequests(properties.isLogEnabled())
                .logResponses(properties.isLogEnabled())
                .build();
    }

    /**
     * 流式版本：{@link OpenAiChatModel} 只实现非流式的 {@code ChatModel}，
     * 逐块输出必须用实现了 {@code StreamingChatModel} 的 {@link OpenAiStreamingChatModel}。
     *
     * <p>与 {@link #openAiChatModel()} 复用同一份 {@code portal.ai.chat} 配置，仅用于流式场景。</p>
     */
    @Bean
    public OpenAiStreamingChatModel openAiStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .modelName(properties.getModel())
                // 请求体（含输入）走 INFO；流式的逐块 SSE 事件在 DEBUG 级别；
                // 是否开启由 portal.ai.chat.log-enabled 决定
                .logRequests(properties.isLogEnabled())
                .logResponses(properties.isLogEnabled())
                .build();
    }
}
