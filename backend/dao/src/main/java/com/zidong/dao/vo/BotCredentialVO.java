package com.zidong.dao.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zidong.dao.entity.BotCredential;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/** 微信个人号（Bot 登录凭证）列表项：只保留渲染列表需要的展示字段，不返回 token / baseurl */
@Getter
@Setter
public class BotCredentialVO {

    /** 登录状态：已登录可用 */
    public static final Integer STATUS_ONLINE = 1;

    /** 登录状态：未登录或凭证已失效 */
    public static final Integer STATUS_OFFLINE = 0;

    private Long id;

    /** 绑定的应用 ID */
    private Long appId;

    /** 机器人账号 ID */
    private String ilinkBotId;

    /** 扫码人 wxid */
    private String ilinkUserId;

    /** 扫码人（人工维护的展示名称，未维护时为 null） */
    private String scannerName;

    /** 登录状态：1 已登录可用，0 未登录或凭证已失效 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    /**
     * 构建列表项：token 为空或已被标记失效时视为未登录。
     *
     * @param credential 凭证行
     * @return 列表项
     */
    public static BotCredentialVO of(BotCredential credential) {
        BotCredentialVO vo = new BotCredentialVO();
        vo.setId(credential.getId());
        vo.setAppId(credential.getAppId());
        vo.setIlinkBotId(credential.getIlinkBotId());
        vo.setIlinkUserId(credential.getIlinkUserId());
        vo.setScannerName(credential.getScannerName());
        vo.setStatus(isOnline(credential) ? STATUS_ONLINE : STATUS_OFFLINE);
        vo.setUpdatedAt(credential.getUpdatedAt());
        return vo;
    }

    /** 有效登录：token 非空且未被标记失效 */
    private static boolean isOnline(BotCredential credential) {
        return StringUtils.hasText(credential.getBotToken())
                && !Boolean.TRUE.equals(credential.getInvalid());
    }
}
