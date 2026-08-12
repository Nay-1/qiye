package com.qiye.security;

import com.qiye.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具
 */
public class SecurityUtils {

    public static LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser lu) {
            return lu;
        }
        throw new BizException(401, "未登录或登录已过期");
    }

    public static Long getUserId() {
        return getLoginUser().getId();
    }

    public static String getRoleCode() {
        return getLoginUser().getRoleCode();
    }

    public static boolean hasRole(String roleCode) {
        return getRoleCode().equals(roleCode);
    }
}
