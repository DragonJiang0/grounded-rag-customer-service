package com.zidong.dao.vo;

import com.zidong.dao.entity.AppAccessWindow;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** 免密窗口列表项 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AccessWindowVO extends AppAccessWindow {

    private static final long serialVersionUID = 1L;

    /** 构建列表项，免密窗口标识原文回显（便于管理端拼链接复制） */
    public static AccessWindowVO of(AppAccessWindow window) {
        AccessWindowVO vo = new AccessWindowVO();
        vo.setId(window.getId());
        vo.setAppId(window.getAppId());
        vo.setName(window.getName());
        vo.setWindowKey(window.getWindowKey());
        vo.setLastUsedAt(window.getLastUsedAt());
        vo.setCreatedAt(window.getCreatedAt());
        vo.setUpdatedAt(window.getUpdatedAt());
        return vo;
    }
}
