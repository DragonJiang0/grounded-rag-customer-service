package com.zidong.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 应用模型对话参数（字段为空表示不覆盖、回落到模型默认值） */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("app_chat_options")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppChatOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 应用 ID */
    private Long appId;

    /** 采样温度，null 表示不覆盖；建议 0.00~2.00，追求确定性取 0 或极小值 */
    private BigDecimal temperature;

    /** nucleus 采样，null 表示不覆盖；取值 0.00~1.00 */
    private BigDecimal topP;

    /** 最大输出 token 数，null 表示不覆盖 */
    private Integer maxOutputTokens;

    /** 是否开启对话记忆：true 开启，false 关闭；null 表示不覆盖，回落到默认开启 */
    private Boolean memoryEnabled;

    /** 对话记忆保留的问答轮数，一轮 = 用户消息 + 模型回复；null 表示不覆盖，回落到默认轮数 */
    private Integer memoryRounds;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
