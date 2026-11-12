package com.zidong.dao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 提示词调试请求：直接传入提示词与问题，不依赖已保存的配置 */
@Getter
@Setter
public class DebugAppPromptDto {

    /** 应用 ID：命中语料需要在该应用的语料范围内匹配 */
    @NotNull(message = "请输入应用ID")
    private Long appId;

    /** 调试任务 ID */
    @NotBlank(message = "请输入任务ID")
    private String taskId;



    /** 提示词内容，必须同时含 {question}、{answer} 变量（与保存接口同一套校验） */
    @Size(max = 40000, message = "提示词内容不能超过 40000 个字符")
    private String prompt;

    /** 待调试的用户问题 */
    @NotBlank(message = "请输入问题")
    @Size(max = 4000, message = "问题不能超过 4000 个字符")
    private String question;

    /** 是否开启会话记忆：未传或为 true 表示开启，false 表示本次调试不带上下文 */
    private boolean memoryEnabled;

    /** 会话记忆保留的问答轮数，取值 1~100；未传表示用服务端默认轮数 */
    @Min(value = 1, message = "记忆轮数不能小于 1")
    @Max(value = 100, message = "记忆轮数不能大于 100")
    private int memoryRounds=1;
    /** 本次调试使用的模型对话参数，为空表示不覆盖、回落到模型默认值 */
    @Valid
    private Ops ops;

    /** 模型对话参数：字段为空表示不覆盖，取值范围与保存接口一致 */
    @Getter
    @Setter
    public static class Ops {

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
}
