package com.zidong.dao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Agent 调试请求：指定 Agent 名称与用户问题，提示词可选 */
@Getter
@Setter
public class DebugAgentPromptDto {

    /** 要调试的 Agent 名称（运行时唯一标识） */
    @NotBlank(message = "请输入Agent名称")
    @Size(max = 64, message = "Agent 名称不能超过 64 个字符")
    private String agentName;

    /** 调试任务 ID */
    @NotBlank(message = "请输入任务ID")
    private String taskId;

    /** 提示词内容：仅对声明消费提示词的 Agent 生效，其余 Agent 忽略 */
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
    private int memoryRounds = 1;

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

        /** 最大输出 token 数，取值 4096~409600；为空表示不覆盖 */
        @Min(value = 4096, message = "最大输出 token 数不能小于 4096")
        @Max(value = 409600, message = "最大输出 token 数不能大于 409600")
        private Integer maxOutputTokens;
    }
}
