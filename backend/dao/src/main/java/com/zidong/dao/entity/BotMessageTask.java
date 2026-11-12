package com.zidong.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Bot 入站消息与处理任务（表 {@code bot_message}）。
 *
 * <p>一条微信消息一行：入库时靠 {@code dedupe_key} 唯一键做幂等，
 * 之后按 {@code status} 状态机在实例间抢占处理，取代进程内的 {@code AtomicBoolean} 单飞。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("bot_message")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BotMessageTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待处理：等待实例抢占 */
    public static final int STATUS_PENDING = 0;

    /** 处理中：已被某实例抢占 */
    public static final int STATUS_RUNNING = 1;

    /** 已完成 */
    public static final int STATUS_DONE = 2;

    /** 失败待重试：retry_count 未达上限时会被重新抢占 */
    public static final int STATUS_FAILED = 3;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 幂等键：优先微信 message_id，缺失时用 用户+上下文+内容 的哈希 */
    private String dedupeKey;

    /** 微信侧消息 ID */
    private Long messageId;

    /** 发送方用户 ID */
    private String fromUserId;

    /** 接收方（机器人）用户 ID */
    private String toUserId;

    /** 消息类型：1=用户消息 */
    private Integer messageType;

    /** 消息状态，透传 SDK 字段 */
    private Integer messageState;

    /** 回复时必须回带的上下文令牌 */
    private String contextToken;

    /** 消息文本内容 */
    private String content;

    /** 处理状态，见 {@link #STATUS_PENDING} 等常量 */
    private Integer status;

    /** 当前处理实例 ID */
    private String owner;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最近一次失败原因 */
    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
