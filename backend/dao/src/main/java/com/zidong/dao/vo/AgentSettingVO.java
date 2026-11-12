package com.zidong.dao.vo;

import lombok.Data;

/** Agent 调试设置详情：目前只有提示词 */
@Data
public class AgentSettingVO {

    /** 提示词内容，未配置时为空串 */
    private String content = "";

    public static AgentSettingVO of(String content) {
        AgentSettingVO vo = new AgentSettingVO();
        vo.setContent(content == null ? "" : content);
        return vo;
    }
}
