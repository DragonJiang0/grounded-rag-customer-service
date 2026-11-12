package com.zidong.manage.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.common.exception.BizException;
import com.zidong.dao.dto.AddCorpusGroupDto;
import com.zidong.dao.entity.CorpusGroup;
import com.zidong.dao.mapper.CorpusGroupMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 语料分组 CRUD */
@Service
public class CorpusGroupService {

    private final CorpusGroupMapper groupMapper;
    /** 与语料服务互相依赖（语料创建时要校验分组），此处延迟注入打破循环 */
    private final CorpusService corpusService;

    public CorpusGroupService(CorpusGroupMapper groupMapper, @Lazy CorpusService corpusService) {
        this.groupMapper = groupMapper;
        this.corpusService = corpusService;
    }

    /** 返回全部小组 */
    public List<CorpusGroup> listAll() {
        return groupMapper.selectAllOrdered();
    }

    /** 按 ID 批量取分组，返回 id -> 分组 */
    public Map<Long, CorpusGroup> mapByIds(Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Map.of();
        }
        return groupMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(CorpusGroup::getId, Function.identity()));
    }

    @Transactional
    public void create(AddCorpusGroupDto request) {
        String name = request.getName().trim();
        ensureNameUnique(name, null);

        CorpusGroup group = new CorpusGroup();
        group.setName(name);
        groupMapper.insert(group);
    }

    @Transactional
    public CorpusGroup update(Long id, AddCorpusGroupDto request) {
        CorpusGroup group = checkExists(id);
        String name = request.getName().trim();
        ensureNameUnique(name, id);

        group.setName(name);
        groupMapper.updateById(group);
        return groupMapper.selectById(id);
    }

    @Transactional
    public void delete(Long id) {
        checkExists(id);
        // 分组下还有语料时不允许删除，避免语料变成游离数据
        long corpusCount = corpusService.countByGroupId(id);
        if (corpusCount > 0) {
            throw BizException.conflict("分组下存在 " + corpusCount + " 条语料，请先移除后再删除");
        }
        groupMapper.deleteById(id);
    }

    /** 校验分组存在 */
    public CorpusGroup checkExists(Long id) {
        CorpusGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw BizException.notFound("语料分组不存在：" + id);
        }
        return group;
    }

    /* ------------------------------------------------------------ 内部方法 */

    private void ensureNameUnique(String name, Long excludeId) {
        Long count = groupMapper.selectCount(Wrappers.<CorpusGroup>lambdaQuery()
                .eq(CorpusGroup::getName, name)
                .ne(excludeId != null, CorpusGroup::getId, excludeId));
        if (count != null && count > 0) {
            throw BizException.conflict("分组名称已存在：" + name);
        }
    }

}
