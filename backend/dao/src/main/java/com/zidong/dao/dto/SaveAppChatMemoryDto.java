package com.zidong.dao.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 应用对话配置（会话记忆）的保存请求：仅更新对话记忆相关字段 */
@Getter
@Setter
public class SaveAppChatMemoryDto {

    /** 应用 ID，按此更新 app_chat_options */
    @NotNull(message = "缺少应用 ID")
    private Long appId;

    /** 是否开启对话记忆：true 开启，false 关闭 */
    @NotNull(message = "请选择是否开启对话记忆")
    private Boolean memoryEnabled;

    /** 对话记忆保留的问答轮数，一轮 = 用户消息 + 模型回复，取值 1~100 */
    @NotNull(message = "请输入对话记忆轮数")
    @Min(value = 1, message = "对话记忆轮数不能小于 1")
    @Max(value = 100, message = "对话记忆轮数不能大于 100")
    private Integer memoryRounds;
}
