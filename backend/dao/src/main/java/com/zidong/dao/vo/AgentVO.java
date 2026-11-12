package com.zidong.dao.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Agent 元数据（管理台展示用）。
 *
 * <p>只包含名称、描述、goal 与 action 的名称描述：
 * 提示词、模型参数、黑板绑定等运行期配置不对外暴露。</p>
 */
@Getter
@Setter
public class AgentVO {

    /** Agent 名称，同时是运行时唯一标识 */
    private String name;

    /** Agent 描述 */
    private String description;

    /** 该 Agent 能达成的目标 */
    private List<GoalVO> goals;

    /** 该 Agent 的动作 */
    private List<ActionVO> actions;

    /** Goal 的展示信息 */
    @Getter
    @Setter
    public static class GoalVO {

        private String name;

        private String description;
    }

    /** Action 的展示信息 */
    @Getter
    @Setter
    public static class ActionVO {

        private String name;

        private String description;
    }
}
