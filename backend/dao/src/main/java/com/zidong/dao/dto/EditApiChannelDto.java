package com.zidong.dao.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** API 渠道的编辑请求 */
@Getter
@Setter
public class EditApiChannelDto extends AddApiChannelDto {

    @NotNull(message = "缺少渠道 ID")
    private Long id;
}
