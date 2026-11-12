package com.zidong.portal.service;

import com.embabel.agent.core.ActionMetadata;
import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentMetadata;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.Goal;
import com.zidong.common.sse.MessageSink;
import com.zidong.portal.agent.agent.InlinePrompt;
import com.zidong.portal.agent.service.ChatOptions;
import com.zidong.portal.agent.service.InlinePromptAgentService;
import com.zidong.portal.bot.BotLeaderElection;
import com.zidong.portal.bot.BotNotLeaderException;
import com.zidong.portal.bot.BotQrcodeService;
import com.zidong.portal.grpc.GrpcStreamSink;
import com.zidong.rpc.service.ActionInfo;
import com.zidong.rpc.service.AgentInfo;
import com.zidong.rpc.service.CreateBotQrcodeRequest;
import com.zidong.rpc.service.CreateBotQrcodeResponse;
import com.zidong.rpc.service.DebugPromptEvent;
import com.zidong.rpc.service.DebugPromptRequest;
import com.zidong.rpc.service.GetBotQrcodeStatusRequest;
import com.zidong.rpc.service.GetBotQrcodeStatusResponse;
import com.zidong.rpc.service.GoalInfo;
import com.zidong.rpc.service.ListAgentsRequest;
import com.zidong.rpc.service.ListAgentsResponse;
import com.zidong.rpc.service.PortalServiceGrpc;
import com.zidong.rpc.service.ResetBotRequest;
import com.zidong.rpc.service.ResetBotResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class RpcService extends PortalServiceGrpc.PortalServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(RpcService.class);

    /** 机器人绑定接口的结果码：成功 */
    private static final String RESULT_OK = "OK";

    /** 机器人绑定接口的结果码：本实例不是主节点，需要调用方重试到主节点 */
    private static final String RESULT_NOT_LEADER = "NOT_LEADER";

    /** 机器人绑定接口的结果码：服务端内部错误 */
    private static final String RESULT_INTERNAL = "INTERNAL";

    /** 调试渠道只走外部提示词入口，与默认入口的服务分开 */
    private final InlinePromptAgentService inlinePromptAgentService;

    /** Agent 调试入口：按名称执行平台上已注册的任意 Agent */
    private final com.zidong.portal.agent.service.AgentDebugService agentDebugService;

    /** 平台注册表：Agent 列表的唯一数据源，与内置 /api/v1/platform-info/agents 同源 */
    private final AgentPlatform agentPlatform;

    /** 扫码绑定：二维码由主节点生成，绑定成功后由该节点开始收消息 */
    private final BotQrcodeService botQrcodeService;

    /** 主节点身份：用于在响应里告知调用方「该找谁扫码」 */
    private final BotLeaderElection botLeaderElection;

    @Override
    public void debugPrompt(DebugPromptRequest request, StreamObserver<DebugPromptEvent> responseObserver) {

        String question = request.getQuestion();

        // 传输细节收敛在 GrpcStreamSink 里，业务只依赖 MessageSink
        MessageSink sink = new GrpcStreamSink(responseObserver);

        // 提示词随请求带进来，走外部提示词入口；appId 决定在哪个应用的语料范围内命中答案
        InlinePrompt inlinePrompt = new InlinePrompt(request.getPrompt(), request.getAppId());

        // 是否开启记忆：未传（null）按开启处理；显式关闭时本次调用不带历史
        boolean memoryEnabled = !request.hasMemoryEnabled() || request.getMemoryEnabled();

        // 对话参数随请求带进来：optional 字段用 hasXxx() 判断是否传递，未传即回落默认值
        ChatOptions chatOptions = new ChatOptions(
                request.getTaskId(),
                memoryEnabled,
                // 记忆轮数由调用方逐次指定，未传即用 ChatOptions 的默认轮数
                request.hasMemoryRounds() ? request.getMemoryRounds() : ChatOptions.DEFAULT_MEMORY_ROUNDS,
                request.hasTemperature() ? request.getTemperature() : null,
                request.hasTopP() ? request.getTopP() : null,
                // optional 字段必须用 hasXxx() 判断：proto3 未设置时 getXxx() 返回 0，
                // 直接透传会变成 max_tokens=0，模型一个 token 的预算都没有，流式响应为空
                request.hasMaxOutputTokens() ? request.getMaxOutputTokens() : 10240);

        // Agent 调试模式：按名称执行指定 Agent，不走应用语料命中链路；
        // 调试只关心执行结果与过程推送，不落对话记录，因此只传执行必需的四个参数
        if (request.hasAgentName()) {
            try {
                agentDebugService.run(question, inlinePrompt,request.getAgentName(), chatOptions, sink);
            } catch (IllegalArgumentException ex) {
                sink.error(ex.getMessage());
                sink.fail(ex);
            } catch (Exception ex) {
                sink.fail(ex);
            }
            return;
        }

        try {
            // 结果与收尾推送都由 AgentExecutor 统一处理，这里只负责把异常转成 error 事件
            // 调试渠道没有接入实体，对话记录标签固定为 null
            inlinePromptAgentService.run(question, inlinePrompt, chatOptions, sink,
                    ChatHistoryService.CHANNEL_DEBUG, null);
        } catch (Exception ex) {
            sink.fail(ex);
        }
    }

    /**
     * 查询平台已注册的 Agent 元数据。
     *
     * <p>数据源与内置 HTTP 接口 /api/v1/platform-info/agents 完全一致（都是 AgentPlatform.agents()），
     * 但只返回名称、描述、goal 与 action 的名称描述，提示词、模型参数等内部配置不下发。</p>
     */
    @Override
    public void listAgents(ListAgentsRequest request, StreamObserver<ListAgentsResponse> responseObserver) {
        try {
            List<AgentInfo> agents = agentPlatform.agents().stream()
                    .map(RpcService::toAgentInfo)
                    .toList();

            log.debug("查询 Agent 列表: count={}", agents.size());
            responseObserver.onNext(ListAgentsResponse.newBuilder().addAllAgents(agents).build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.error("查询 Agent 列表失败", ex);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("查询 Agent 列表失败: " + ex.getMessage())
                    .withCause(ex)
                    .asRuntimeException());
        }
    }

    /**
     * 生成机器人登录二维码：只有主节点能执行，绑定成功后由该节点立即开始收消息。
     *
     * <p>失败不抛 gRPC 异常，而是用 {@code error_code} 表达：管理台需要区分
     * 「非主节点（可换节点重试）」与「服务端错误（需排查）」。</p>
     */
    @Override
    public void createBotQrcode(CreateBotQrcodeRequest request,
                                StreamObserver<CreateBotQrcodeResponse> responseObserver) {
        try {
            Long appId = request.hasAppId() ? request.getAppId() : null;
            BotQrcodeService.BotQrcode qrcode = botQrcodeService.create(appId);
            responseObserver.onNext(CreateBotQrcodeResponse.newBuilder()
                    .setErrorCode(RESULT_OK)
                    .setQrcode(StringUtils.defaultString(qrcode.qrcode()))
                    .setQrcodeImgContent(StringUtils.defaultString(qrcode.imgContent()))
                    .setExpireAtMillis(qrcode.expireAtMillis())
                    .setLeaderInstanceId(botLeaderElection.instanceId())
                    .build());
            responseObserver.onCompleted();
        } catch (BotNotLeaderException ex) {
            log.warn("创建 Bot 二维码被拒绝: {}, 当前主节点={}", ex.getMessage(), ex.leaderInstanceId());
            responseObserver.onNext(CreateBotQrcodeResponse.newBuilder()
                    .setErrorCode(RESULT_NOT_LEADER)
                    .setMessage(ex.getMessage())
                    .setLeaderInstanceId(StringUtils.defaultString(ex.leaderInstanceId()))
                    .build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.error("创建 Bot 登录二维码失败", ex);
            responseObserver.onNext(CreateBotQrcodeResponse.newBuilder()
                    .setErrorCode(RESULT_INTERNAL)
                    .setMessage("生成二维码失败：" + ex.getMessage())
                    .setLeaderInstanceId(botLeaderElection.instanceId())
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * 查询扫码状态：任意实例都能响应。
     *
     * <p>「是否已登录」读共享存储，不依赖当前实例是否主节点，
     * 因此管理台轮询打到 follower 也能拿到正确的绑定结果。</p>
     */
    @Override
    public void getBotQrcodeStatus(GetBotQrcodeStatusRequest request,
                                   StreamObserver<GetBotQrcodeStatusResponse> responseObserver) {
        try {
            BotQrcodeService.QrcodeStatus status = botQrcodeService.status(request.getQrcode());
            responseObserver.onNext(GetBotQrcodeStatusResponse.newBuilder()
                    .setErrorCode(RESULT_OK)
                    .setStatus(status.status())
                    .setIsLoggedIn(status.isLoggedIn())
                    .setIlinkBotId(StringUtils.defaultString(status.ilinkBotId()))
                    .setIlinkUserId(StringUtils.defaultString(status.ilinkUserId()))
                    .setLeaderInstanceId(StringUtils.defaultString(status.leaderInstanceId()))
                    .build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.error("查询 Bot 扫码状态失败: qrcode={}", request.getQrcode(), ex);
            responseObserver.onNext(GetBotQrcodeStatusResponse.newBuilder()
                    .setErrorCode(RESULT_INTERNAL)
                    .setMessage("查询扫码状态失败：" + ex.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * 解绑机器人：清除共享凭证，并把处理中的消息任务重置为待处理。
     * <p>同样只允许主节点执行，避免解绑与扫码登录并发造成 token 状态错乱。</p>
     */
    @Override
    public void resetBot(ResetBotRequest request, StreamObserver<ResetBotResponse> responseObserver) {
        try {
            int resetTasks = botQrcodeService.reset();
            responseObserver.onNext(ResetBotResponse.newBuilder()
                    .setErrorCode(RESULT_OK)
                    .setSuccess(true)
                    .setResetTasks(resetTasks)
                    .setLeaderInstanceId(botLeaderElection.instanceId())
                    .build());
            responseObserver.onCompleted();
        } catch (BotNotLeaderException ex) {
            log.warn("解绑 Bot 被拒绝: {}, 当前主节点={}", ex.getMessage(), ex.leaderInstanceId());
            responseObserver.onNext(ResetBotResponse.newBuilder()
                    .setErrorCode(RESULT_NOT_LEADER)
                    .setMessage(ex.getMessage())
                    .setLeaderInstanceId(StringUtils.defaultString(ex.leaderInstanceId()))
                    .build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.error("解绑 Bot 失败", ex);
            responseObserver.onNext(ResetBotResponse.newBuilder()
                    .setErrorCode(RESULT_INTERNAL)
                    .setMessage("解绑失败：" + ex.getMessage())
                    .setLeaderInstanceId(botLeaderElection.instanceId())
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * 把框架的 Agent 转成 RPC 消息。
     *
     * <p>用 AgentMetadata 而不是直接序列化 Agent：Agent 上的 @JsonSerialize 约定只允许元数据外传，
     * 且元数据里已经过滤掉了 actions 的实现细节。</p>
     */
    private static AgentInfo toAgentInfo(Agent agent) {
        AgentMetadata metadata = new AgentMetadata(agent);

        AgentInfo.Builder builder = AgentInfo.newBuilder()
                .setName(StringUtils.defaultString(metadata.getName()))
                .setDescription(StringUtils.defaultString(metadata.getDescription()));

        for (Goal goal : metadata.getGoals()) {
            builder.addGoals(GoalInfo.newBuilder()
                    .setName(StringUtils.defaultString(goal.getName()))
                    .setDescription(StringUtils.defaultString(goal.getDescription())));
        }

        for (ActionMetadata action : metadata.getActions()) {
            builder.addActions(ActionInfo.newBuilder()
                    .setName(StringUtils.defaultString(action.getName()))
                    .setDescription(StringUtils.defaultString(action.getDescription())));
        }

        return builder.build();
    }

}
