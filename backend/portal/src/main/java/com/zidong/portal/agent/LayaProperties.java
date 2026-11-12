package com.zidong.portal.agent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * laya 决策模型配置（进程内推理）。
 *
 * <p>模型在 {@link LayaConfig} 里启动时加载一次并常驻，因此 JVM 需要
 * {@code -Xmx3g} 与 {@code --add-modules=jdk.incubator.vector}。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "portal.ai.laya")
public class LayaProperties {

    /** 是否启用 laya 选 Agent；关闭时不加载模型（也就不占堆内存） */
    private boolean enabled = true;

    /**
     * 模型来源：相对进程工作目录的 checkpoint 路径（jar 启动时即 {@code java -jar} 的当前目录）。
     * 目录不存在或不完整时自动改用 {@link #fallbackModelSource} 下载，见 {@link LayaConfig}。
     */
    private String modelSource = "./models/laya-multilingual-mlx";

    /** 本地模型缺失时的下载来源：HF repo id，缓存根为 {@code $LAYA_CACHE}（默认 {@code ~/.cache/laya}） */
    private String fallbackModelSource = "aac6fef/laya-multilingual-mlx";

    /** 兜底选项的标签：没命中任何业务 Agent 时落到它，等价于「不选」 */
    private String fallbackLabel = "None";

    /** 兜底选项的描述，给模型看，说明什么情况算「都不合适」 */
    private String fallbackDescription = "以上都不合适：打招呼、寒暄、闲聊、常识问题，或与上述助手职责无关的问题";

    /** 选中所需的最低概率（choice 的 top1） */
    private double minScore = 0.5;

    /** top1 与 top2 的最小差距，避免相近时误选；候选只有 1 个时忽略 */
    private double margin = 0.15;

    /** 决策时带上的历史轮数（1 轮 = 用户与助手各一条） */
    private int historyTurns = 3;

    /** 候选 Agent 名称白名单，留空表示平台内全部 Agent */
    private List<String> candidates = new ArrayList<>();

    /** 候选 Agent 名称前缀黑名单，默认排除调试用 Agent */
    private List<String> excludePrefixes = new ArrayList<>(List.of("Debug"));
}
