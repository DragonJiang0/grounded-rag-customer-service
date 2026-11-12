package com.zidong.dao.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 语料分组的编辑请求 */
@Getter
@Setter
public class EditCorpusGroupDto extends AddCorpusGroupDto {

    @NotNull(message = "缺少分组 ID")
    private Long id;
}
