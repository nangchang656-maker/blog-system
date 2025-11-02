package cn.lzx.utils;

import org.springframework.security.core.Authentication;

/**
 * 安全上下文工具类
 * 提供获取当前登录用户信息的工具方法
 *
 * @author lzx
 * @since 2025-10-31
 */
public class SecurityContextUtil {

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID，未登录返回null
     */
    public static Long getCurrentUserId() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取认证对象
     *
     * @return Authentication对象
     */
    public static Authentication getAuthentication() {
        return org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();
    }

    /**
     * 检查是否已认证
     *
     * @return true-已认证 false-未认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
