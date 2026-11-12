package com.zidong.portal.agent.agent;

/**
 * 调用方逐次传入的提示词及其应用上下文（外部提示词入口）。
 *
 * <p>用独立类型而不是 {@code String}：agent 的动作参数是按类型在黑板里解析的，
 * 裸 {@code String} 容易和别的绑定撞车，也容易触发「同类型参数不唯一」的校验。
 *
 * @param template 提示词模板，含 {@code {question}} 与 {@code {answer}} 占位符
 * @param appId    应用 ID，用于查该应用下的语料；暂未接入时可为 null
 */
public record InlinePrompt(String template, Long appId) {
}
