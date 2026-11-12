package com.zidong.rpc.service;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 **
 * 门户对外 RPC 服务。
 * 事件语义与 SSE 保持一致：event 取值 status / message / done / error，
 * data 为该事件对应的文本内容，便于调用方原样转发到前端。
 * </pre>
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class PortalServiceGrpc {

  private PortalServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "PortalService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.zidong.rpc.service.DebugPromptRequest,
      com.zidong.rpc.service.DebugPromptEvent> getDebugPromptMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DebugPrompt",
      requestType = com.zidong.rpc.service.DebugPromptRequest.class,
      responseType = com.zidong.rpc.service.DebugPromptEvent.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.zidong.rpc.service.DebugPromptRequest,
      com.zidong.rpc.service.DebugPromptEvent> getDebugPromptMethod() {
    io.grpc.MethodDescriptor<com.zidong.rpc.service.DebugPromptRequest, com.zidong.rpc.service.DebugPromptEvent> getDebugPromptMethod;
    if ((getDebugPromptMethod = PortalServiceGrpc.getDebugPromptMethod) == null) {
      synchronized (PortalServiceGrpc.class) {
        if ((getDebugPromptMethod = PortalServiceGrpc.getDebugPromptMethod) == null) {
          PortalServiceGrpc.getDebugPromptMethod = getDebugPromptMethod =
              io.grpc.MethodDescriptor.<com.zidong.rpc.service.DebugPromptRequest, com.zidong.rpc.service.DebugPromptEvent>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DebugPrompt"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.zidong.rpc.service.DebugPromptRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.zidong.rpc.service.DebugPromptEvent.getDefaultInstance()))
              .setSchemaDescriptor(new PortalServiceMethodDescriptorSupplier("DebugPrompt"))
              .build();
        }
      }
    }
    return getDebugPromptMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.zidong.rpc.service.ListAgentsRequest,
      com.zidong.rpc.service.ListAgentsResponse> getListAgentsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListAgents",
      requestType = com.zidong.rpc.service.ListAgentsRequest.class,
      responseType = com.zidong.rpc.service.ListAgentsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.zidong.rpc.service.ListAgentsRequest,
      com.zidong.rpc.service.ListAgentsResponse> getListAgentsMethod() {
    io.grpc.MethodDescriptor<com.zidong.rpc.service.ListAgentsRequest, com.zidong.rpc.service.ListAgentsResponse> getListAgentsMethod;
    if ((getListAgentsMethod = PortalServiceGrpc.getListAgentsMethod) == null) {
      synchronized (PortalServiceGrpc.class) {
        if ((getListAgentsMethod = PortalServiceGrpc.getListAgentsMethod) == null) {
          PortalServiceGrpc.getListAgentsMethod = getListAgentsMethod =
              io.grpc.MethodDescriptor.<com.zidong.rpc.service.ListAgentsRequest, com.zidong.rpc.service.ListAgentsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListAgents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.zidong.rpc.service.ListAgentsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.zidong.rpc.service.ListAgentsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PortalServiceMethodDescriptorSupplier("ListAgents"))
              .build();
        }
      }
    }
    return getListAgentsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.zidong.rpc.service.CreateBotQrcodeRequest,
      com.zidong.rpc.service.CreateBotQrcodeResponse> getCreateBotQrcodeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateBotQrcode",
      requestType = com.zidong.rpc.service.CreateBotQrcodeRequest.class,
      responseType = com.zidong.rpc.service.CreateBotQrcodeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.zidong.rpc.service.CreateBotQrcodeRequest,
      com.zidong.rpc.service.CreateBotQrcodeResponse> getCreateBotQrcodeMethod() {
    io.grpc.MethodDescriptor<com.zidong.rpc.service.CreateBotQrcodeRequest, com.zidong.rpc.service.CreateBotQrcodeResponse> getCreateBotQrcodeMethod;
    if ((getCreateBotQrcodeMethod = PortalServiceGrpc.getCreateBotQrcodeMethod) == null) {
      synchronized (PortalServiceGrpc.class) {
        if ((getCreateBotQrcodeMethod = PortalServiceGrpc.getCreateBotQrcodeMethod) == null) {
          PortalServiceGrpc.getCreateBotQrcodeMethod = getCreateBotQrcodeMethod =
              io.grpc.MethodDescriptor.<com.zidong.rpc.service.CreateBotQrcodeRequest, com.zidong.rpc.service.CreateBotQrcodeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateBotQrcode"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.zidong.rpc.service.CreateBotQrcodeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.zidong.rpc.service.CreateBotQrcodeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PortalServiceMethodDescriptorSupplier("CreateBotQrcode"))
              .build();
        }
      }
    }
    return getCreateBotQrcodeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.zidong.rpc.service.GetBotQrcodeStatusRequest,
      com.zidong.rpc.service.GetBotQrcodeStatusResponse> getGetBotQrcodeStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetBotQrcodeStatus",
      requestType = com.zidong.rpc.service.GetBotQrcodeStatusRequest.class,
      responseType = com.zidong.rpc.service.GetBotQrcodeStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.zidong.rpc.service.GetBotQrcodeStatusRequest,
      com.zidong.rpc.service.GetBotQrcodeStatusResponse> getGetBotQrcodeStatusMethod() {
    io.grpc.MethodDescriptor<com.zidong.rpc.service.GetBotQrcodeStatusRequest, com.zidong.rpc.service.GetBotQrcodeStatusResponse> getGetBotQrcodeStatusMethod;
    if ((getGetBotQrcodeStatusMethod = PortalServiceGrpc.getGetBotQrcodeStatusMethod) == null) {
      synchronized (PortalServiceGrpc.class) {
        if ((getGetBotQrcodeStatusMethod = PortalServiceGrpc.getGetBotQrcodeStatusMethod) == null) {
          PortalServiceGrpc.getGetBotQrcodeStatusMethod = getGetBotQrcodeStatusMethod =
              io.grpc.MethodDescriptor.<com.zidong.rpc.service.GetBotQrcodeStatusRequest, com.zidong.rpc.service.GetBotQrcodeStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetBotQrcodeStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.zidong.rpc.service.GetBotQrcodeStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.zidong.rpc.service.GetBotQrcodeStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PortalServiceMethodDescriptorSupplier("GetBotQrcodeStatus"))
              .build();
        }
      }
    }
    return getGetBotQrcodeStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.zidong.rpc.service.ResetBotRequest,
      com.zidong.rpc.service.ResetBotResponse> getResetBotMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ResetBot",
      requestType = com.zidong.rpc.service.ResetBotRequest.class,
      responseType = com.zidong.rpc.service.ResetBotResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.zidong.rpc.service.ResetBotRequest,
      com.zidong.rpc.service.ResetBotResponse> getResetBotMethod() {
    io.grpc.MethodDescriptor<com.zidong.rpc.service.ResetBotRequest, com.zidong.rpc.service.ResetBotResponse> getResetBotMethod;
    if ((getResetBotMethod = PortalServiceGrpc.getResetBotMethod) == null) {
      synchronized (PortalServiceGrpc.class) {
        if ((getResetBotMethod = PortalServiceGrpc.getResetBotMethod) == null) {
          PortalServiceGrpc.getResetBotMethod = getResetBotMethod =
              io.grpc.MethodDescriptor.<com.zidong.rpc.service.ResetBotRequest, com.zidong.rpc.service.ResetBotResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ResetBot"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.zidong.rpc.service.ResetBotRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.zidong.rpc.service.ResetBotResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PortalServiceMethodDescriptorSupplier("ResetBot"))
              .build();
        }
      }
    }
    return getResetBotMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PortalServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PortalServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PortalServiceStub>() {
        @java.lang.Override
        public PortalServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PortalServiceStub(channel, callOptions);
        }
      };
    return PortalServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static PortalServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PortalServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PortalServiceBlockingV2Stub>() {
        @java.lang.Override
        public PortalServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PortalServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return PortalServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PortalServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PortalServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PortalServiceBlockingStub>() {
        @java.lang.Override
        public PortalServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PortalServiceBlockingStub(channel, callOptions);
        }
      };
    return PortalServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PortalServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PortalServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PortalServiceFutureStub>() {
        @java.lang.Override
        public PortalServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PortalServiceFutureStub(channel, callOptions);
        }
      };
    return PortalServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   **
   * 门户对外 RPC 服务。
   * 事件语义与 SSE 保持一致：event 取值 status / message / done / error，
   * data 为该事件对应的文本内容，便于调用方原样转发到前端。
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     **
     * 提示词调试：服务端流式返回一次问答的调试过程。
     * 依次推送 status（阶段提示）、message（累积到当前的完整文本）、done（最终答复）；
     * 过程中出错返回 error 事件，或直接以 gRPC 状态码结束。
     * </pre>
     */
    default void debugPrompt(com.zidong.rpc.service.DebugPromptRequest request,
        io.grpc.stub.StreamObserver<com.zidong.rpc.service.DebugPromptEvent> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDebugPromptMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * 查询平台已注册的 Agent 元数据（供管理台做 Agent 列表/绑定选择）。
     * 只返回展示需要的字段：名称、描述、goal 与 action 的名称描述；
     * 提示词、模型参数、黑板绑定等内部配置一律不返回。
     * </pre>
     */
    default void listAgents(com.zidong.rpc.service.ListAgentsRequest request,
        io.grpc.stub.StreamObserver<com.zidong.rpc.service.ListAgentsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListAgentsMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * 绑定机器人：由主节点生成登录二维码。
     * 只有主节点能扫码（重复扫码会让其它实例的 token 失效），非主节点返回
     * error_code=NOT_LEADER，调用方据此提示或重试到主节点。
     * </pre>
     */
    default void createBotQrcode(com.zidong.rpc.service.CreateBotQrcodeRequest request,
        io.grpc.stub.StreamObserver<com.zidong.rpc.service.CreateBotQrcodeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateBotQrcodeMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * 查询扫码绑定状态：任意实例都可调用，状态由主节点在进程内维护。
     * 同时返回当前是否已登录，已登录时附带 ilink_bot_id / ilink_user_id。
     * </pre>
     */
    default void getBotQrcodeStatus(com.zidong.rpc.service.GetBotQrcodeStatusRequest request,
        io.grpc.stub.StreamObserver<com.zidong.rpc.service.GetBotQrcodeStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetBotQrcodeStatusMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * 解绑机器人：清除共享存储中的登录凭证，并把处理中的消息任务重置为待处理。
     * 只有主节点能执行，非主节点返回 error_code=NOT_LEADER。
     * </pre>
     */
    default void resetBot(com.zidong.rpc.service.ResetBotRequest request,
        io.grpc.stub.StreamObserver<com.zidong.rpc.service.ResetBotResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getResetBotMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service PortalService.
   * <pre>
   **
   * 门户对外 RPC 服务。
   * 事件语义与 SSE 保持一致：event 取值 status / message / done / error，
   * data 为该事件对应的文本内容，便于调用方原样转发到前端。
   * </pre>
   */
  public static abstract class PortalServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return PortalServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service PortalService.
   * <pre>
   **
   * 门户对外 RPC 服务。
   * 事件语义与 SSE 保持一致：event 取值 status / message / done / error，
   * data 为该事件对应的文本内容，便于调用方原样转发到前端。
   * </pre>
   */
  public static final class PortalServiceStub
      extends io.grpc.stub.AbstractAsyncStub<PortalServiceStub> {
    private PortalServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PortalServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PortalServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     **
     * 提示词调试：服务端流式返回一次问答的调试过程。
     * 依次推送 status（阶段提示）、message（累积到当前的完整文本）、done（最终答复）；
     * 过程中出错返回 error 事件，或直接以 gRPC 状态码结束。
     * </pre>
     */
    public void debugPrompt(com.zidong.rpc.service.DebugPromptRequest request,
        io.grpc.stub.StreamObserver<com.zidong.rpc.service.DebugPromptEvent> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getDebugPromptMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * 查询平台已注册的 Agent 元数据（供管理台做 Agent 列表/绑定选择）。
     * 只返回展示需要的字段：名称、描述、goal 与 action 的名称描述；
     * 提示词、模型参数、黑板绑定等内部配置一律不返回。
     * </pre>
     */
    public void listAgents(com.zidong.rpc.service.ListAgentsRequest request,
        io.grpc.stub.StreamObserver<com.zidong.rpc.service.ListAgentsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListAgentsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * 绑定机器人：由主节点生成登录二维码。
     * 只有主节点能扫码（重复扫码会让其它实例的 token 失效），非主节点返回
     * error_code=NOT_LEADER，调用方据此提示或重试到主节点。
     * </pre>
     */
    public void createBotQrcode(com.zidong.rpc.service.CreateBotQrcodeRequest request,
        io.grpc.stub.StreamObserver<com.zidong.rpc.service.CreateBotQrcodeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateBotQrcodeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * 查询扫码绑定状态：任意实例都可调用，状态由主节点在进程内维护。
     * 同时返回当前是否已登录，已登录时附带 ilink_bot_id / ilink_user_id。
     * </pre>
     */
    public void getBotQrcodeStatus(com.zidong.rpc.service.GetBotQrcodeStatusRequest request,
        io.grpc.stub.StreamObserver<com.zidong.rpc.service.GetBotQrcodeStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetBotQrcodeStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     **
     * 解绑机器人：清除共享存储中的登录凭证，并把处理中的消息任务重置为待处理。
     * 只有主节点能执行，非主节点返回 error_code=NOT_LEADER。
     * </pre>
     */
    public void resetBot(com.zidong.rpc.service.ResetBotRequest request,
        io.grpc.stub.StreamObserver<com.zidong.rpc.service.ResetBotResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getResetBotMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service PortalService.
   * <pre>
   **
   * 门户对外 RPC 服务。
   * 事件语义与 SSE 保持一致：event 取值 status / message / done / error，
   * data 为该事件对应的文本内容，便于调用方原样转发到前端。
   * </pre>
   */
  public static final class PortalServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<PortalServiceBlockingV2Stub> {
    private PortalServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PortalServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PortalServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     **
     * 提示词调试：服务端流式返回一次问答的调试过程。
     * 依次推送 status（阶段提示）、message（累积到当前的完整文本）、done（最终答复）；
     * 过程中出错返回 error 事件，或直接以 gRPC 状态码结束。
     * </pre>
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, com.zidong.rpc.service.DebugPromptEvent>
        debugPrompt(com.zidong.rpc.service.DebugPromptRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getDebugPromptMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * 查询平台已注册的 Agent 元数据（供管理台做 Agent 列表/绑定选择）。
     * 只返回展示需要的字段：名称、描述、goal 与 action 的名称描述；
     * 提示词、模型参数、黑板绑定等内部配置一律不返回。
     * </pre>
     */
    public com.zidong.rpc.service.ListAgentsResponse listAgents(com.zidong.rpc.service.ListAgentsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListAgentsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * 绑定机器人：由主节点生成登录二维码。
     * 只有主节点能扫码（重复扫码会让其它实例的 token 失效），非主节点返回
     * error_code=NOT_LEADER，调用方据此提示或重试到主节点。
     * </pre>
     */
    public com.zidong.rpc.service.CreateBotQrcodeResponse createBotQrcode(com.zidong.rpc.service.CreateBotQrcodeRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateBotQrcodeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * 查询扫码绑定状态：任意实例都可调用，状态由主节点在进程内维护。
     * 同时返回当前是否已登录，已登录时附带 ilink_bot_id / ilink_user_id。
     * </pre>
     */
    public com.zidong.rpc.service.GetBotQrcodeStatusResponse getBotQrcodeStatus(com.zidong.rpc.service.GetBotQrcodeStatusRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetBotQrcodeStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * 解绑机器人：清除共享存储中的登录凭证，并把处理中的消息任务重置为待处理。
     * 只有主节点能执行，非主节点返回 error_code=NOT_LEADER。
     * </pre>
     */
    public com.zidong.rpc.service.ResetBotResponse resetBot(com.zidong.rpc.service.ResetBotRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getResetBotMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service PortalService.
   * <pre>
   **
   * 门户对外 RPC 服务。
   * 事件语义与 SSE 保持一致：event 取值 status / message / done / error，
   * data 为该事件对应的文本内容，便于调用方原样转发到前端。
   * </pre>
   */
  public static final class PortalServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<PortalServiceBlockingStub> {
    private PortalServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PortalServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PortalServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     **
     * 提示词调试：服务端流式返回一次问答的调试过程。
     * 依次推送 status（阶段提示）、message（累积到当前的完整文本）、done（最终答复）；
     * 过程中出错返回 error 事件，或直接以 gRPC 状态码结束。
     * </pre>
     */
    public java.util.Iterator<com.zidong.rpc.service.DebugPromptEvent> debugPrompt(
        com.zidong.rpc.service.DebugPromptRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getDebugPromptMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * 查询平台已注册的 Agent 元数据（供管理台做 Agent 列表/绑定选择）。
     * 只返回展示需要的字段：名称、描述、goal 与 action 的名称描述；
     * 提示词、模型参数、黑板绑定等内部配置一律不返回。
     * </pre>
     */
    public com.zidong.rpc.service.ListAgentsResponse listAgents(com.zidong.rpc.service.ListAgentsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListAgentsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * 绑定机器人：由主节点生成登录二维码。
     * 只有主节点能扫码（重复扫码会让其它实例的 token 失效），非主节点返回
     * error_code=NOT_LEADER，调用方据此提示或重试到主节点。
     * </pre>
     */
    public com.zidong.rpc.service.CreateBotQrcodeResponse createBotQrcode(com.zidong.rpc.service.CreateBotQrcodeRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateBotQrcodeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * 查询扫码绑定状态：任意实例都可调用，状态由主节点在进程内维护。
     * 同时返回当前是否已登录，已登录时附带 ilink_bot_id / ilink_user_id。
     * </pre>
     */
    public com.zidong.rpc.service.GetBotQrcodeStatusResponse getBotQrcodeStatus(com.zidong.rpc.service.GetBotQrcodeStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetBotQrcodeStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     **
     * 解绑机器人：清除共享存储中的登录凭证，并把处理中的消息任务重置为待处理。
     * 只有主节点能执行，非主节点返回 error_code=NOT_LEADER。
     * </pre>
     */
    public com.zidong.rpc.service.ResetBotResponse resetBot(com.zidong.rpc.service.ResetBotRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getResetBotMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service PortalService.
   * <pre>
   **
   * 门户对外 RPC 服务。
   * 事件语义与 SSE 保持一致：event 取值 status / message / done / error，
   * data 为该事件对应的文本内容，便于调用方原样转发到前端。
   * </pre>
   */
  public static final class PortalServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<PortalServiceFutureStub> {
    private PortalServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PortalServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PortalServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     **
     * 查询平台已注册的 Agent 元数据（供管理台做 Agent 列表/绑定选择）。
     * 只返回展示需要的字段：名称、描述、goal 与 action 的名称描述；
     * 提示词、模型参数、黑板绑定等内部配置一律不返回。
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.zidong.rpc.service.ListAgentsResponse> listAgents(
        com.zidong.rpc.service.ListAgentsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListAgentsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * 绑定机器人：由主节点生成登录二维码。
     * 只有主节点能扫码（重复扫码会让其它实例的 token 失效），非主节点返回
     * error_code=NOT_LEADER，调用方据此提示或重试到主节点。
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.zidong.rpc.service.CreateBotQrcodeResponse> createBotQrcode(
        com.zidong.rpc.service.CreateBotQrcodeRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateBotQrcodeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * 查询扫码绑定状态：任意实例都可调用，状态由主节点在进程内维护。
     * 同时返回当前是否已登录，已登录时附带 ilink_bot_id / ilink_user_id。
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.zidong.rpc.service.GetBotQrcodeStatusResponse> getBotQrcodeStatus(
        com.zidong.rpc.service.GetBotQrcodeStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetBotQrcodeStatusMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     **
     * 解绑机器人：清除共享存储中的登录凭证，并把处理中的消息任务重置为待处理。
     * 只有主节点能执行，非主节点返回 error_code=NOT_LEADER。
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.zidong.rpc.service.ResetBotResponse> resetBot(
        com.zidong.rpc.service.ResetBotRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getResetBotMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_DEBUG_PROMPT = 0;
  private static final int METHODID_LIST_AGENTS = 1;
  private static final int METHODID_CREATE_BOT_QRCODE = 2;
  private static final int METHODID_GET_BOT_QRCODE_STATUS = 3;
  private static final int METHODID_RESET_BOT = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_DEBUG_PROMPT:
          serviceImpl.debugPrompt((com.zidong.rpc.service.DebugPromptRequest) request,
              (io.grpc.stub.StreamObserver<com.zidong.rpc.service.DebugPromptEvent>) responseObserver);
          break;
        case METHODID_LIST_AGENTS:
          serviceImpl.listAgents((com.zidong.rpc.service.ListAgentsRequest) request,
              (io.grpc.stub.StreamObserver<com.zidong.rpc.service.ListAgentsResponse>) responseObserver);
          break;
        case METHODID_CREATE_BOT_QRCODE:
          serviceImpl.createBotQrcode((com.zidong.rpc.service.CreateBotQrcodeRequest) request,
              (io.grpc.stub.StreamObserver<com.zidong.rpc.service.CreateBotQrcodeResponse>) responseObserver);
          break;
        case METHODID_GET_BOT_QRCODE_STATUS:
          serviceImpl.getBotQrcodeStatus((com.zidong.rpc.service.GetBotQrcodeStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.zidong.rpc.service.GetBotQrcodeStatusResponse>) responseObserver);
          break;
        case METHODID_RESET_BOT:
          serviceImpl.resetBot((com.zidong.rpc.service.ResetBotRequest) request,
              (io.grpc.stub.StreamObserver<com.zidong.rpc.service.ResetBotResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getDebugPromptMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.zidong.rpc.service.DebugPromptRequest,
              com.zidong.rpc.service.DebugPromptEvent>(
                service, METHODID_DEBUG_PROMPT)))
        .addMethod(
          getListAgentsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.zidong.rpc.service.ListAgentsRequest,
              com.zidong.rpc.service.ListAgentsResponse>(
                service, METHODID_LIST_AGENTS)))
        .addMethod(
          getCreateBotQrcodeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.zidong.rpc.service.CreateBotQrcodeRequest,
              com.zidong.rpc.service.CreateBotQrcodeResponse>(
                service, METHODID_CREATE_BOT_QRCODE)))
        .addMethod(
          getGetBotQrcodeStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.zidong.rpc.service.GetBotQrcodeStatusRequest,
              com.zidong.rpc.service.GetBotQrcodeStatusResponse>(
                service, METHODID_GET_BOT_QRCODE_STATUS)))
        .addMethod(
          getResetBotMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.zidong.rpc.service.ResetBotRequest,
              com.zidong.rpc.service.ResetBotResponse>(
                service, METHODID_RESET_BOT)))
        .build();
  }

  private static abstract class PortalServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PortalServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.zidong.rpc.service.PortalServiceOuterClass.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PortalService");
    }
  }

  private static final class PortalServiceFileDescriptorSupplier
      extends PortalServiceBaseDescriptorSupplier {
    PortalServiceFileDescriptorSupplier() {}
  }

  private static final class PortalServiceMethodDescriptorSupplier
      extends PortalServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    PortalServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (PortalServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PortalServiceFileDescriptorSupplier())
              .addMethod(getDebugPromptMethod())
              .addMethod(getListAgentsMethod())
              .addMethod(getCreateBotQrcodeMethod())
              .addMethod(getGetBotQrcodeStatusMethod())
              .addMethod(getResetBotMethod())
              .build();
        }
      }
    }
    return result;
  }
}
