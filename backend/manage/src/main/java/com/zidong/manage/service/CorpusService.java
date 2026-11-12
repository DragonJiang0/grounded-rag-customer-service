package com.zidong.manage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.common.exception.BizException;
import com.zidong.common.result.PageResult;
import com.zidong.dao.dto.AddCorpusDto;
import com.zidong.dao.dto.CorpusPageQueryDto;
import com.zidong.dao.dto.EditCorpusDto;
import com.zidong.dao.entity.Corpus;
import com.zidong.dao.entity.CorpusQuestion;
import com.zidong.dao.vo.CorpusVO;
import com.zidong.dao.mapper.CorpusMapper;
import com.zidong.dao.mapper.CorpusQuestionMapper;
import com.zidong.manage.support.PageResults;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 语料管理 */
@Service
@RequiredArgsConstructor
public class CorpusService {

    private final CorpusMapper corpusMapper;
    private final CorpusQuestionMapper questionMapper;
    private final CorpusGroupService groupService;


    public PageResult<CorpusVO> page(CorpusPageQueryDto request) {
        IPage<CorpusVO> page = corpusMapper.selectPageByQuery(request.toPage(), request);
        fillQuestions(page.getRecords());
        return PageResults.of(page);
    }

    /** 给当前页语料补齐问题列表 */
    private void fillQuestions(List<CorpusVO> records) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }
        List<Long> corpusIds = records.stream().map(Corpus::getId).toList();
        Map<Long, List<CorpusQuestion>> grouped = questionMapper.selectByCorpusIds(corpusIds).stream()
                .collect(Collectors.groupingBy(CorpusQuestion::getCorpusId));

        records.forEach(corpus -> corpus.setQuestions(grouped.getOrDefault(corpus.getId(), List.of())));
    }

    @Transactional
    public void create(AddCorpusDto request) {
        // 分组不存在直接抛业务异常
        groupService.checkExists(request.getGroupId());

        Corpus corpus = request.toSave();
        corpusMapper.insert(corpus);

        questionMapper.insert(request.toQuestions(corpus.getId()));
    }

    @Transactional
    public void update(EditCorpusDto request) {
        // 语料或分组不存在直接抛业务异常
        Corpus corpus = checkExists(request.getId());
        groupService.checkExists(request.getGroupId());

        corpusMapper.updateById(request.update(corpus));

        // 问题表里冗余了分组，整组替换最省事
        questionMapper.deleteByCorpusId(corpus.getId());
        questionMapper.insert(request.toQuestions(corpus.getId()));
    }

    /** 切换启用状态，返回切换后的状态 */
    public boolean toggleEnabled(Long id) {
        Corpus corpus = checkExists(id);
        boolean enabled = !Boolean.TRUE.equals(corpus.getEnabled());

        // 只更新 enabled 一列：不回写整行旧值，updated_at 交给数据库刷新
        corpusMapper.update(null, Wrappers.<Corpus>lambdaUpdate()
                .set(Corpus::getEnabled, enabled)
                .eq(Corpus::getId, id));
        return enabled;
    }

    @Transactional
    public void delete(Long id) {
        checkExists(id);
        questionMapper.deleteByCorpusId(id);
        corpusMapper.deleteById(id);
    }

    /** 统计某分组下的语料数量 */
    public long countByGroupId(Long groupId) {
        Long count = corpusMapper.selectCount(Wrappers.<Corpus>lambdaQuery()
                .eq(Corpus::getGroupId, groupId));
        return count == null ? 0L : count;
    }

    public Corpus checkExists(Long id) {
        Corpus corpus = corpusMapper.selectById(id);
        if (corpus == null) {
            throw BizException.notFound("语料不存在：" + id);
        }
        return corpus;
    }
}
