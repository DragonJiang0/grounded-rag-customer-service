package com.zidong.portal.agent.service;

import com.zidong.dao.entity.AppChatOptions;

/**
 * 一次对话请求的可调参数，字段与 langchain4j 的 {@code OpenAiChatRequestParameters} 一一对应，
 * 用于把「温度、topP、最大输出」等对外暴露给调用方。
 *
 * <p>约定：字段为 {@code null} 时不做覆盖，交给模型 bean（{@code portal.ai.chat}）的默认值，
 * 因此只需按需填要改的项。{@link #defaults()} 给出了项目默认的一套参数，可直接使用。</p>
 *
 * <p>{@code memoryRounds} 不参与模型请求参数，只决定本次调用读写的会话记忆保留多少轮，
 * 由 {@code LLMService} 在构建记忆窗口时读取，因此同一个会话在不同调用里可以有不同的记忆长度。</p>
 *
 * @param temperature     采样温度
 * @param topP            nucleus 采样
 * @param maxOutputTokens 最大输出 token 数
 */
public record ChatOptions(
        String taskId,
        boolean memoryEnabled,
        int round,
        Double temperature,
        Double topP,
        Integer maxOutputTokens) {

    /** 会话记忆默认保留的问答轮数：一轮 = 用户消息 + 模型回复 */
    public static final int DEFAULT_MEMORY_ROUNDS = 1;

    /** 项目默认参数。 */
    public static ChatOptions defaults() {
        return new ChatOptions(null, true, 1, null, null, 10240);
    }

    /** 由应用的对话参数配置构建：字段为空即不覆盖；传入 null 时返回 {@link #defaults()}。 */
    public static ChatOptions of(String taskId, AppChatOptions options) {

        return new ChatOptions(
                taskId,options.getMemoryEnabled(),options.getMemoryRounds(),
                options.getTemperature() == null ? null : options.getTemperature().doubleValue(),
                options.getTopP() == null ? null : options.getTopP().doubleValue(),
                options.getMaxOutputTokens());
    }


}
