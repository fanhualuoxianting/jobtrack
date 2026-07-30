package com.fanhua.jobtrack.security;

import com.fanhua.jobtrack.common.enums.ErrorCode;
import com.fanhua.jobtrack.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具：业务代码不接受前端传入 userId，统一从此处获取
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 获取当前登录用户；未登录时抛出 401 */
    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new UnauthorizedException(
                    ErrorCode.AUTH_UNAUTHORIZED.getCode(),
                    ErrorCode.AUTH_UNAUTHORIZED.getDefaultMessage());
        }
        return loginUser;
    }

    /** 当前用户 ID */
    public static Long currentUserId() {
        return currentUser().getUserId();
    }
}
