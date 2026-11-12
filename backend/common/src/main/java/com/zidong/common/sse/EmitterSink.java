package com.zidong.common.sse;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link MessageSink} 的 SSE 实现：把命名事件写到一条 SseEmitter 连接上，manage / portal 共用。
 *
 * <p>对外 API（{@code status/message/error/done/complete/fail/send/isClosed/isNoop}）与抽象之前完全一致，
 * 只是额外实现了 {@link MessageSink}，所以既有调用方零改动。
 */
public class EmitterSink implements MessageSink {

    private static final Logger log = LoggerFactory.getLogger(EmitterSink.class);

    private final SseEmitter emitter;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private EmitterSink(SseEmitter emitter) {
        this.emitter = emitter;
    }

    /**
     * 无操作 sink：没有 SSE 连接时占位。
     * <p>MessageSink 是 answer/hitQuestion 的前置条件，黑板里必须始终有一个，
     * 否则 planner 找不到计划，进程会 STUCK。
     */
    public static EmitterSink noop() {
        return new EmitterSink(null);
    }

    public static EmitterSink create(long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        EmitterSink sink = new EmitterSink(emitter);
        emitter.onCompletion(() -> sink.markClosed("completed"));
        emitter.onTimeout(() -> sink.markClosed("timeout"));
        emitter.onError(ex -> sink.markClosed("error: " + ex.getMessage()));
        return sink;
    }

    public SseEmitter emitter() {
        return emitter;
    }

    @Override
    public boolean isNoop() {
        return emitter == null;
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void complete() {
        if (emitter == null) {
            return; // noop：无连接可关
        }
        if (closed.compareAndSet(false, true)) {
            emitter.complete();
        }
    }

    @Override
    public void fail(Throwable ex) {
        if (emitter == null) {
            return; // noop：无连接可关
        }
        if (closed.compareAndSet(false, true)) {
            String text = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            try {
                emitter.send(sseEvent(ERROR, text));
            } catch (Exception sendFailure) {
                log.debug("推送 error 事件失败（连接可能已断开）: {}", sendFailure.getMessage());
            }
            emitter.completeWithError(ex);
        }
    }

    @Override
    public void send(String event, Object data) {
        if (emitter == null) {
            return; // noop：事件直接丢弃
        }
        if (closed.get()) {
            throw new SseSendException("SSE 连接已关闭, event=" + event);
        }
        try {
            emitter.send(sseEvent(event, data));
        } catch (IOException ex) {
            closed.set(true);
            throw new SseSendException("SSE 发送失败, event=" + event, ex);
        } catch (IllegalStateException ex) {
            closed.set(true);
            throw new SseSendException("SSE 连接已不可用, event=" + event, ex);
        }
    }

    /**
     * 构造标准 SSE 事件：事件名走 event 字段、业务数据走 data 字段。
     * <pre>
     * event: message
     * data: "渲染结果"
     *
     * </pre>
     * data 统一 JSON 序列化，保证换行等特殊字符被转义在同一行内，避免多行 data 破坏事件分帧。
     * 前端按 event 字段分发到 status / message / done，因此这里不能再把事件名包进 data 的 JSON 里。
     */
    private SseEmitter.SseEventBuilder sseEvent(String event, Object data) {
        return SseEmitter.event()
                .name(event)
                .data(JSON.toJSONString(data), MediaType.TEXT_PLAIN);
    }

    private void markClosed(String reason) {
        if (closed.compareAndSet(false, true)) {
            log.info("SSE 连接已关闭（{}）", reason);
        }
    }

    /**
     * SSE 专属的发送异常：继承自 {@link MessageSink.SinkSendException}，
     * 保留原有类型名以兼容既有的 catch 写法。
     */
    public static class SseSendException extends SinkSendException {

        public SseSendException(String message) {
            super(message);
        }

        public SseSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
