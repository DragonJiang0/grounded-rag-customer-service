package com.zidong.portal.agent.agent;

/**
 * 本次请求的会话上下文，目前只承载 taskId。
 *
 * <p>用独立类型而不是裸 {@code String}：agent 的动作参数是按类型在黑板里解析的，
 * 裸 String 容易和别的绑定撞车，也会触发「同类型参数不唯一」的校验（与 {@link InlinePrompt} 同理）。</p>
 *
 * @param taskId 会话/任务 ID，作为 langchain4j 的记忆 id 使用；为空表示本次调用不使用记忆
 */
public record ChatSession(String taskId) {
}
