package com.zidong.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 应用账号密码窗口（凭账号口令登录直达对话的链接凭证） */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("app_password_window")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppPasswordWindow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态：启用 */
    public static final int STATUS_ENABLED = 1;
    /** 状态：禁用 */
    public static final int STATUS_DISABLED = 0;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属应用 ID */
    private Long appId;

    /** 窗口名称，同一应用内唯一，供管理端识别 */
    private String name;

    /** 登录账号，同一应用内唯一 */
    private String username;

    /** 登录密码，BCrypt 密文，不对外序列化 */
    @JsonIgnore
    private String password;

    /** 状态：1 启用 0 禁用，禁用后不允许登录 */
    private Integer status;

    /** 登录令牌，登录成功后签发；退出或过期后清空，不对外序列化 */
    @JsonIgnore
    private String token;

    /** 登录令牌过期时间，null 表示当前无有效登录态 */
    @JsonIgnore
    private LocalDateTime tokenExpireAt;

    /** 最后一次登录时间，未使用过为 null */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastUsedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
