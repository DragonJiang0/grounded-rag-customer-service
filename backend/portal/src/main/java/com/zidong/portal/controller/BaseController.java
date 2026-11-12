package com.zidong.portal.controller;

import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.stream.Collectors;

/** 控制器基类 */
public abstract class BaseController {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected HttpServletRequest request;

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /** 打印请求日志 */
    protected void requestLog(Object... params) {
        try {
            // 不传参数时输出 null；只传一个参数时保持原有格式；多个参数按数组输出
            Object toLog = null;
            if (params != null && params.length == 1) {
                toLog = params[0];
            } else if (params != null && params.length > 1) {
                toLog = params;
            }
            log.info("请求 url={},入参={}", request.getRequestURI(), JSON.toJSONString(toLog));
        } catch (Exception e) {
            log.error("请求日志序列化失败", e);
        }
    }

    /**
     * 问答接口耗时（info 级别）：问答是长耗时调用，线上日志级别是 info，debug 看不到，
     * 因此统一在这里打一条，各入口格式一致，便于按 taskId 或 api 统计。
     *
     * <p>接口标识由调用方传入而不用 {@link #request}：流式入口在虚拟线程里执行，
     * 那里没有 thread-bound request，取 request 会抛异常。</p>
     *
     * @param api        接口标识，如 {@code /api/chat/block}
     * @param taskId     会话标识，用于把选择、执行、收尾串成一条链路
     * @param startNanos 处理开始时间，取自 {@link System#nanoTime()}
     */
    protected void logAnswerCost(String api, String taskId, long startNanos) {
        long costMillis = (System.nanoTime() - startNanos) / 1_000_000L;
        log.info("[问答耗时] api={}, taskId={}, 耗时={}ms", api, taskId, costMillis);
    }

    /** 校验对象参数，违反约束时抛异常 */
    protected void validate(Object t) {
        Set<ConstraintViolation<Object>> violations = validator.validate(t);

        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(","));
            throw new IllegalArgumentException(msg);
        }
    }
}
