package com.zidong.dao.vo;

import lombok.Getter;
import lombok.Setter;

/** 扫码绑定状态：前端按 status 决定继续轮询、提示过期还是跳转到已绑定 */
@Getter
@Setter
public class BotQrcodeStatusVO {

    /** WAITING 待扫码 / CONFIRMED 已确认 / EXPIRED 二维码过期 / TIMEOUT 等待超时 / UNKNOWN 未知令牌 */
    private String status;

    /** 当前是否已登录（凭证已写入 portal 共享存储） */
    private Boolean isLoggedIn;

    /** 已绑定的机器人 ID */
    private String ilinkBotId;

    /** 已绑定的微信用户 ID */
    private String ilinkUserId;
}
