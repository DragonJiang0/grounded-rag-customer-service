package com.zidong.manage.security;

import com.zidong.common.exception.BizException;
import com.zidong.dao.entity.SysLoginSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 当前登录用户工具类 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 当前登录用户，未登录抛业务异常 */
    public static SysLoginSession currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SysLoginSession loginUser)) {
            throw BizException.unauthorized("未登录或登录已过期");
        }
        return loginUser;
    }
}
