package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.entity.AppCorpusGroup;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

/** 应用-语料分组关联 */
public interface AppCorpusGroupMapper extends BaseMapper<AppCorpusGroup> {

    /** 批量取应用绑定的分组关联 */
    default List<AppCorpusGroup> selectByAppIds(Collection<Long> appIds) {
        if (CollectionUtils.isEmpty(appIds)) {
            return List.of();
        }
        return selectList(Wrappers.<AppCorpusGroup>lambdaQuery()
                .in(AppCorpusGroup::getAppId, appIds)
                .orderByAsc(AppCorpusGroup::getAppId)
                .orderByAsc(AppCorpusGroup::getId));
    }

    /** 应用删除时清空其分组绑定 */
    default int deleteByAppId(Long appId) {
        return delete(Wrappers.<AppCorpusGroup>lambdaQuery()
                .eq(AppCorpusGroup::getAppId, appId));
    }

    /**
     * 按应用 ID 查询该应用下全部语料问题（已去重）。
     * <p>SQL 见 {@code resources/mapper/AppCorpusGroupMapper.xml}。
     *
     * @param appId 应用 ID
     * @return 去重后的问题列表，无数据返回空集合
     */
    List<String> listQuestionsByAppId(Long appId);
}
