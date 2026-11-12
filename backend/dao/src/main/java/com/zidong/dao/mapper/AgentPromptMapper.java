package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.entity.AgentPrompt;
import org.apache.ibatis.annotations.Param;

/** Agent 调试提示词 */
public interface AgentPromptMapper extends BaseMapper<AgentPrompt> {

    /**
     * 按 agent_name 保存提示词：已存在则覆盖 content。
     * <p>依赖 agent_prompt.agent_name 上的唯一键，SQL 见 {@code resources/mapper/AgentPromptMapper.xml}。
     *
     * @param prompt 待保存的提示词，agentName 必传
     * @return 受影响行数，插入返回 1，命中唯一键更新返回 2
     */
    int saveOrUpdateByAgentName(@Param("prompt") AgentPrompt prompt);

    /** 按 Agent 名称取提示词，查不到返回 null */
    default AgentPrompt findByAgentName(String agentName) {
        return selectOne(Wrappers.<AgentPrompt>lambdaQuery()
                .eq(AgentPrompt::getAgentName, agentName));
    }

    /** 删除某 Agent 的提示词：没有配置时删除 0 行，按成功处理 */
    default int deleteByAgentName(String agentName) {
        return delete(Wrappers.<AgentPrompt>lambdaQuery()
                .eq(AgentPrompt::getAgentName, agentName));
    }
}
