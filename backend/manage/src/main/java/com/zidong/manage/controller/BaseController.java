package com.zidong.manage.controller;

import com.alibaba.fastjson.JSON;
import com.zidong.common.exception.BizException;
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

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

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
            logger.info("请求 url={},入参={}", request.getRequestURI(), JSON.toJSONString(toLog));
        } catch (Exception e) {
            logger.error("请求日志序列化失败", e);
        }
    }

    /** 校验对象参数，违反约束时抛异常 */
    protected void validate(Object t) {
        Set<ConstraintViolation<Object>> violations = validator.validate(t);

        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(","));
            throw new BizException(msg);
        }
    }
}
