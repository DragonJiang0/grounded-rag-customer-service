package com.zidong.portal.service;

import com.zidong.dao.entity.AppPrompt;
import com.zidong.dao.mapper.AppPromptMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 应用提示词。
 *
 * <p>提示词存在 app_prompt 表、按应用维度配置，本服务只负责按 appId 取出模板；
 * 占位符替换由 {@code AbstractQaAgent} 统一完成，保证数据库入口与外部传入提示词
 * 这两个入口用的是同一份替换逻辑。
 */
@Service
@RequiredArgsConstructor
public class AppPromptService {

    private static final Logger log = LoggerFactory.getLogger(AppPromptService.class);

    /**
     * 未配置提示词时的兜底模板：空串。
     * <p>空模板意味着该应用不需要大模型润色，调用方应直接把命中的标准答案返回，不必调用大模型。
     */
    public static final String DEFAULT_PROMPT = "";

    private final AppPromptMapper appPromptMapper;

    /**
     * 取应用的提示词模板（含占位符，未替换），未配置时返回空串。
     * <p>app_prompt.app_id 上有唯一键，一个应用最多一条。
     *
     * @param appId 应用 ID
     * @return 提示词模板，未配置时为空串
     */
    public String template(Long appId) {
        AppPrompt appPrompt = appPromptMapper.findByAppId(appId);
        if (appPrompt == null || StringUtils.isBlank(appPrompt.getContent())) {
            log.info("应用未配置提示词, appId={}", appId);
            return DEFAULT_PROMPT;
        }
        return appPrompt.getContent();
    }
}
