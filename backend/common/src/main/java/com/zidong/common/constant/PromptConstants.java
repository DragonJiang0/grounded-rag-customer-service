package com.zidong.common.constant;

import java.util.List;

/**
 * 提示词占位符常量。
 *
 * <p>提示词（app_prompt.content）中必须同时包含 {@link #QUESTION_PLACEHOLDER} 与
 * {@link #ANSWER_PLACEHOLDER} 两个占位符，落库前由 manage 侧校验，
 * 使用前由 portal 侧分别替换为用户问题与命中语料的标准答案，缺失会导致无法正确包装返回。
 */
public final class PromptConstants {

    /** 用户问题占位符，会被替换为用户问题 */
    public static final String QUESTION_PLACEHOLDER = "{question}";

    /** 标准答案占位符，会被替换为命中语料的标准答案 */
    public static final String ANSWER_PLACEHOLDER = "{answer}";

    public static final String SYSTEM_KEYWORD = "{systemKeyword}";

    /** 必需占位符，提示词中缺一不可 */
    public static final List<String> REQUIRED_PLACEHOLDERS =
            List.of(QUESTION_PLACEHOLDER, ANSWER_PLACEHOLDER);

    private PromptConstants() {
    }

    /**
     * 取出提示词中缺失的必需占位符。
     *
     * @param content 提示词内容，可为 null
     * @return 缺失的占位符列表，全部存在时返回空列表
     */
    public static List<String> missingPlaceholders(String content) {
        if (content == null || content.isBlank()) {
            return REQUIRED_PLACEHOLDERS;
        }
        return REQUIRED_PLACEHOLDERS.stream()
                .filter(placeholder -> !content.contains(placeholder))
                .toList();
    }
}
