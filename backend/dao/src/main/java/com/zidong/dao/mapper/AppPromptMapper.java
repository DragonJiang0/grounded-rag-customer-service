package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.entity.AppPrompt;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 提示词 */
public interface AppPromptMapper extends BaseMapper<AppPrompt> {

    /**
     * 按 app_id 保存提示词：已存在则覆盖 content。
     * <p>依赖 app_prompt.app_id 上的唯一键，SQL 见 {@code resources/mapper/AppPromptMapper.xml}。
     *
     * @param prompt 待保存的提示词，appId 必传
     * @return 受影响行数，插入返回 1，命中唯一键更新返回 2
     */
    int saveOrUpdateByAppId(@Param("prompt") AppPrompt prompt);

    /** 按应用取全部提示词，按主键升序 */
    default List<AppPrompt> selectByAppId(Long appId) {
        return selectList(Wrappers.<AppPrompt>lambdaQuery()
                .eq(AppPrompt::getAppId, appId)
                .orderByAsc(AppPrompt::getId));
    }

    /** 取应用最新一条提示词，查不到返回 null */
    default AppPrompt findLatestByAppId(Long appId) {
        return selectOne(Wrappers.<AppPrompt>lambdaQuery()
                .eq(AppPrompt::getAppId, appId)
                .orderByDesc(AppPrompt::getId), false);
    }

    /** 取应用最新一条提示词，查不到返回 null */
    default AppPrompt findByAppId(Long appId) {
        return selectOne(Wrappers.<AppPrompt>lambdaQuery()
                .eq(AppPrompt::getAppId, appId));
    }

    /** 清空某应用的全部提示词：删除提示词接口与应用删除时共用 */
    default int deleteByAppId(Long appId) {
        return delete(Wrappers.<AppPrompt>lambdaQuery()
                .eq(AppPrompt::getAppId, appId));
    }
}
