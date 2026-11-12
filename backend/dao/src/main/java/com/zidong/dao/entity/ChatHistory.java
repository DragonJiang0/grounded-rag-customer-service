package com.zidong.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

/** 聊天记录 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("chat_history")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属会话标识，存 taskId（与 chat_memory.memory_id 同源），多条记录归属同一会话 */
    private String conversationId;

    /** 所属应用 ID */
    private Long appId;

    /** 标题 */
    private String title;

    /** 聊天记录内容 */
    private String history;

    /**
     * 分段序号：同一 conversationId 下的记录先后，从 1 开始。
     *
     * <p>history 是 TEXT（上限约 64KB），写满后不再硬塞，而是新建一条 seq + 1 的分段行，
     * 查询时在内存里按 seq 升序把各分段合并回一条会话，页面仍按「一个会话一行」使用。
     */
    private Integer seq;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** 来源渠道：0.调试 1.API访问 2.免登陆窗口 3.账号密码登录窗口 4.微信个人号 5.飞书机器人 6.钉钉机器人 7.企业微信机器人 8.微信公众号 */
    private Integer channel;

    /**
     * 标签：来源接入实体 ID，按渠道区分。
     * <ul>
     *     <li>0.调试：null（无接入实体）</li>
     *     <li>1.API访问：app_api_access.id</li>
     *     <li>2.免登陆窗口：app_access_window.id</li>
     *     <li>3.账号密码登录窗口：app_password_window.id</li>
     *     <li>4.微信个人号：bot_credential.id</li>
     * </ul>
     */
    private Long tag;

    /**
     * 标签名称：只用于查询展示，不落库（无对应表字段）。
     *
     * <p>账号密码登录窗口取 {@code app_password_window.username}，
     * 微信个人号取 {@code bot_credential.scanner_name}，其它渠道为空；
     * 由查询侧（/app/setting/chatHistory）按 tag 反查后回填。
     */
    @TableField(exist = false)
    private String tagName;

    /** 用户侧是否已删除：false 未删除（默认），true 已删除（软删除，仅列表过滤，不物理删除） */
    private Boolean customerDeleted;
}
