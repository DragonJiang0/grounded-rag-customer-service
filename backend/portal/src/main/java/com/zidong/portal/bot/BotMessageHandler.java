package com.zidong.portal.bot;

/**
 * 消息业务处理入口：只负责「处理一条已入库的消息」，回复通过 {@link BotApiHelper} 自行推送。
 *
 * <p>把业务从监听线程里剥出去，是为了让「收消息（必须单例）」和「处理消息（可并发、可重试）」
 * 解耦：监听只管幂等入库，处理由 {@link BotMessageDispatcher} 按状态机调度，
 * 实例崩溃后消息仍在库里，不会被静默丢弃。</p>
 *
 * <p>接入真实业务时实现本接口并注册为 Spring Bean 即可替换默认的占位实现
 * （{@code PlaceholderBotMessageHandler}），无需改动监听与调度代码。</p>
 */
public interface BotMessageHandler {

    /**
     * 处理一条入站消息。
     *
     * @param inbound 入站消息（含 context_token，回复时须回带）
     * @throws Exception 抛出即视为失败，由调度器记录失败原因并计入重试
     */
    void handle(BotApiHelper.BotMessage inbound) throws Exception;
}
