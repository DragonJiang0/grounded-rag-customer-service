package com.zidong.portal.agent.agent;

/**
 * 本次请求所属应用的标识，作为黑板绑定在 agent 动作间传递。
 *
 * <p>用独立类型而不是裸 {@code Long}：agent 的动作参数是按类型在黑板里解析的，
 * 裸 Long 容易和别的绑定撞车（与 {@link ChatSession} 同理）。
 *
 * <p>它只承载「应用 id」这一最小契约，因此不再绑定具体接入实体：
 * {@code AppApiAccess}（/api 渠道）与 {@code AppAccessWindow}（免密窗口渠道）
 * 都只需映射成本类型，即可复用同一个 Agent。
 *
 * @param appId 应用 ID
 */
public record AppId(Long appId) {
}
