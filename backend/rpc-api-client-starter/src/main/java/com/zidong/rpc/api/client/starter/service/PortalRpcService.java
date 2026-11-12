package com.zidong.rpc.api.client.starter.service;

import com.zidong.common.enums.RpcServerEnum;
import com.zidong.rpc.api.client.starter.config.RpcClientConfig;
import com.zidong.rpc.service.AgentInfo;
import com.zidong.rpc.service.CreateBotQrcodeRequest;
import com.zidong.rpc.service.CreateBotQrcodeResponse;
import com.zidong.rpc.service.DebugPromptEvent;
import com.zidong.rpc.service.DebugPromptRequest;
import com.zidong.rpc.service.GetBotQrcodeStatusRequest;
import com.zidong.rpc.service.GetBotQrcodeStatusResponse;
import com.zidong.rpc.service.ListAgentsRequest;
import com.zidong.rpc.service.PortalServiceGrpc;
import com.zidong.rpc.service.ResetBotRequest;
import com.zidong.rpc.service.ResetBotResponse;
import io.grpc.ManagedChannel;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * portal 服务的 gRPC 客户端封装。
 *
 * @Author ZiDong
 * @create 2025 2025/9/22 16:02
 **/
@Service
public class PortalRpcService {

    /** 绑定 / 解绑是短调用（一次取码或一次清凭证），不用调试流的超时 */
    private static final long BOT_BIND_DEADLINE_SECONDS = 30L;

    /** 状态查询更短：页面高频轮询，宁可快速失败让前端重试 */
    private static final long BOT_STATUS_DEADLINE_SECONDS = 10L;

    private final RpcClientConfig rpcClientConfig;

    public PortalRpcService(RpcClientConfig rpcClientConfig) {
        this.rpcClientConfig = rpcClientConfig;
    }



    public Iterator<DebugPromptEvent> debugPrompt(DebugPromptRequest request) {

        ManagedChannel channel = rpcClientConfig.getChannel(RpcServerEnum.PORTAL.getName());


        PortalServiceGrpc.PortalServiceBlockingStub stub = PortalServiceGrpc.newBlockingStub(channel);

        // 服务端流是长调用：deadline 覆盖整条流（含大模型调用与逐块输出），
        // 用 rpc.client.deadline-seconds 配置，别再写死 10 秒
        long deadlineSeconds = rpcClientConfig.getDeadlineSeconds(RpcServerEnum.PORTAL.getName());

        return stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .debugPrompt(request);
    }

    /**
     * 查询 portal 上已注册的 Agent 元数据。
     *
     * <p>一元调用，用于管理台的 Agent 列表/绑定选择；返回内容已由服务端裁剪为
     * 名称、描述、goal 与 action 的名称描述，不含提示词等内部配置。</p>
     *
     * @return Agent 列表，按名称排序
     */
    public List<AgentInfo> listAgents() {

        ManagedChannel channel = rpcClientConfig.getChannel(RpcServerEnum.PORTAL.getName());

        PortalServiceGrpc.PortalServiceBlockingStub stub = PortalServiceGrpc.newBlockingStub(channel);

        long deadlineSeconds = rpcClientConfig.getDeadlineSeconds(RpcServerEnum.PORTAL.getName());

        return stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .listAgents(ListAgentsRequest.newBuilder().build())
                .getAgentsList();
    }

    /**
     * 生成机器人登录二维码：portal 主节点调微信接口取码后立即返回，
     * 扫码确认在 portal 侧后台等待，管理台再轮询 {@link #getBotQrcodeStatus(String)}。
     *
     * <p>超时给 30s 足够（一次取码请求），不用全局 240s 的调试超时，
     * 避免页面点绑定后长时间卡住。</p>
     *
     * @param appId 要绑定的应用 ID：扫码后该机器人消息按此应用触发 embabel 回答，可为 null
     */
    public CreateBotQrcodeResponse createBotQrcode(Long appId) {
        ManagedChannel channel = rpcClientConfig.getChannel(RpcServerEnum.PORTAL.getName());
        PortalServiceGrpc.PortalServiceBlockingStub stub = PortalServiceGrpc.newBlockingStub(channel);
        CreateBotQrcodeRequest.Builder request = CreateBotQrcodeRequest.newBuilder();
        if (appId != null) {
            request.setAppId(appId);
        }
        return stub.withDeadlineAfter(BOT_BIND_DEADLINE_SECONDS, TimeUnit.SECONDS)
                .createBotQrcode(request.build());
    }

    /**
     * 查询扫码绑定状态：任意 portal 实例都能响应，管理台按 1~2s 轮询。
     *
     * @param qrcode 创建二维码时返回的令牌
     */
    public GetBotQrcodeStatusResponse getBotQrcodeStatus(String qrcode) {
        ManagedChannel channel = rpcClientConfig.getChannel(RpcServerEnum.PORTAL.getName());
        PortalServiceGrpc.PortalServiceBlockingStub stub = PortalServiceGrpc.newBlockingStub(channel);
        return stub.withDeadlineAfter(BOT_STATUS_DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getBotQrcodeStatus(GetBotQrcodeStatusRequest.newBuilder()
                        .setQrcode(qrcode == null ? "" : qrcode)
                        .build());
    }

    /**
     * 解绑机器人：清除 portal 共享存储里的登录凭证，并重置处理中的消息任务。
     */
    public ResetBotResponse resetBot() {
        ManagedChannel channel = rpcClientConfig.getChannel(RpcServerEnum.PORTAL.getName());
        PortalServiceGrpc.PortalServiceBlockingStub stub = PortalServiceGrpc.newBlockingStub(channel);
        return stub.withDeadlineAfter(BOT_BIND_DEADLINE_SECONDS, TimeUnit.SECONDS)
                .resetBot(ResetBotRequest.newBuilder().build());
    }
}
