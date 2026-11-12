package com.zidong.dao.vo;

import com.zidong.dao.entity.AppChatOptions;
import com.zidong.dao.entity.AppPrompt;
import lombok.Getter;
import lombok.Setter;

/** 应用设置详情 */
@Getter
@Setter
public class AppSettingVO {

    /** 提示词内容（含 {question}、{answer} 变量），未配置时为空串 */
    private String content = "";

    /** 应用模型对话参数（字段为空表示不覆盖），未配置时为 null */
    private AppChatOptions chatOptions;

    /** 从提示词与对话参数实体构建设置详情，未配置时对应字段为空 */
    public static AppSettingVO of(AppPrompt prompt, AppChatOptions chatOptions) {
        AppSettingVO vo = new AppSettingVO();
        vo.setContent(prompt == null || prompt.getContent() == null ? "" : prompt.getContent());
        vo.setChatOptions(chatOptions);
        return vo;
    }
}
