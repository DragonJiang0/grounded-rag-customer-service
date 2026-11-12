package com.zidong.common.sse;

/**
 * 面向客户端的推送通道抽象。
 *
 * <p>实现可以是 SSE（{@link EmitterSink}）、gRPC 服务端流（GrpcStreamSink）、WebSocket、内存队列等。
 * 上层业务只依赖本接口，不再关心「事件往哪里送」，因此同一套流程可以同时跑在多种传输之上。
 *
 * <p>事件语义统一约定为：事件名走 {@link #TASK_ID} / {@link #STATUS} / {@link #MESSAGE} / {@link #ERROR} / {@link #DONE}，
 * 载荷 data 交给具体实现去序列化（SSE 序列化成 JSON，gRPC 走 proto 的文本字段）。
 */
public interface MessageSink {

    /** 任务标识，用于客户端把本次推送关联到具体会话 / 任务 */
    String TASK_ID = "taskId";

    /** 阶段提示 */
    String STATUS = "status";

    /** 过程消息（流式场景下是累积到当前的完整文本） */
    String MESSAGE = "message";

    /** 错误信息 */
    String ERROR = "error";

    /** 最终答复，发送后随即结束通道 */
    String DONE = "done";

    /**
     * sink 在黑板绑定里的固定名字。
     * <p>agent 的 {@code @Provided} 参数按类型解析，这里保持稳定的键名便于排查问题。
     */
    String SINK = "sink";

    /** 是否为占位（没有真实连接），事件直接丢弃 */
    boolean isNoop();

    /** 是否已终止（正常结束 / 异常结束 / 超时 / 客户端断开） */
    boolean isClosed();

    /** 发送一条命名事件，data 由实现自行序列化 */
    void send(String event, Object data);

    /** 正常结束，只生效一次 */
    void complete();

    /** 异常结束，只生效一次；实现应尽量补推一条 error 事件后再终结 */
    void fail(Throwable ex);

    // ---- 语义便捷方法，默认实现即可，实现类无需重复 ----

    default void status(String text) {
        send(STATUS, text);
    }

    default void message(Object data) {
        send(MESSAGE, data);
    }

    default void error(String text) {
        send(ERROR, text);
    }

    default void done(Object data) {
        send(DONE, data);
        complete();
    }

    /** 发送失败时抛出，携带「哪个事件推失败」的上下文 */
    class SinkSendException extends RuntimeException {

        public SinkSendException(String message) {
            super(message);
        }

        public SinkSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
