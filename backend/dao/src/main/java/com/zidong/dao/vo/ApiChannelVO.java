package com.zidong.dao.vo;

import com.zidong.dao.entity.AppApiAccess;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** API 渠道列表项 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ApiChannelVO extends AppApiAccess {

    private static final long serialVersionUID = 1L;

    /** 构建列表项，API Key 脱敏 */
    public static ApiChannelVO of(AppApiAccess access) {
        ApiChannelVO vo = new ApiChannelVO();
        vo.setId(access.getId());
        vo.setAppId(access.getAppId());
        vo.setName(access.getName());
        vo.setApiKey(mask(access.getApiKey()));
        vo.setExpireAt(access.getExpireAt());
        vo.setLastUsedAt(access.getLastUsedAt());
        vo.setCreatedAt(access.getCreatedAt());
        vo.setUpdatedAt(access.getUpdatedAt());
        return vo;
    }

    /* ------------------------------------------------------------ 内部方法 */

    /** API Key 脱敏 */
    private static String mask(String apiKey) {
        if (apiKey == null || apiKey.length() <= 12) {
            return "****";
        }
        return apiKey.substring(0, 7) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
