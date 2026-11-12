package com.zidong.dao.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 应用模型对话参数的保存请求 */
@Getter
@Setter
public class SaveAppChatOptionsDto {

    /** 应用 ID，按此更新 app_chat_options */
    @NotNull(message = "缺少应用 ID")
    private Long appId;

    /** 采样温度，取值 0.0~2.0 */
    @DecimalMin(value = "0.0", message = "采样温度不能小于 0.0")
    @DecimalMax(value = "2.0", message = "采样温度不能大于 2.0")
    private BigDecimal temperature;

    /** nucleus 采样，取值 0.1~0.9 */
    @DecimalMin(value = "0.1", message = "nucleus 采样不能小于 0.1")
    @DecimalMax(value = "0.9", message = "nucleus 采样不能大于 0.9")
    private BigDecimal topP;

    /** 最大输出 token 数，取值 4096~409600 */
    @NotNull(message = "请输入最大输出 token 数")
    @Min(value = 4096, message = "最大输出 token 数不能小于 4096")
    @Max(value = 409600, message = "最大输出 token 数不能大于 409600")
    private Integer maxOutputTokens;
}
