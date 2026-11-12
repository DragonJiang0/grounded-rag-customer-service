package com.zidong.dao.dto;

import com.zidong.dao.entity.AppPrompt;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 应用提示词的保存请求 */
@Getter
@Setter
public class SaveAppPromptDto {

    /** 应用 ID，对应 app_prompt.app_id 唯一键 */
    @NotNull(message = "缺少应用 ID")
    private Long appId;

    /** 提示词内容，必须同时含 {question}、{answer} 变量（见 PromptConstants） */
    @NotBlank(message = "请输入提示词内容")
    @Size(max = 20000, message = "提示词内容不能超过 20000 个字符")
    private String content;

    /** 按本请求构建待保存的提示词实体 */
    public AppPrompt toSave() {
        LocalDateTime now = LocalDateTime.now();
        return AppPrompt.builder()
                .appId(appId)
                .content(content.trim())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
