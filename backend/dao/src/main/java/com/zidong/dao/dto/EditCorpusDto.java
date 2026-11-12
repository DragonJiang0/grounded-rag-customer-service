package com.zidong.dao.dto;

import com.zidong.dao.entity.Corpus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 语料的编辑请求 */
@Getter
@Setter
public class EditCorpusDto extends AddCorpusDto {

    @NotNull(message = "缺少语料 ID")
    private Long id;

    /** 把本请求的字段写到已查出的实体上 */
    public Corpus update(Corpus corpus) {
        corpus.setName(getName().trim());
        corpus.setAnswer(getAnswer().trim());
        corpus.setGroupId(getGroupId());
        return corpus;
    }
}
