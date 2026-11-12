package com.zidong.dao.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 创建机器人绑定二维码入参 */
@Getter
@Setter
public class CreateBotQrcodeQueryDto {

    /** 要绑定的应用 ID：扫码后该机器人消息按此应用触发 embabel 回答 */
    @NotNull(message = "appId 不能为空")
    private Long appId;
}
