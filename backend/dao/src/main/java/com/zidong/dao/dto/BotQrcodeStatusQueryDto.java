package com.zidong.dao.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 扫码状态查询入参 */
@Getter
@Setter
public class BotQrcodeStatusQueryDto {

    /** 创建二维码时返回的轮询令牌 */
    @NotBlank(message = "qrcode 不能为空")
    private String qrcode;
}
