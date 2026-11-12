package com.zidong.portal.agent.agent;

/**
 * Agent 的最终返回结果。
 *
 * <p>所有 Agent 的 {@code @AchievesGoal} 动作统一返回本类型，取结果时按类型取
 * （{@code process.resultOfType(AgentResult.class)}）即可拿到，
 * 不再依赖黑板里「最后放入的对象」，避免取到入参或中间结果。
 *
 * @param output 最终答复正文
 */
public record AgentResult(String output) {
}
