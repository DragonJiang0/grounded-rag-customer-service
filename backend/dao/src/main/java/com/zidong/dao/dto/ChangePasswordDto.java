package com.zidong.dao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 修改密码请求 */
@Getter
@Setter
public class ChangePasswordDto {

    @NotBlank(message = "请输入原密码")
    @Size(max = 128, message = "原密码过长")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 128, message = "新密码长度需为 6-128 个字符")
    private String newPassword;
}
