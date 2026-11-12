package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zidong.dao.dto.CorpusPageQueryDto;
import com.zidong.dao.entity.Corpus;
import com.zidong.dao.vo.CorpusVO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/** 语料 */
public interface CorpusMapper extends BaseMapper<Corpus> {

    /** 分页查询语料 */
    IPage<CorpusVO> selectPageByQuery(Page<CorpusVO> page, @Param("query") CorpusPageQueryDto query);

    /**
     * 按问题文本批量查询答案。
     * <p>SQL 见 {@code resources/mapper/CorpusMapper.xml}。
     *
     * @param questions 问题文本集合，调用方需保证非空
     * @return 命中的答案列表，无数据返回空集合
     */
    List<String> listAnswersByQuestions(@Param("questions") Collection<String> questions);
}
