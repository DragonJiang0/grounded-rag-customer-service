package com.zidong.portal.agent;

import com.embabel.agent.api.common.autonomy.AutonomyProperties;
import com.embabel.agent.api.common.ranking.Ranker;
import com.embabel.agent.api.common.ranking.Ranking;
import com.embabel.agent.api.common.ranking.Rankings;
import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 大模型排序选 Agent：把每个 Agent 的 {@code name: description} 交给 {@link Ranker}
 * （框架自带的 {@code LlmRanker}），由模型按用户意图打分，取超过置信度阈值的最高分。
 *
 * <p>这是框架原生的语义选择方式（{@code Autonomy.chooseAndRunAgent} 用的就是它），
 * 能区分"你好"和"坐地铁怎么到"；代价是一次 LLM 调用比较慢。</p>
 *
 * <p>与 {@link LayaAgentSelector} 互斥：仅在 {@code portal.ai.laya.enabled=false} 时注册，
 * 此时 laya 模型不加载、不占堆内存。条件必须写成「显式 false + 缺省不匹配」，
 * 与 laya 侧的 {@code matchIfMissing=true} 互补，否则两边都可能注册导致注入不唯一。</p>
 *
 * <p>阈值来自 {@code embabel.agent.platform.autonomy.agent-confidence-cut-off}（默认 0.6），
 * 排序用的模型可用 {@code embabel.agent.platform.ranking.llm} 指定。</p>
 */
@Component
@ConditionalOnProperty(prefix = "portal.ai.laya", name = "enabled",
        havingValue = "false", matchIfMissing = false)
public class LlmRankingAgentSelector implements AgentSelector {


    private static final Logger log = LoggerFactory.getLogger(LlmRankingAgentSelector.class);

    private final AgentPlatform agentPlatform;

    private final Ranker ranker;

    private final AutonomyProperties autonomyProperties;

    public LlmRankingAgentSelector(AgentPlatform agentPlatform,
                                   Ranker ranker,
                                   AutonomyProperties autonomyProperties) {
        this.agentPlatform = agentPlatform;
        this.ranker = ranker;
        this.autonomyProperties = autonomyProperties;
    }

    @Override
    public Optional<Agent> select(String message) {
        List<Agent> candidates = agentPlatform.agents();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        long startNanos = System.nanoTime();
        Rankings<Agent> rankings = ranker.rank("agent", message, candidates);
        long costMillis = (System.nanoTime() - startNanos) / 1_000_000L;
        double cutOff = autonomyProperties.getAgentConfidenceCutOff();
        log.info("[llm] 意图={}, 阈值={}, 排序={}", message, cutOff, rankings.infoString(false, 0));
        Optional<Agent> selected = rankings.rankings().stream()
                .filter(ranking -> ranking.getScore() > cutOff)
                .map(Ranking::getMatch)
                .findFirst();
        log.info("[llm] 选择 agent 耗时 {} ms，候选数={}，命中={}", costMillis, candidates.size(),
                selected.map(Agent::getName).orElse("无"));
        return selected;
    }
}
