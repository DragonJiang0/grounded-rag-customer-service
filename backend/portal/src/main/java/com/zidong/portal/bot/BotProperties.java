package com.zidong.portal.bot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bot 相关配置：{@code bot.*}。
 *
 * <p>多实例部署时主要关心两段：
 * <ul>
 *     <li>{@code bot.leader}：主节点租约。心跳周期要远小于租约时长，
 *         否则 leader 来不及续租就会被其它实例抢走（建议 {@code leaseSeconds >= 3 * heartbeatMillis}）；</li>
 *     <li>{@code bot.dispatcher}：消息消费。{@code maxConcurrent} 控制全局并发度
 *         （默认 1，保持改造前「同一时刻只跑一个任务」的语义）；
 *         {@code leaderOnly} 为 true 时只有 leader 消费——现阶段回复用户必须走已登录的客户端，
 *         只有 leader 持有，后续若把消息发送能力做成共享服务再放开为 false。</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "bot")
public class BotProperties {

    /** ilink 基座地址，私有化部署时覆盖 */
    private String baseUrl = "https://ilinkai.weixin.qq.com";

    private Leader leader = new Leader();

    private Dispatcher dispatcher = new Dispatcher();

    private Login login = new Login();

    /** 登录配置：凭证缺失或 token 失效时，由主节点自动发起扫码 */
    @Data
    public static class Login {

        /**
         * 是否允许主节点自动发起扫码登录。
         * 关闭后需要外部显式调用 {@link BotLoginService#loginIfNecessary()} 或
         * {@link BotApiHelper#fetchQrcode()}，适合有独立管理端展示二维码的场景。
         */
        private boolean auto = true;

        /** 等待扫码确认的超时（秒），超时后重新获取二维码 */
        private long scanTimeoutSeconds = 300;

        /** 登录失败/超时后的最小重试间隔（毫秒），避免疯狂刷新二维码 */
        private long retryIntervalMillis = 60_000;
    }

    /** 选主（租约）配置 */
    @Data
    public static class Leader {

        /**
         * 是否开启选主。关闭后本实例直接按 leader 运行——单实例部署或运维上明确指定某台跑 Bot 时使用，
         * 此时请务必保证只有一个实例开启，否则与不开选主没有区别。
         */
        private boolean enabled = true;

        /** 租约时长（秒）：超过该时间未续租，其它实例可接管 */
        private long leaseSeconds = 30;

        /** leader 续租心跳间隔（毫秒） */
        private long heartbeatMillis = 5_000;

        /** 非 leader 尝试抢占的间隔（毫秒），同时决定了 leader 故障后的接管延迟上限 */
        private long acquireIntervalMillis = 5_000;
    }

    /** 消息消费配置 */
    @Data
    public static class Dispatcher {

        /** 本实例同时处理的消息数（全局并发度 = 实例数 × 该值，仅 leaderOnly=false 时才叠加） */
        private int maxConcurrent = 1;

        /** 没有待处理消息时的扫描间隔（毫秒） */
        private long pollIntervalMillis = 1_000;

        /** 单条消息最大重试次数，超过后不再抢占 */
        private int maxRetry = 3;

        /** 处理中超过该秒数未更新即视为卡死，重置回待处理 */
        private long stuckSeconds = 600;

        /** 是否只允许 leader 消费：true 表示只有 leader 处理消息（现阶段回复依赖 leader 的登录态） */
        private boolean leaderOnly = true;
    }
}
