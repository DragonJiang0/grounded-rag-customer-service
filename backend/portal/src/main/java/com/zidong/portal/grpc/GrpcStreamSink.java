package com.zidong.portal.grpc;

import com.alibaba.fastjson2.JSON;
import com.zidong.common.sse.MessageSink;
import com.zidong.rpc.service.DebugPromptEvent;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 把 gRPC 服务端流的 {@link StreamObserver} 适配成 {@link MessageSink}。
 *
 * <p>事件语义与 SSE 完全对齐：{@code event} 落 {@link DebugPromptEvent#getEvent()}，
 * {@code data} 落 {@link DebugPromptEvent#getData()}，
 * 这样同一套业务代码既能推 SSE 也能推 gRPC。
 *
 * <p>与 {@code EmitterSink} 一样保持两个原子状态：
 * {@code closed} 表示通道是否还可用，{@code terminated} 保证 complete / fail 只生效一次。
 */
public class GrpcStreamSink implements MessageSink {

    private final StreamObserver<DebugPromptEvent> observer;

    /** StreamObserver 不是线程安全的，发送与终结必须串行化 */
    private final Object sendLock = new Object();

    /** 通道是否已不可用（发送失败或已终结），后续 send 直接拒绝 */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /** 终结动作（complete / fail）只允许生效一次 */
    private final AtomicBoolean terminated = new AtomicBoolean(false);

    /** 无操作 sink：没有真实 gRPC 连接时占位，事件直接丢弃 */
    public static GrpcStreamSink noop() {
        return new GrpcStreamSink(null);
    }

    public GrpcStreamSink(StreamObserver<DebugPromptEvent> observer) {
        this.observer = observer;
    }

    @Override
    public boolean isNoop() {
        return observer == null;
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void send(String event, Object data) {
        if (observer == null) {
            return; // noop：事件直接丢弃
        }
        if (closed.get()) {
            throw new SinkSendException("gRPC 流已关闭, event=" + event);
        }
        DebugPromptEvent response = response(event, data);
        synchronized (sendLock) {
            // 双重检查：可能已被并发的 send/fail 关闭
            if (closed.get()) {
                throw new SinkSendException("gRPC 流已关闭, event=" + event);
            }
            try {
                observer.onNext(response);
            } catch (Exception ex) {
                // 发送失败说明这条流已不可用，标记关闭；但终结动作仍留给 fail/complete，
                // 让上层有机会补推一条 error 事件（与 EmitterSink 的记录式收尾保持一致）
                closed.set(true);
                throw new SinkSendException("gRPC 发送失败, event=" + event, ex);
            }
        }
    }

    @Override
    public void complete() {
        if (observer == null) {
            return; // noop：无流可关
        }
        if (!terminated.compareAndSet(false, true)) {
            return; // 已终结，只生效一次
        }
        closed.set(true);
        synchronized (sendLock) {
            observer.onCompleted();
        }
    }

    @Override
    public void fail(Throwable ex) {
        if (observer == null) {
            return; // noop：无流可关
        }
        if (!terminated.compareAndSet(false, true)) {
            return; // 已终结，只生效一次
        }
        closed.set(true);
        String text = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        synchronized (sendLock) {
            // 与 SSE 实现一致：先补推一条 error 事件，客户端已断开时推不出去就忽略
            try {
                observer.onNext(response(ERROR, text));
            } catch (Exception sendFailure) {
                // 流已不可用，交给下面的 onError 收尾
            }
            // 再用异常结束调用，让调用方能区分「正常完成」和「异常结束」
            observer.onError(ex);
        }
    }

    /**
     * 组装一条流事件。
     * <p>proto 的 data 是文本字段，String 原样透传（与既有约定一致），其它类型再 JSON 序列化。
     */
    private DebugPromptEvent response(String event, Object data) {
        return DebugPromptEvent.newBuilder()
                .setEvent(event)
                .setData(toText(data))
                .build();
    }

    private String toText(Object data) {
        if (data == null) {
            return "";
        }
        return data instanceof String text ? text : JSON.toJSONString(data);
    }
}
