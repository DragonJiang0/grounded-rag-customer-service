package com.zidong.dao.vo;

import lombok.Getter;
import lombok.Setter;

/** 机器人绑定二维码：前端拿 qrcodeImgContent 渲染成图片，用 qrcode 轮询绑定状态 */
@Getter
@Setter
public class BotQrcodeVO {

    /** 轮询扫码状态用的令牌 */
    private String qrcode;

    /** 二维码内容（链接/文本），前端渲染成二维码图片 */
    private String qrcodeImgContent;

    /** 二维码过期时间（毫秒时间戳），前端用于倒计时 */
    private Long expireAtMillis;
}
