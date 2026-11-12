package com.zidong.dao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 删除窗口历史会话请求（免密窗口 / 账号密码登录窗口共用）：
 * 应用归属只能取自当前窗口（windowKey / 登录令牌已由过滤器校验并还原），
 * 调用方只需给出会话标识，不接受传入 AppKey。
 */
@Getter
@Setter
public class DeleteWindowHistoryDto {

    /** 会话标识（taskId）：同一会话的所有分段一起置为已删除 */
    @NotBlank(message = "会话标识不能为空")
    @Size(max = 64, message = "会话标识过长")
    private String conversationId;
}
