package com.zidong.dao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 语料分组的新增请求 */
@Getter
@Setter
public class AddCorpusGroupDto {

    @NotBlank(message = "请输入分组名称")
    @Size(max = 64, message = "分组名称不能超过 64 个字符")
    private String name;
}
