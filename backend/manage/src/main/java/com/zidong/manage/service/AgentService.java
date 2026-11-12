package com.zidong.manage.service;

import com.zidong.dao.dto.SaveAgentPromptDto;
import com.zidong.dao.entity.AgentPrompt;
import com.zidong.dao.mapper.AgentConfigMapper;
import com.zidong.dao.mapper.AgentPromptMapper;
import com.zidong.dao.vo.AgentSettingVO;
import com.zidong.dao.vo.AgentVO;
import com.zidong.rpc.api.client.starter.service.PortalRpcService;
import com.zidong.rpc.service.ActionInfo;
import com.zidong.rpc.service.AgentInfo;
import com.zidong.rpc.service.GoalInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Agent 配置与查询 */
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentConfigMapper agentConfigMapper;

    /** Agent 调试提示词持久化 */
    private final AgentPromptMapper agentPromptMapper;

    /** Agent 的运行时元数据在 portal，通过 gRPC 获取 */
    private final PortalRpcService portalRpcService;

    /**
     * 查询 portal 上已注册的 Agent 元数据。
     *
     * <p>数据来自 portal 的平台注册表，manage 只做字段裁剪（名称 / 描述 / goals / actions），
     * 不在这里重新解析或缓存，保证与管理台看到的结果和运行时一致。</p>
     *
     * @return 按名称排序的 Agent 列表
     */
    public List<AgentVO> list() {
        return portalRpcService.listAgents().stream()
                .map(AgentService::toVO)
                .toList();
    }

    /**
     * Agent 调试设置详情：目前只有提示词。
     *
     * <p>agent_name 上有唯一键，一个 Agent 最多一条提示词，未配置时返回空的提示词内容。</p>
     *
     * @param agentName Agent 名称（唯一标识）
     */
    public AgentSettingVO settingInfo(String agentName) {
        AgentPrompt prompt = agentPromptMapper.findByAgentName(agentName);
        return AgentSettingVO.of(prompt == null ? null : prompt.getContent());
    }

    /**
     * 保存 Agent 调试提示词。
     *
     * <p>一个 Agent 只保留一条提示词：agent_prompt.agent_name 上有唯一键，
     * 已存在时由 MySQL 的 ON DUPLICATE KEY UPDATE 直接覆盖。</p>
     */
    @Transactional
    public void savePrompt(SaveAgentPromptDto request) {
        agentPromptMapper.saveOrUpdateByAgentName(request.toSave());
    }

    /**
     * 删除 Agent 调试提示词。
     *
     * <p>删除后该 Agent 回到「未配置提示词」状态；本来就没有提示词时按成功处理。</p>
     */
    @Transactional
    public void deletePrompt(String agentName) {
        agentPromptMapper.deleteByAgentName(agentName);
    }

    /** RPC 消息转展示 VO：只保留名称与描述 */
    private static AgentVO toVO(AgentInfo agent) {
        AgentVO vo = new AgentVO();
        vo.setName(agent.getName());
        vo.setDescription(agent.getDescription());
        vo.setGoals(agent.getGoalsList().stream().map(AgentService::toGoalVO).toList());
        vo.setActions(agent.getActionsList().stream().map(AgentService::toActionVO).toList());
        return vo;
    }

    private static AgentVO.GoalVO toGoalVO(GoalInfo goal) {
        AgentVO.GoalVO vo = new AgentVO.GoalVO();
        vo.setName(goal.getName());
        vo.setDescription(goal.getDescription());
        return vo;
    }

    private static AgentVO.ActionVO toActionVO(ActionInfo action) {
        AgentVO.ActionVO vo = new AgentVO.ActionVO();
        vo.setName(action.getName());
        vo.setDescription(action.getDescription());
        return vo;
    }
}
