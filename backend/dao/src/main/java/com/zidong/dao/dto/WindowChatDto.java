package com.zidong.dao.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 免密窗口对话请求 */
@Data
public class WindowChatDto {

    @NotBlank(message = "windowKey 不能为空")
    private String windowKey;

    @NotBlank(message = "Message cannot be blank")
    private String message;

    private String taskId;
}
