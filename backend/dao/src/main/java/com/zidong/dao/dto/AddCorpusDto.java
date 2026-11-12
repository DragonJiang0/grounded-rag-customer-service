package com.zidong.dao.dto;

import com.zidong.dao.entity.Corpus;
import com.zidong.dao.entity.CorpusQuestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;

/** 语料的新增请求 */
@Getter
@Setter
public class AddCorpusDto {

    @NotBlank(message = "请输入语料名称")
    @Size(max = 64, message = "语料名称不能超过 64 个字符")
    private String name;

    @NotBlank(message = "请输入答案")
    private String answer;

    /** 所属分组 */
    @NotNull(message = "请选择所属分组")
    private Long groupId;

    /**
     * 问题列表，落 corpus_question 表。
     *
     * <p>用 LinkedHashSet 接：重复问题自动去重，同时保留提交顺序——
     * 第一条是标准问（换成 Set 会退化成 HashSet，顺序丢失）。
     */
    private LinkedHashSet<String> questions;

    /** 按本请求构建语料实体 */
    public Corpus toSave() {
        return Corpus.builder()
                .name(name.trim())
                .answer(answer.trim())
                .groupId(groupId)
                .enabled(true)
                .build();
    }

    /** 按本请求构建问题实体 */
    public List<CorpusQuestion> toQuestions(Long corpusId) {
        if (questions == null) {
            return List.of();
        }
        return questions.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .map(question -> CorpusQuestion.builder()
                        .corpusId(corpusId)
                        .groupId(groupId)
                        .question(question)
                        .build())
                .toList();
    }
}
