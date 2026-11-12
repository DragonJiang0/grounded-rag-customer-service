package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.dto.EditApiChannelDto;
import com.zidong.dao.entity.AppApiAccess;

import java.time.LocalDateTime;
import java.util.List;

/** 应用 API 访问接入 */
public interface AppApiAccessMapper extends BaseMapper<AppApiAccess> {

    /** 按应用取全部接入 */
    default List<AppApiAccess> selectByAppId(Long appId) {
        return selectList(Wrappers.<AppApiAccess>lambdaQuery()
                .eq(AppApiAccess::getAppId, appId)
                .orderByAsc(AppApiAccess::getId));
    }

    /** 应用删除时清空其 API 访问接入 */
    default int deleteByAppId(Long appId) {
        return delete(Wrappers.<AppApiAccess>lambdaQuery()
                .eq(AppApiAccess::getAppId, appId));
    }

    /** 编辑 API 渠道：只更新名称与过期时间 */
    default int editApi(EditApiChannelDto request) {
        return update(null, Wrappers.<AppApiAccess>lambdaUpdate()
                .set(AppApiAccess::getName, request.getName().trim())
                .set(AppApiAccess::getExpireAt, request.getExpireAt())
                .set(AppApiAccess::getUpdatedAt, LocalDateTime.now())
                .eq(AppApiAccess::getId, request.getId()));
    }

    /** 按 API Key 查询接入信息，查不到返回 null */
    default AppApiAccess findByApiKey(String apiKey) {
        return selectOne(Wrappers.<AppApiAccess>lambdaQuery()
                .eq(AppApiAccess::getApiKey, apiKey));
    }
}
