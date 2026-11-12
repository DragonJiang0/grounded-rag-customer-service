package com.zidong.manage.service;

import com.zidong.common.exception.BizException;
import com.zidong.dao.vo.BotQrcodeStatusVO;
import com.zidong.dao.vo.BotQrcodeVO;
import com.zidong.dao.vo.BotResetVO;
import com.zidong.rpc.api.client.starter.service.PortalRpcService;
import com.zidong.rpc.service.CreateBotQrcodeResponse;
import com.zidong.rpc.service.GetBotQrcodeStatusResponse;
import com.zidong.rpc.service.ResetBotResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 机器人绑定 / 解绑：manage 不持有微信客户端，全部经 gRPC 交给 portal 主节点执行。
 *
 * <p>为什么要绕到 portal：扫码与收消息用的是同一个 bot_token，重复扫码会把正在用的
 * token 挤下线；而监听只有主节点在跑。所以绑定必须由主节点完成，成功后该节点立即开始收消息。</p>
 */
@Service
@RequiredArgsConstructor
public class BotBindService {

    private static final Logger log = LoggerFactory.getLogger(BotBindService.class);

    /** 结果码：成功 */
    private static final String RESULT_OK = "OK";

    /** 结果码：调用的 portal 实例不是主节点 */
    private static final String RESULT_NOT_LEADER = "NOT_LEADER";

    private final PortalRpcService portalRpcService;

    /** 生成绑定二维码：由 portal 主节点取码，扫码确认在其后台等待 */
    public BotQrcodeVO createQrcode(Long appId) {
        CreateBotQrcodeResponse response = portalRpcService.createBotQrcode(appId);
        checkResult(response.getErrorCode(), response.getMessage(), response.getLeaderInstanceId());

        BotQrcodeVO vo = new BotQrcodeVO();
        vo.setQrcode(response.getQrcode());
        vo.setQrcodeImgContent(response.getQrcodeImgContent());
        vo.setExpireAtMillis(response.getExpireAtMillis());
        return vo;
    }

    /** 查询扫码状态：页面轮询，任意 portal 实例均可响应 */
    public BotQrcodeStatusVO qrcodeStatus(String qrcode) {
        if (!StringUtils.hasText(qrcode)) {
            throw BizException.badRequest("qrcode 不能为空");
        }
        GetBotQrcodeStatusResponse response = portalRpcService.getBotQrcodeStatus(qrcode);
        checkResult(response.getErrorCode(), response.getMessage(), null);

        BotQrcodeStatusVO vo = new BotQrcodeStatusVO();
        vo.setStatus(response.getStatus());
        vo.setIsLoggedIn(response.getIsLoggedIn());
        vo.setIlinkBotId(response.getIlinkBotId());
        vo.setIlinkUserId(response.getIlinkUserId());
        return vo;
    }

    /** 解绑：清除 portal 共享凭证，并把处理中的消息重置为待处理 */
    public BotResetVO reset() {
        ResetBotResponse response = portalRpcService.resetBot();
        checkResult(response.getErrorCode(), response.getMessage(), response.getLeaderInstanceId());

        BotResetVO vo = new BotResetVO();
        vo.setSuccess(response.getSuccess());
        vo.setResetTasks(response.getResetTasks());
        return vo;
    }

    /**
     * 统一处理 portal 返回的结果码。
     *
     * <p>非主节点用 409 明确表达「换个节点/稍后重试」，
     * 而不是当成 500 让前端以为服务挂了。</p>
     */
    private void checkResult(String errorCode, String message, String leaderInstanceId) {
        if (RESULT_OK.equals(errorCode)) {
            return;
        }
        if (RESULT_NOT_LEADER.equals(errorCode)) {
            String leader = StringUtils.hasText(leaderInstanceId) ? "（当前主节点：" + leaderInstanceId + "）" : "";
            log.warn("机器人绑定请求落到非主节点{}", leader);
            throw BizException.conflict("当前门户实例不是 Bot 主节点" + leader + "，请稍后重试或检查多实例部署配置");
        }
        throw new BizException(StringUtils.hasText(message) ? message : "机器人服务调用失败");
    }
}
