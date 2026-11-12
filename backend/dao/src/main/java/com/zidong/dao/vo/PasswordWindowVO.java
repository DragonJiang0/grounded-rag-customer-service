package com.zidong.dao.vo;

import com.zidong.dao.entity.AppPasswordWindow;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** 账号密码窗口列表项 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PasswordWindowVO extends AppPasswordWindow {

    private static final long serialVersionUID = 1L;

    /** 构建列表项，不返回密码 */
    public static PasswordWindowVO of(AppPasswordWindow window) {
        PasswordWindowVO vo = new PasswordWindowVO();
        vo.setId(window.getId());
        vo.setAppId(window.getAppId());
        vo.setName(window.getName());
        vo.setUsername(window.getUsername());
        vo.setStatus(window.getStatus());
        vo.setLastUsedAt(window.getLastUsedAt());
        vo.setCreatedAt(window.getCreatedAt());
        vo.setUpdatedAt(window.getUpdatedAt());
        return vo;
    }
}
