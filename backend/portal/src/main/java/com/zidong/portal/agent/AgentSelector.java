package com.zidong.portal.agent;

import com.embabel.agent.core.Agent;

import java.util.List;
import java.util.Optional;

/**
 * 多 Agent 选择策略：根据用户消息挑一个要执行的 Agent。
 *
 * <p>两个实现<b>互斥注册</b>，同一时刻容器里只有一个，切换只改配置、不改代码：</p>
 * <ul>
 *   <li>{@link LayaAgentSelector}：进程内决策模型，{@code portal.ai.laya.enabled=true}（默认）；</li>
 *   <li>{@link LlmRankingAgentSelector}：大模型排序，{@code portal.ai.laya.enabled=false}。</li>
 * </ul>
 *
 * <p>调用方只依赖本接口，不关心当前用的是哪一种；返回空表示「没选出可执行 Agent」，由调用方回落默认 Agent。</p>
 */
public interface AgentSelector {

    /**
     * @param message 当前用户输入
     * @return 命中的 Agent；未达门槛、候选为空或落到兜底选项时为空
     */
    Optional<Agent> select(String message);

    /**
     * 带历史的版本：能让决策看到上下文（如「那它到哪了」承接上一轮）。
     *
     * <p>不支持历史的实现忽略 {@code history}，只按当前这句判断，行为与单参版本一致。</p>
     *
     * @param message 当前用户输入
     * @param history 最近若干轮历史（按时间正序）
     */
    default Optional<Agent> select(String message, List<ChatTurn> history) {
        return select(message);
    }
}
