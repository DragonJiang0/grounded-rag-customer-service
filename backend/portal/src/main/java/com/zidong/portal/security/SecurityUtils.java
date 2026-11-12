package com.zidong.portal.security;

import com.zidong.common.exception.BizException;
import com.zidong.dao.entity.AppAccessWindow;
import com.zidong.dao.entity.AppApiAccess;
import com.zidong.dao.entity.AppPasswordWindow;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前请求安全上下文工具类。
 * <p>认证通过后，{@code ApiFilter} 会把 {@link AppApiAccess} 作为 Authentication 的 principal
 * 放入 SecurityContextHolder；业务代码通过本工具类即可直接获取，无需再解析令牌。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前请求的接入信息，未认证时抛业务异常。
     *
     * @return 当前请求对应的应用 API 接入信息
     * @throws BizException 未携带或未通过认证时抛出
     */
    public static AppApiAccess currentAppApiAccess() {
        AppApiAccess appApiAccess = currentAppApiAccessOrNull();
        if (appApiAccess == null) {
            throw BizException.unauthorized("未认证或认证已过期");
        }
        return appApiAccess;
    }


    /**
     * 获取当前请求的免密窗口信息，未认证时抛业务异常。
     *
     * @return 当前请求对应的免密窗口信息
     * @throws BizException 未携带或未通过认证时抛出
     */
    public static AppAccessWindow currentAppAccessWindow() {
        AppAccessWindow appAccessWindow = currentAppAccessWindowOrNull();
        if (appAccessWindow == null) {
            throw BizException.unauthorized("未认证或认证已过期");
        }
        return appAccessWindow;
    }

    /**
     * 获取当前请求的接入信息，未认证时返回 null。
     *
     * @return 当前请求对应的应用 API 接入信息，未认证返回 null
     */
    public static AppApiAccess currentAppApiAccessOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppApiAccess appApiAccess)) {
            return null;
        }
        return appApiAccess;
    }

    /**
     * 获取当前请求的免密窗口信息，未认证时返回 null。
     *
     * @return 当前请求对应的免密窗口信息，未认证返回 null
     */
    public static AppAccessWindow currentAppAccessWindowOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppAccessWindow appAccessWindow)) {
            return null;
        }
        return appAccessWindow;
    }

    /**
     * 获取当前请求的账号密码登录窗口信息，未认证时抛业务异常。
     *
     * @return 当前请求对应的账号密码登录窗口信息
     * @throws BizException 未携带或未通过认证时抛出
     */
    public static AppPasswordWindow currentAppPasswordWindow() {
        AppPasswordWindow appPasswordWindow = currentAppPasswordWindowOrNull();
        if (appPasswordWindow == null) {
            throw BizException.unauthorized("未认证或认证已过期");
        }
        return appPasswordWindow;
    }

    /**
     * 获取当前请求的账号密码登录窗口信息，未认证时返回 null。
     *
     * @return 当前请求对应的账号密码登录窗口信息，未认证返回 null
     */
    public static AppPasswordWindow currentAppPasswordWindowOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppPasswordWindow appPasswordWindow)) {
            return null;
        }
        return appPasswordWindow;
    }

}
