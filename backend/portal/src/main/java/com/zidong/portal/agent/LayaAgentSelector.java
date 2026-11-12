package com.zidong.portal.agent;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import io.github.apscot.laya.core.Question;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用 laya 决策模型选 Agent：一次 {@code predict}、一道 {@code choice} 题，毫秒级、结果确定。
 *
 * <p>与 {@link LlmRankingAgentSelector} 的本质区别在「问题怎么组合」：
 * LLM 排序是让模型给每个 Agent 各打一个分再比大小；laya 直接用一道 {@code choice} 题，
 * 返回的就是「在候选里挑一个」的概率分布，天然归一、天然可比。</p>
 *
 * <p><b>候选集必须包含一个兜底选项</b>（label 默认 {@code None}）：choice 是「必须选一个」，
 * 若候选里只有业务 Agent，闲聊话术的概率会被强行摊给业务 Agent（实测「你好呀」在
 * 只有两个业务候选时仍会给出 0.7 以上的 top1）。加了兜底选项后，闲聊/无关问题会
 * 以 0.96 左右的概率落到兜底，业务问题则稳定落到对应 Agent。</p>
 *
 * <p>实测（multilingual checkpoint、3 结论）：订单号问题 1.0 命中 OrderQueryAgent、
 * 物流问题 0.59 命中 LogisticsTrackAgent、闲聊 0.96 落兜底。阈值仍需按自有话术标定。</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "portal.ai.laya", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class LayaAgentSelector implements AgentSelector {

    private static final Logger log = LoggerFactory.getLogger(LayaAgentSelector.class);

    /** 题号：与 predict 返回的 answers key 对应 */
    private static final String Q_AGENT = "agent";

    /** embabel 平台，用于取候选 Agent */
    private final AgentPlatform agentPlatform;

    /** laya 推理主体，启动时加载一次后复用 */
    private final io.github.apscot.laya.core.Agent layaAgent;

    private final LayaProperties properties;

    /** 无历史版本：等价于只按当前这句话判断 */
    @Override
    public Optional<Agent> select(String message) {
        return select(message, List.of());
    }

    /**
     * @param message 当前用户输入
     * @param history 最近若干轮历史（按时间正序），与 message 一起组成 laya 的 state
     * @return 命中的 Agent；未达门槛、候选为空或落到兜底选项时为空（由调用方回落默认 Agent）
     */
    @Override
    public Optional<Agent> select(String message, List<ChatTurn> history) {
        long startNanos = System.nanoTime();
        Optional<Agent> selected = doSelect(message, history);
        long costMillis = (System.nanoTime() - startNanos) / 1_000_000L;
        log.debug("[laya] 决策总耗时 {} ms，选中={}", costMillis, selected.map(Agent::getName).orElse("无"));
        return selected;
    }

    /** 实际决策过程；info 里打印的是模型推理耗时，外层 debug 打印的是含候选构造、判定的总耗时 */
    private Optional<Agent> doSelect(String message, List<ChatTurn> history) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }
        List<Agent> candidates = candidates();
        if (candidates.isEmpty()) {
            log.info("[laya] 无可用候选 Agent，跳过选择");
            return Optional.empty();
        }

        // 选项：先放兜底，再放业务 Agent。标签用 Agent 名称，需唯一
        String fallbackLabel = properties.getFallbackLabel();
        Map<String, Agent> byLabel = new LinkedHashMap<>();
        Map<String, String> criteria = new LinkedHashMap<>();
        criteria.put(fallbackLabel, properties.getFallbackDescription());
        for (Agent candidate : candidates) {
            String label = uniqueLabel(criteria, candidate);
            byLabel.put(label, candidate);
            criteria.put(label, candidate.getDescription());
        }

        Map<String, Question> questions = new LinkedHashMap<>();
        questions.put(Q_AGENT, Question.choice(
                "这条用户消息应该交给哪个助手处理？", criteria));

        Object state = history.isEmpty() ? message : buildState(history, message);

        // 入参回放：state 是实际喂给模型的文本/消息数组，选项是 label=描述
        log.debug("[laya] 决策入参: 历史消息数={}, state={}, 选项={}",
                history.size(), state, formatCriteria(criteria));

        long startNanos = System.nanoTime();
        Map<?, ?> response = layaAgent.predict(state, questions);
        long costMillis = (System.nanoTime() - startNanos) / 1_000_000L;

        List<Map.Entry<String, Double>> ranked =
                rank(asMap(asMap(response.get("answers")).get(Q_AGENT)).get("probabilities"));
        if (ranked.isEmpty()) {
            log.info("[laya] 模型未返回概率分布，不选 Agent");
            return Optional.empty();
        }

        double top1 = ranked.get(0).getValue();
        double top2 = ranked.size() > 1 ? ranked.get(1).getValue() : 0.0;
        String topLabel = ranked.get(0).getKey();

        log.info("[laya] 意图={}, 候选数={}, 分布={}, 推理耗时={}ms",
                message, candidates.size(), formatDistribution(ranked), costMillis);

        if (fallbackLabel.equals(topLabel)) {
            log.info("[laya] 命中兜底选项，不选 Agent");
            return Optional.empty();
        }
        if (top1 < properties.getMinScore()) {
            log.info("[laya] top1={} 低于最低分 {}，不选 Agent", format(top1), properties.getMinScore());
            return Optional.empty();
        }
        if (top1 - top2 < properties.getMargin()) {
            log.info("[laya] top1={} 与 top2={} 差距小于 {}，不选 Agent",
                    format(top1), format(top2), properties.getMargin());
            return Optional.empty();
        }

        Agent selected = byLabel.get(topLabel);
        log.info("[laya] 选中 Agent: {}", selected.getName());
        return Optional.of(selected);
    }

    /** 候选集：白名单非空时只取白名单，否则取平台内全部；再按前缀黑名单与空描述过滤 */
    private List<Agent> candidates() {
        List<String> whitelist = properties.getCandidates();
        List<String> blacklist = properties.getExcludePrefixes();
        return agentPlatform.agents().stream()
                .filter(agent -> agent.getName() != null && !agent.getName().isBlank())
                .filter(agent -> whitelist.isEmpty() || whitelist.contains(agent.getName()))
                .filter(agent -> blacklist.stream().noneMatch(prefix -> agent.getName().startsWith(prefix)))
                .filter(agent -> agent.getDescription() != null && !agent.getDescription().isBlank())
                .toList();
    }

    /** 标签唯一化：正常情况 Agent 名称唯一，只需避开兜底选项与已用标签 */
    private String uniqueLabel(Map<String, String> used, Agent agent) {
        String base = agent.getName();
        String label = base;
        int suffix = 2;
        while (used.containsKey(label)) {
            label = base + "#" + suffix++;
        }
        return label;
    }

    /**
     * 多轮 state：laya 认消息数组（role/content），传 List 时从最旧端截断，
     * 因此这里只保留最近 historyTurns 轮，避免无意义的截断。
     */
    private Object buildState(List<ChatTurn> history, String message) {
        int maxMessages = Math.max(1, properties.getHistoryTurns()) * 2;
        int from = Math.max(0, history.size() - maxMessages);
        List<Map<String, String>> messages = new ArrayList<>(history.size() - from + 1);
        for (ChatTurn turn : history.subList(from, history.size())) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }
        messages.add(Map.of("role", ChatTurn.ROLE_USER, "content", message));
        return messages;
    }

    /** probabilities: {label: 0~1} -> 按概率降序的列表 */
    private List<Map.Entry<String, Double>> rank(Object probabilities) {
        return asMap(probabilities).entrySet().stream()
                .map(entry -> Map.entry(String.valueOf(entry.getKey()), number(entry.getValue())))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .toList();
    }

    /** 选项回放：{label=描述, ...}，顺序与提交给模型的一致 */
    private String formatCriteria(Map<String, String> criteria) {
        return criteria.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private String formatDistribution(List<Map.Entry<String, Double>> ranked) {
        return ranked.stream()
                .map(entry -> entry.getKey() + "=" + format(entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static String format(double value) {
        return String.format("%.4f", value);
    }

    private static double number(Object value) {
        return value instanceof Number num ? num.doubleValue() : 0.0;
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<Object, Object>) map : Map.of();
    }
}
