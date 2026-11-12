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
 * 微信 Bot 登录凭证（每个扫码用户一行）。
 *
 * <p>一个应用可被多个用户扫码绑定：每个用户扫码确认后生成一行（按 {@code ilink_bot_id} 幂等，
 * 同一用户重复扫码更新已有行、token 轮换），各会话独立长轮询收消息，
 * 消息按各自行的 {@code app_id} 路由到对应应用回答。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("bot_credential")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BotCredential implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 自增主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** Bearer 鉴权令牌 */
    private String botToken;

    /** ilink 基座地址 */
    private String baseurl;

    /** 扫码确认返回的 ilink_bot_id（机器人账号 ID，唯一） */
    private String ilinkBotId;

    /** 扫码确认返回的 ilink_user_id（扫码人 wxid） */
    private String ilinkUserId;

    /** 扫码人（人工维护的展示名称，非微信返回值，可为空） */
    private String scannerName;

    /** 绑定的应用 ID：该机器人收到的消息按此应用触发 embabel 回答 */
    private Long appId;

    /** 凭证是否无效：1 表示该 token 已失效（getupdates 异常时标记），启动/恢复时跳过 */
    private Boolean invalid;

    /** get_updates 游标（本会话独立维护） */
    private String updatesBuf;

    /** 最后写入者实例 ID */
    private String updatedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
