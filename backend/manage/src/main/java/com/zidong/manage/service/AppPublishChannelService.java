package com.zidong.manage.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.common.exception.BizException;
import com.zidong.dao.dto.AddAccessWindowDto;
import com.zidong.dao.dto.AddApiChannelDto;
import com.zidong.dao.dto.EditAccessWindowDto;
import com.zidong.dao.dto.EditApiChannelDto;
import com.zidong.dao.entity.AppAccessWindow;
import com.zidong.dao.entity.AppApiAccess;
import com.zidong.dao.vo.AccessWindowVO;
import com.zidong.dao.vo.ApiChannelVO;
import com.zidong.dao.mapper.AppAccessWindowMapper;
import com.zidong.dao.mapper.AppApiAccessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/** 应用发布渠道管理 */
@Service
@RequiredArgsConstructor
public class AppPublishChannelService {

    /** API Key 前缀，形如 sk-58lbnj7ayH8s6EgK9OswtyZw2JgtsCkD */
    private static final String API_KEY_PREFIX = "sk-";
    /** API Key 随机部分长度 */
    private static final int API_KEY_SUFFIX_LENGTH = 32;
    /** API Key 生成重试次数：撞上唯一键的概率极低，留几次兜底 */
    private static final int API_KEY_MAX_ATTEMPTS = 5;

    /** 免密窗口唯一标识前缀，形如 wk-58lbnj7ayH8s6EgK9OswtyZw2JgtsCkD */
    private static final String WINDOW_KEY_PREFIX = "wk-";
    /** 免密窗口唯一标识随机部分长度 */
    private static final int WINDOW_KEY_SUFFIX_LENGTH = 32;
    /** 免密窗口唯一标识生成重试次数：撞上唯一键的概率极低，留几次兜底 */
    private static final int WINDOW_KEY_MAX_ATTEMPTS = 5;

    private static final char[] KEY_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppApiAccessMapper apiAccessMapper;
    private final AppAccessWindowMapper accessWindowMapper;
    private final AppService appService;

    /** 创建 API 渠道，返回 API Key */
    @Transactional
    public String createApi(Long appId, AddApiChannelDto request) {
        // 应用不存在直接抛业务异常
        appService.checkExists(appId);
        // 同一应用内渠道名唯一，重复直接抛业务异常
        ensureNameUnique(appId, request.getName().trim(), null);

        AppApiAccess access = request.toSave(appId, nextApiKey());
        apiAccessMapper.insert(access);
        return access.getApiKey();
    }

    /** 编辑 API 渠道 */
    @Transactional
    public void editApi(EditApiChannelDto request) {
        // 记录不存在直接抛业务异常
        AppApiAccess access = checkExists(request.getId());
        // 同一应用内渠道名唯一，校验时排除自己，否则只改过期时间也会被判成重复
        ensureNameUnique(access.getAppId(), request.getName().trim(), request.getId());

        apiAccessMapper.editApi(request);
    }

    /** 删除 API 渠道 */
    @Transactional
    public void deleteApi(Long id) {
        checkExists(id);
        apiAccessMapper.deleteById(id);
    }

    /** API 渠道列表 */
    public List<ApiChannelVO> apiList(Long appId) {
        // 应用不存在直接抛业务异常
        appService.checkExists(appId);
        return apiAccessMapper.selectByAppId(appId).stream()
                .map(ApiChannelVO::of)
                .toList();
    }

    /** 校验接入存在 */
    public AppApiAccess checkExists(Long id) {
        AppApiAccess access = apiAccessMapper.selectById(id);
        if (access == null) {
            throw BizException.notFound("API 渠道不存在：" + id);
        }
        return access;
    }

    /** 创建免密窗口，返回免密窗口唯一标识 */
    @Transactional
    public String createWindow(Long appId, AddAccessWindowDto request) {
        // 应用不存在直接抛业务异常
        appService.checkExists(appId);
        // 同一应用内窗口名唯一，重复直接抛业务异常
        ensureWindowNameUnique(appId, request.getName().trim(), null);

        AppAccessWindow window = request.toSave(appId, nextWindowKey());
        accessWindowMapper.insert(window);
        return window.getWindowKey();
    }

    /** 编辑免密窗口，仅允许修改名称 */
    @Transactional
    public void editWindow(EditAccessWindowDto request) {
        // 记录不存在直接抛业务异常
        AppAccessWindow window = checkWindowExists(request.getId());
        // 同一应用内窗口名唯一，校验时排除自己
        ensureWindowNameUnique(window.getAppId(), request.getName().trim(), request.getId());

        accessWindowMapper.editWindow(request.getId(), request.getName().trim());
    }

    /** 删除免密窗口 */
    @Transactional
    public void deleteWindow(Long id) {
        checkWindowExists(id);
        accessWindowMapper.deleteById(id);
    }

    /** 免密窗口列表 */
    public List<AccessWindowVO> windowList(Long appId) {
        // 应用不存在直接抛业务异常
        appService.checkExists(appId);
        return accessWindowMapper.selectByAppId(appId).stream()
                .map(AccessWindowVO::of)
                .toList();
    }

    /** 校验免密窗口存在 */
    public AppAccessWindow checkWindowExists(Long id) {
        AppAccessWindow window = accessWindowMapper.selectById(id);
        if (window == null) {
            throw BizException.notFound("免密窗口不存在：" + id);
        }
        return window;
    }

    /* ------------------------------------------------------------ 内部方法 */

    /** 校验渠道名在同一应用内唯一 */
    private void ensureNameUnique(Long appId, String name, Long excludeId) {
        Long count = apiAccessMapper.selectCount(Wrappers.<AppApiAccess>lambdaQuery()
                .eq(AppApiAccess::getAppId, appId)
                .eq(AppApiAccess::getName, name)
                .ne(excludeId != null, AppApiAccess::getId, excludeId));
        if (count != null && count > 0) {
            throw BizException.conflict("渠道名称已存在：" + name);
        }
    }

    /** 生成全局唯一的 API Key */
    private String nextApiKey() {
        for (int attempt = 0; attempt < API_KEY_MAX_ATTEMPTS; attempt++) {
            StringBuilder apiKey = new StringBuilder(API_KEY_PREFIX);
            for (int i = 0; i < API_KEY_SUFFIX_LENGTH; i++) {
                apiKey.append(KEY_CHARS[RANDOM.nextInt(KEY_CHARS.length)]);
            }
            String candidate = apiKey.toString();
            Long count = apiAccessMapper.selectCount(Wrappers.<AppApiAccess>lambdaQuery()
                    .eq(AppApiAccess::getApiKey, candidate));
            if (count == null || count == 0) {
                return candidate;
            }
        }
        throw BizException.conflict("API Key 生成失败，请重试");
    }

    /** 校验免密窗口名在同一应用内唯一 */
    private void ensureWindowNameUnique(Long appId, String name, Long excludeId) {
        Long count = accessWindowMapper.selectCount(Wrappers.<AppAccessWindow>lambdaQuery()
                .eq(AppAccessWindow::getAppId, appId)
                .eq(AppAccessWindow::getName, name)
                .ne(excludeId != null, AppAccessWindow::getId, excludeId));
        if (count != null && count > 0) {
            throw BizException.conflict("免密窗口名称已存在：" + name);
        }
    }

    /** 生成全局唯一的免密窗口标识 */
    private String nextWindowKey() {
        for (int attempt = 0; attempt < WINDOW_KEY_MAX_ATTEMPTS; attempt++) {
            StringBuilder windowKey = new StringBuilder(WINDOW_KEY_PREFIX);
            for (int i = 0; i < WINDOW_KEY_SUFFIX_LENGTH; i++) {
                windowKey.append(KEY_CHARS[RANDOM.nextInt(KEY_CHARS.length)]);
            }
            String candidate = windowKey.toString();
            Long count = accessWindowMapper.selectCount(Wrappers.<AppAccessWindow>lambdaQuery()
                    .eq(AppAccessWindow::getWindowKey, candidate));
            if (count == null || count == 0) {
                return candidate;
            }
        }
        throw BizException.conflict("免密窗口标识生成失败，请重试");
    }
}
