package com.zidong.dao.dto;

import com.zidong.dao.entity.App;
import com.zidong.dao.entity.AppAgent;
import com.zidong.dao.entity.AppCorpusGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

/** 应用的新增请求 */
@Getter
@Setter
public class AddAppDto {

    @NotBlank(message = "请输入应用名称")
    @Size(max = 64, message = "应用名称不能超过 64 个字符")
    private String name;

    @Size(max = 500, message = "应用描述不能超过 500 个字符")
    private String description;

    /** 是否启用多 Agent：不传按关闭处理 */
    private boolean multiAgentEnabled;

    /**
     * 绑定的语料分组 ID，必选可多选。
     *
     * <p>用 LinkedHashSet 接：重复 ID 自动去重，同时保留提交顺序。
     */
    @NotEmpty(message = "请至少选择一个语料分组")
    private LinkedHashSet<Long> corpusGroupIds;

    /**
     * 参与多 Agent 选择的 Agent 名称，multiAgentEnabled 为 true 时必选可多选。
     *
     * <p>同样用 LinkedHashSet 接：重复名称自动去重，同时保留勾选顺序。
     */
    private LinkedHashSet<String> agents;

    /** 按本请求构建待保存的应用实体 */
    public App toSave(String appKey) {
        LocalDateTime now = LocalDateTime.now();
        return App.builder()
                .name(name.trim())
                .description(descriptionOrEmpty())
                .appKey(appKey)
                .multiAgentEnabled(Boolean.TRUE.equals(multiAgentEnabled))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /** 描述选填：空白按空串处理 */
    public String descriptionOrEmpty() {
        return StringUtils.hasText(description) ? description.trim() : "";
    }

    /**
     * 多 Agent 开关与 Agent 选择的联动校验。
     *
     * <p>放在方法里而不是注解上，是因为「开关为 true 时 agents 必填」属于跨字段约束；
     * 且 dao 模块不依赖 common 模块，这里只返回提示语，由 controller 决定抛什么异常。
     *
     * @return 校验不通过时的提示语，通过时返回 null
     */
    public String checkMultiAgent() {
        if (!Boolean.TRUE.equals(multiAgentEnabled)) {
            return null;
        }
        // 只填了空白名称等同于没选
        boolean noneSelected = agents == null || agents.stream().noneMatch(StringUtils::hasText);
        return noneSelected ? "启用多 Agent 后请至少选择一个 Agent" : null;
    }

    /** 按本请求构建绑定关系 */
    public List<AppCorpusGroup> toRelations(Long appId) {
        LocalDateTime now = LocalDateTime.now();
        return corpusGroupIds.stream()
                .map(groupId -> AppCorpusGroup.builder()
                        .appId(appId)
                        .groupId(groupId)
                        .createdAt(now)
                        .build())
                .toList();
    }

    /**
     * 按本请求构建 Agent 绑定关系，供 service 写入 app_agent 表。
     *
     * <p>空白名称跳过，trim 后去重，避免与 uk_app_agent 唯一键冲突。
     *
     * @param appId 应用 ID
     * @return 待插入的关联列表，未选 Agent 时返回空集合
     */
    public List<AppAgent> toAgentRelations(Long appId) {
        if (agents == null || agents.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        return agents.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .map(agentName -> AppAgent.builder()
                        .appId(appId)
                        .agentName(agentName)
                        .createdAt(now)
                        .build())
                .toList();
    }
}
