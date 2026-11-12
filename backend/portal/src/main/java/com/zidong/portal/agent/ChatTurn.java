package com.zidong.portal.agent;

/**
 * 供决策模型使用的历史对话轮次。
 *
 * <p>{@code role} 取 {@code user} / {@code assistant}，字段名与 laya 消息数组一致，
 * 组 state 时可直接转成 Map。</p>
 */
public record ChatTurn(String role, String content) {

    public static final String ROLE_USER = "user";

    public static final String ROLE_ASSISTANT = "assistant";

    public static ChatTurn user(String content) {
        return new ChatTurn(ROLE_USER, content);
    }

    public static ChatTurn assistant(String content) {
        return new ChatTurn(ROLE_ASSISTANT, content);
    }
}
