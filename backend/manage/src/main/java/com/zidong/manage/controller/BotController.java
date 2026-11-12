package com.zidong.manage.controller;

import com.zidong.dao.dto.BotQrcodeStatusQueryDto;
import com.zidong.dao.dto.CreateBotQrcodeQueryDto;
import com.zidong.dao.vo.BotQrcodeStatusVO;
import com.zidong.dao.vo.BotQrcodeVO;
import com.zidong.dao.vo.BotResetVO;
import com.zidong.manage.service.BotBindService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 企业微信机器人绑定：manage 只做转发，扫码与监听都在 portal 主节点 */
@RestController
@RequestMapping("/bot")
@RequiredArgsConstructor
public class BotController extends BaseController {

    private final BotBindService botBindService;

    /** 新增/绑定机器人：生成登录二维码，body 带要绑定的应用 appId */
    @PostMapping("/qrcode")
    public BotQrcodeVO createQrcode(@RequestBody CreateBotQrcodeQueryDto request) {
        requestLog(request);
        validate(request);
        return botBindService.createQrcode(request.getAppId());
    }

    /** 轮询扫码状态（前端 1~2s 一次，body 传 qrcode） */
    @PostMapping("/qrcode/status")
    public BotQrcodeStatusVO qrcodeStatus(@RequestBody BotQrcodeStatusQueryDto request) {
        requestLog(request);
        validate(request);
        return botBindService.qrcodeStatus(request.getQrcode());
    }

    /** 解绑机器人 */
    @PostMapping("/reset")
    public BotResetVO reset() {
        requestLog();
        return botBindService.reset();
    }
}
