package com.zidong.portal.bot;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 默认的占位处理器：真实业务流水线（如金价分析）尚未接入本模块时使用。
 *
 * <p>接入业务时请二选一：
 * <ol>
 *     <li>给自己的 {@link BotMessageHandler} 实现加 {@code @Primary}，本类会被覆盖为非首选；</li>
 *     <li>或直接删除本类（连同整个文件），再用同名类承载真实业务实现。</li>
 * </ol>
 *
 * <p>如果忘了去重，启动时会因为容器里出现两个 {@link BotMessageHandler} Bean 注入失败而报错，
 * 这正是期望的「接错了就立即知道」信号。</p>
 */
@Service
@RequiredArgsConstructor
public class PlaceholderBotMessageHandler implements BotMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(PlaceholderBotMessageHandler.class);

    private final BotApiHelper botApiHelper;

    @Override
    public void handle(BotApiHelper.BotMessage inbound) {
        log.warn("收到用户消息但尚未接入业务处理链路，仅做占位回复 [{}]: {}", inbound.fromUserId(), inbound.text());
        botApiHelper.replyTextByBot(inbound, "消息已收到，但当前未接入处理链路，请稍后再试。");
    }
}
