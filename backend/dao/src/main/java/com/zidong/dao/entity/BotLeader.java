package com.zidong.dao.entity;

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
 * Bot 监听主节点租约（固定单行，id 恒为 1）。
 *
 * <p>多实例部署时用它选主：同一时刻只有一个实例能持有未过期的租约，
 * 拿到租约的实例才启动微信长轮询与扫码登录，其余实例 standby 并周期性尝试接管。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("bot_leader")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BotLeader implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 固定单行主键 */
    @TableId(value = "id")
    private Long id;

    /** 当前 leader 实例 ID（空串表示无人持有） */
    private String instanceId;

    /** 租约到期时间，由 leader 心跳续租 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireTime;

    /** get_updates 游标，随 leader 交接，避免新 leader 从空游标重拉历史消息 */
    private String updatesBuf;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
