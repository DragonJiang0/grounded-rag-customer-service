package com.zidong.manage.controller;

import com.zidong.dao.dto.AddAccessWindowDto;
import com.zidong.dao.dto.AddApiChannelDto;
import com.zidong.dao.dto.EditAccessWindowDto;
import com.zidong.dao.dto.EditApiChannelDto;
import com.zidong.dao.vo.AccessWindowVO;
import com.zidong.dao.vo.ApiChannelVO;
import com.zidong.manage.service.AppPublishChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 应用发布渠道 */
@RestController
@RequestMapping("/app-publish-channels")
@RequiredArgsConstructor
public class AppPublishChannelController extends BaseController {

    private final AppPublishChannelService publishChannelService;


    /** 创建 API 渠道 */
    @PostMapping("/createApi/{appId}")
    public String createApi(@PathVariable Long appId, @RequestBody AddApiChannelDto request) {
        requestLog(appId, request);
        validate(request);
        return publishChannelService.createApi(appId, request);
    }

    /** 编辑 API 渠道 */
    @PostMapping("/editApi")
    public void editApi(@RequestBody EditApiChannelDto request) {
        requestLog(request);
        validate(request);
        publishChannelService.editApi(request);
    }

    /** API 渠道列表 */
    @GetMapping("/apiList/{appId}")
    public List<ApiChannelVO> apiList(@PathVariable Long appId) {
        requestLog(appId);
        return publishChannelService.apiList(appId);
    }

    /** 删除 API 渠道 */
    @PostMapping("/deleteApi/{id}")
    public void deleteApi(@PathVariable Long id) {
        requestLog(id);
        publishChannelService.deleteApi(id);
    }

    /** 创建免密窗口，只填名称 */
    @PostMapping("/createWindow/{appId}")
    public String createWindow(@PathVariable Long appId, @RequestBody AddAccessWindowDto request) {
        requestLog(appId, request);
        validate(request);
        return publishChannelService.createWindow(appId, request);
    }

    /** 编辑免密窗口，只允许改名称 */
    @PostMapping("/editWindow")
    public void editWindow(@RequestBody EditAccessWindowDto request) {
        requestLog(request);
        validate(request);
        publishChannelService.editWindow(request);
    }

    /** 免密窗口列表 */
    @GetMapping("/windowList/{appId}")
    public List<AccessWindowVO> windowList(@PathVariable Long appId) {
        requestLog(appId);
        return publishChannelService.windowList(appId);
    }

    /** 删除免密窗口 */
    @PostMapping("/deleteWindow/{id}")
    public void deleteWindow(@PathVariable Long id) {
        requestLog(id);
        publishChannelService.deleteWindow(id);
    }
}
