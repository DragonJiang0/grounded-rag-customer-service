package com.zidong.dao.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/** 语料分页查询条件 */
@Getter
@Setter
public class CorpusPageQueryDto extends PageDto {

    /** 所属分组，为空表示不限 */
    private Long groupId;

    /** 语料名称，模糊匹配 */
    private String name;

    /** 关键词，命中任意一个即算命中；匹配该语料下的问题文本（标准问 / 相似问） */
    private List<String> keywords;

    /** 答案关键词，模糊匹配 */
    private String answer;

    /** 清洗后的关键词：去空白、trim 并去重 */
    public List<String> getKeywords() {
        if (CollectionUtils.isEmpty(keywords)) {
            return List.of();
        }
        return keywords.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }
}
