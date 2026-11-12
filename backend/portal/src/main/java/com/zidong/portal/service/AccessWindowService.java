package com.zidong.portal.service;

import com.zidong.common.exception.BizException;
import com.zidong.dao.entity.AppAccessWindow;
import com.zidong.dao.mapper.AppAccessWindowMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 免密窗口的业务入口：所有免密入口都先经过这里，把 window_key 换成窗口信息。
 *
 * <p>免密入口不经过任何认证过滤器（走 portal 的兜底放行链），因此 {@code window_key}
 * 本身就是凭证，校验一律收敛在本类：存在 + 未过期，任一不满足直接抛业务异常。
 */
@Service
@RequiredArgsConstructor
public class AccessWindowService {

    private final AppAccessWindowMapper appAccessWindowMapper;

    /**
     * 按唯一标识校验并返回免密窗口，同时刷新最后使用时间。
     *
     * @param windowKey 免密窗口唯一标识
     * @return 免密窗口信息（最后使用时间已刷新为当前时间）
     * @throws BizException 标识为空、窗口不存在或已过期时抛出
     */
    public AppAccessWindow requireByWindowKey(String windowKey) {
        if (StringUtils.isBlank(windowKey)) {
            throw BizException.badRequest("windowKey 不能为空");
        }
        AppAccessWindow window = appAccessWindowMapper.findByWindowKey(windowKey);
        if (window == null) {
            throw BizException.notFound("免密窗口不存在");
        }

        // 打开窗口即视为一次使用，刷新最后使用时间
        LocalDateTime now = LocalDateTime.now();
        appAccessWindowMapper.touchLastUsed(windowKey);
        window.setLastUsedAt(now);
        return window;
    }
}
