package com.zidong.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * portal 自己直连的 OpenAI 兼容模型配置：
 *
 * <pre>
 * portal:
 *   ai:
 *     chat:
 *       api-key: sk-xxx
 *       base-url: https://api.siliconflow.cn
 *       model: deepseek-ai/DeepSeek-V4-Flash
 * </pre>
 *
 * <p>与 yml 中 embabel 的 openai-custom 段各自独立定义：当前取值相同，
 * 但将来两者可能指向不同的 key / endpoint / 模型，故不做复用。</p>
 */
@ConfigurationProperties(prefix = "portal.ai.chat")
public class OpenAiCustomProperties {

    /** OpenAI 兼容服务的 api-key（当前为 SiliconFlow） */
    private String apiKey;

    /** 自定义 endpoint，不指向 OpenAI 官方地址 */
    private String baseUrl;

    /** 使用的模型，只用一个 */
    private String model;

    /**
     * 是否打印每次调用的请求体（输入 messages）与响应体（输出内容），对应 langchain4j 的
     * {@code logRequests / logResponses}。
     *
     * <p>关掉后连包装日志的 HttpClient 都不会加，日志级别再开也不会输出；
     * 开关只管「打不打」，具体级别（info 打请求/响应、debug 额外打流式逐块）仍由
     * {@code logging.level.dev.langchain4j.http.client.log} 控制。</p>
     */
    private boolean logEnabled = true;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isLogEnabled() {
        return logEnabled;
    }

    public void setLogEnabled(boolean logEnabled) {
        this.logEnabled = logEnabled;
    }
}
