package com.zidong.manage.config;

import com.zidong.common.exception.BizException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** 全局异常处理 */
// 不加 basePackages/value 过滤，覆盖本应用全部 Controller：
// 该注解的 value 是 basePackages 的别名，传非包名字符串会把作用范围收窄到不存在的包，导致整个 Advice 失效
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String DEFAULT_ERROR_MESSAGE = "服务内部错误";

    /** 业务异常 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ProblemDetail> handleBiz(BizException ex, WebRequest request) {
        String message = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : DEFAULT_ERROR_MESSAGE;
        logWithStack("业务异常", ex, request);
        // 状态由异常自带（conflict→409、notFound→404 等），不再一律 500
        return error(ex.getStatus(), message, request);
    }

    /** 请求体校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "参数不合法" : fe.getDefaultMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new));
        logWithStack("请求体参数校验未通过 " + errors, ex, request);
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "请求参数校验未通过", request);
        detail.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(detail);
    }

    /** 方法参数校验失败 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleMethodValidation(HandlerMethodValidationException ex, WebRequest request) {
        String message = ex.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream())
                .map(e -> e.getDefaultMessage() == null ? "参数不合法" : e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        String finalMessage = message.isEmpty() ? "请求参数校验未通过" : message;
        logWithStack("方法参数校验未通过 " + finalMessage, ex, request);
        return error(HttpStatus.BAD_REQUEST, finalMessage, request);
    }

    /** 约束校验异常 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        String finalMessage = message.isEmpty() ? "请求参数校验未通过" : message;
        logWithStack("约束校验未通过 " + finalMessage, ex, request);
        return error(HttpStatus.BAD_REQUEST, finalMessage, request);
    }

    /** 缺少必填查询参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        logWithStack("缺少必填参数 " + ex.getParameterName(), ex, request);
        return error(HttpStatus.BAD_REQUEST, "缺少必填参数：" + ex.getParameterName(), request);
    }

    /** 参数类型不匹配 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        logWithStack("参数类型不正确 " + ex.getName(), ex, request);
        return error(HttpStatus.BAD_REQUEST, "参数类型不正确：" + ex.getName(), request);
    }

    /** 请求体无法解析 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex, WebRequest request) {
        logWithStack("请求体格式错误，无法解析", ex, request);
        return error(HttpStatus.BAD_REQUEST, "请求体格式错误，无法解析", request);
    }

    /** 兜底异常处理 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, WebRequest request) {
        logWithStack("未处理异常", ex, request);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, DEFAULT_ERROR_MESSAGE, request);
    }

    /* ------------------------------------------------------------ 内部方法 */

    /** 统一以 error 级别输出完整堆栈，throwable 作为最后一个参数即可被 slf4j 识别并打印堆栈 */
    private static void logWithStack(String summary, Throwable ex, WebRequest request) {
        log.error("{} | {} | {}", summary, request.getDescription(false), ex.getClass().getName(), ex);
    }

    private static ResponseEntity<ProblemDetail> error(HttpStatus status, String message, WebRequest request) {
        return ResponseEntity.status(status).body(problem(status, message, request));
    }

    private static ProblemDetail problem(HttpStatus status, String detailMessage, WebRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, detailMessage);
        detail.setTitle(status.getReasonPhrase());
        detail.setProperty("timestamp", OffsetDateTime.now().toString());
        String description = request.getDescription(false);
        if (description != null && description.startsWith("uri=")) {
            detail.setInstance(URI.create(description.substring(4)));
        }
        return detail;
    }
}
