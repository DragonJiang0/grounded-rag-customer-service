package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.entity.AppPasswordWindow;

import java.time.LocalDateTime;
import java.util.List;

/** 应用账号密码窗口 */
public interface AppPasswordWindowMapper extends BaseMapper<AppPasswordWindow> {

    /** 按应用取全部账号密码窗口 */
    default List<AppPasswordWindow> selectByAppId(Long appId) {
        return selectList(Wrappers.<AppPasswordWindow>lambdaQuery()
                .eq(AppPasswordWindow::getAppId, appId)
                .orderByAsc(AppPasswordWindow::getId));
    }

    /** 按应用取全部账号密码窗口，创建时间倒序（最新创建的在前） */
    default List<AppPasswordWindow> selectByAppIdOrderByCreatedDesc(Long appId) {
        return selectList(Wrappers.<AppPasswordWindow>lambdaQuery()
                .eq(AppPasswordWindow::getAppId, appId)
                .orderByDesc(AppPasswordWindow::getCreatedAt)
                .orderByDesc(AppPasswordWindow::getId));
    }

    /** 应用删除时清空其账号密码窗口 */
    default int deleteByAppId(Long appId) {
        return delete(Wrappers.<AppPasswordWindow>lambdaQuery()
                .eq(AppPasswordWindow::getAppId, appId));
    }

    /** 按应用与登录账号查询，查不到返回 null */
    default AppPasswordWindow findByAppIdAndUsername(Long appId, String username) {
        return selectOne(Wrappers.<AppPasswordWindow>lambdaQuery()
                .eq(AppPasswordWindow::getAppId, appId)
                .eq(AppPasswordWindow::getUsername, username));
    }

    /** 按登录令牌查询，查不到返回 null */
    default AppPasswordWindow findByToken(String token) {
        return selectOne(Wrappers.<AppPasswordWindow>lambdaQuery()
                .eq(AppPasswordWindow::getToken, token));
    }

    /** 编辑窗口：更新名称与状态 */
    default int editWindow(Long id, String name, Integer status) {
        return update(null, Wrappers.<AppPasswordWindow>lambdaUpdate()
                .set(AppPasswordWindow::getName, name)
                .set(AppPasswordWindow::getStatus, status)
                .set(AppPasswordWindow::getUpdatedAt, LocalDateTime.now())
                .eq(AppPasswordWindow::getId, id));
    }

    /** 重置登录密码 */
    default int updatePassword(Long id, String encodedPassword) {
        return update(null, Wrappers.<AppPasswordWindow>lambdaUpdate()
                .set(AppPasswordWindow::getPassword, encodedPassword)
                .set(AppPasswordWindow::getUpdatedAt, LocalDateTime.now())
                .eq(AppPasswordWindow::getId, id));
    }

    /** 修改启用/禁用状态 */
    default int updateStatus(Long id, Integer status) {
        return update(null, Wrappers.<AppPasswordWindow>lambdaUpdate()
                .set(AppPasswordWindow::getStatus, status)
                .set(AppPasswordWindow::getUpdatedAt, LocalDateTime.now())
                .eq(AppPasswordWindow::getId, id));
    }

    /** 登录成功时刷新最后使用时间 */
    default int touchLastUsed(Long id) {
        return update(null, Wrappers.<AppPasswordWindow>lambdaUpdate()
                .set(AppPasswordWindow::getLastUsedAt, LocalDateTime.now())
                .eq(AppPasswordWindow::getId, id));
    }

    /** 登录成功签发登录令牌：写入 token 与过期时间 */
    default int updateLoginToken(Long id, String token, LocalDateTime tokenExpireAt) {
        return update(null, Wrappers.<AppPasswordWindow>lambdaUpdate()
                .set(AppPasswordWindow::getToken, token)
                .set(AppPasswordWindow::getTokenExpireAt, tokenExpireAt)
                .set(AppPasswordWindow::getUpdatedAt, LocalDateTime.now())
                .eq(AppPasswordWindow::getId, id));
    }

    /** 退出登录：按令牌清空登录态（token 与过期时间置空） */
    default int clearLoginToken(String token) {
        return update(null, Wrappers.<AppPasswordWindow>lambdaUpdate()
                .set(AppPasswordWindow::getToken, null)
                .set(AppPasswordWindow::getTokenExpireAt, null)
                .set(AppPasswordWindow::getUpdatedAt, LocalDateTime.now())
                .eq(AppPasswordWindow::getToken, token));
    }
}
