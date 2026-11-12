package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.entity.AppAccessWindow;

import java.time.LocalDateTime;
import java.util.List;

/** 应用免密窗口 */
public interface AppAccessWindowMapper extends BaseMapper<AppAccessWindow> {

    /** 按应用取全部免密窗口 */
    default List<AppAccessWindow> selectByAppId(Long appId) {
        return selectList(Wrappers.<AppAccessWindow>lambdaQuery()
                .eq(AppAccessWindow::getAppId, appId)
                .orderByAsc(AppAccessWindow::getId));
    }

    /** 应用删除时清空其免密窗口 */
    default int deleteByAppId(Long appId) {
        return delete(Wrappers.<AppAccessWindow>lambdaQuery()
                .eq(AppAccessWindow::getAppId, appId));
    }

    /** 按唯一标识查询免密窗口，查不到返回 null */
    default AppAccessWindow findByWindowKey(String windowKey) {
        return selectOne(Wrappers.<AppAccessWindow>lambdaQuery()
                .eq(AppAccessWindow::getWindowKey, windowKey));
    }

    /** 编辑免密窗口：只更新名称 */
    default int editWindow(Long id, String name) {
        return update(null, Wrappers.<AppAccessWindow>lambdaUpdate()
                .set(AppAccessWindow::getName, name)
                .set(AppAccessWindow::getUpdatedAt, LocalDateTime.now())
                .eq(AppAccessWindow::getId, id));
    }

    /** 免密打开时刷新最后使用时间 */
    default int touchLastUsed(String windowKey) {
        return update(null, Wrappers.<AppAccessWindow>lambdaUpdate()
                .set(AppAccessWindow::getLastUsedAt, LocalDateTime.now())
                .eq(AppAccessWindow::getWindowKey, windowKey));
    }
}
