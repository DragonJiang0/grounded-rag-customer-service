package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.entity.CorpusQuestion;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

/** 语料问题 */
public interface CorpusQuestionMapper extends BaseMapper<CorpusQuestion> {

    /** 批量取语料的问题 */
    default List<CorpusQuestion> selectByCorpusIds(Collection<Long> corpusIds) {
        if (CollectionUtils.isEmpty(corpusIds)) {
            return List.of();
        }
        LambdaQueryWrapper<CorpusQuestion> wrapper = Wrappers.lambdaQuery();
        wrapper.in(CorpusQuestion::getCorpusId, corpusIds)
                .orderByAsc(CorpusQuestion::getCorpusId)
                .orderByAsc(CorpusQuestion::getId);

        return selectList(wrapper);
    }

    /** 清空语料的问题 */
    default int deleteByCorpusId(Long corpusId) {
        return delete(Wrappers.<CorpusQuestion>lambdaQuery()
                .eq(CorpusQuestion::getCorpusId, corpusId));
    }
}
