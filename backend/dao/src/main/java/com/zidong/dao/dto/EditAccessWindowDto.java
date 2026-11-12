package com.zidong.dao.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 免密窗口的编辑请求：仅允许修改名称 */
@Getter
@Setter
public class EditAccessWindowDto extends AddAccessWindowDto {

    @NotNull(message = "缺少免密窗口 ID")
    private Long id;
}
