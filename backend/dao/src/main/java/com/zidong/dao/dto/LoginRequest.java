package com.zidong.dao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 登录请求 */
@Getter
@Setter
public class LoginRequest {

    /**
     * 调用凭证 AppKey：账号密码登录窗口按应用隔离账号，登录时必填；
     * 管理端账号登录不使用该字段，故此处不做非空校验，由使用方自行校验。
     */
    @Size(max = 64, message = "AppKey 过长")
    private String appKey;

    @NotBlank(message = "请输入用户名")
    @Size(max = 64, message = "用户名过长")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(max = 128, message = "密码过长")
    private String password;
}
