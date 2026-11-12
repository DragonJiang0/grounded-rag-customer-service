package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.dto.SaveAppChatMemoryDto;
import com.zidong.dao.dto.SaveAppChatOptionsDto;
import com.zidong.dao.entity.AppChatOptions;

import java.time.LocalDateTime;

/** 应用模型对话参数 */
public interface AppChatOptionsMapper extends BaseMapper<AppChatOptions> {

    /** 取应用的对话参数配置，查不到返回 null */
    default AppChatOptions findByAppId(Long appId) {
        return selectOne(Wrappers.<AppChatOptions>lambdaQuery()
                .eq(AppChatOptions::getAppId, appId));
    }

    /** 按应用更新对话参数配置，返回受影响行数 */
    default int saveByAppId(SaveAppChatOptionsDto request) {
        return update(null, Wrappers.<AppChatOptions>lambdaUpdate()
                .eq(AppChatOptions::getAppId, request.getAppId())
                .set(AppChatOptions::getTemperature, request.getTemperature())
                .set(AppChatOptions::getTopP, request.getTopP())
                .set(AppChatOptions::getMaxOutputTokens, request.getMaxOutputTokens())
                .set(AppChatOptions::getUpdatedAt, LocalDateTime.now()));
    }

    /** 按应用只更新对话记忆配置（开关 + 轮数），不影响模型参数，返回受影响行数 */
    default int saveMemoryByAppId(SaveAppChatMemoryDto request) {
        return update(null, Wrappers.<AppChatOptions>lambdaUpdate()
                .eq(AppChatOptions::getAppId, request.getAppId())
                .set(AppChatOptions::getMemoryEnabled, request.getMemoryEnabled())
                .set(AppChatOptions::getMemoryRounds, request.getMemoryRounds())
                .set(AppChatOptions::getUpdatedAt, LocalDateTime.now()));
    }

    /** 应用删除时清空其对话参数配置 */
    default int deleteByAppId(Long appId) {
        return delete(Wrappers.<AppChatOptions>lambdaQuery()
                .eq(AppChatOptions::getAppId, appId));
    }
}
