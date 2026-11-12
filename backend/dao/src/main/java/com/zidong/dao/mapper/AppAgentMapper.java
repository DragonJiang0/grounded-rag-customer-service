package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.entity.AppAgent;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** 应用-Agent关联 */
public interface AppAgentMapper extends BaseMapper<AppAgent> {

    /** 批量取应用绑定的 Agent 关联 */
    default List<AppAgent> selectByAppIds(Collection<Long> appIds) {
        if (CollectionUtils.isEmpty(appIds)) {
            return List.of();
        }
        return selectList(Wrappers.<AppAgent>lambdaQuery()
                .in(AppAgent::getAppId, appIds)
                .orderByAsc(AppAgent::getAppId)
                .orderByAsc(AppAgent::getId));
    }

    /** 取单个应用绑定的 Agent 关联 */
    default List<AppAgent> selectByAppId(Long appId) {
        if (appId == null) {
            return List.of();
        }
        return selectByAppIds(List.of(appId));
    }

    /** 取应用绑定的 Agent 名称（按绑定顺序） */
    default List<String> listAgentNamesByAppId(Long appId) {
        return selectByAppId(appId).stream()
                .map(AppAgent::getAgentName)
                .collect(Collectors.toList());
    }

    /** 应用删除时清空其 Agent 绑定 */
    default int deleteByAppId(Long appId) {
        return delete(Wrappers.<AppAgent>lambdaQuery()
                .eq(AppAgent::getAppId, appId));
    }
}
