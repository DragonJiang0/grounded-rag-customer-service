package com.zidong.portal.agent;

import io.github.apscot.laya.core.ModelStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 进程内加载 laya 决策模型。
 *
 * <p>模型常驻内存（约 2.5G 堆），且加载发生在容器启动阶段，会让启动变慢几十秒，
 * 部署时注意健康检查的起始等待时间。</p>
 *
 * <p><b>模型从哪来</b>：只看 {@code portal.ai.laya.model-source} 这一个相对路径
 * （相对进程工作目录，jar 启动时就是 {@code java -jar} 的当前目录）。
 * 目录在就直接加载；不在就从 {@code portal.ai.laya.fallback-model-source}（HF repo id）拉一份，
 * 缓存到 {@code $LAYA_CACHE}（默认 {@code ~/.cache/laya}），再软链回该路径，
 * 于是工作目录下始终有 {@code ./models/...}，下次启动直接命中本地。</p>
 *
 * <p>返回类型必须写全限定名：laya 的 Agent 与 embabel 的
 * {@code com.embabel.agent.core.Agent} 同名。</p>
 */
@Configuration
public class LayaConfig {

    private static final Logger log = LoggerFactory.getLogger(LayaConfig.class);

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "portal.ai.laya", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public io.github.apscot.laya.core.Agent layaDecisionAgent(LayaProperties properties) throws IOException {
        Path local = Path.of(properties.getModelSource()).toAbsolutePath().normalize();
        if (!Files.isDirectory(local)) {
            // laya 的下载缓存根固定是 $LAYA_CACHE（默认 ~/.cache/laya），改不了，软链过去即可
            Path cached = ModelStore.resolve(properties.getFallbackModelSource(), null, null);
            Files.createDirectories(local.getParent());
            Files.createSymbolicLink(local, cached);
            log.warn("[laya] 本地无模型，已下载到 {} 并软链到 {}", cached, local);
        }
        log.info("[laya] 使用模型 {}", local);
        return io.github.apscot.laya.core.Agent.load(local.toString());
    }
}
