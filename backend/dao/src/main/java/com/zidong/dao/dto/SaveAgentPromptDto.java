package com.zidong.dao.dto;

import com.zidong.dao.entity.AgentPrompt;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Agent 调试提示词的保存请求 */
@Getter
@Setter
public class SaveAgentPromptDto {

    /** Agent 名称，对应 agent_prompt.agent_name 唯一键 */
    @NotBlank(message = "缺少 Agent 名称")
    @Size(max = 64, message = "Agent 名称不能超过 64 个字符")
    private String agentName;

    /** 提示词内容，允许为空串（表示不使用提示词包装） */
    @Size(max = 20000, message = "提示词内容不能超过 20000 个字符")
    private String content = "";

    /** 按本请求构建待保存的提示词实体 */
    public AgentPrompt toSave() {
        LocalDateTime now = LocalDateTime.now();
        return AgentPrompt.builder()
                .agentName(agentName.trim())
                .content(content == null ? "" : content.trim())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
