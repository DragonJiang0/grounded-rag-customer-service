package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.entity.CorpusGroup;

import java.util.List;

/** 语料分组 */
public interface CorpusGroupMapper extends BaseMapper<CorpusGroup> {

    /** 返回全部小组 */
    default List<CorpusGroup> selectAllOrdered() {
        return selectList(Wrappers.<CorpusGroup>lambdaQuery()
                .orderByAsc(CorpusGroup::getId));
    }
}
