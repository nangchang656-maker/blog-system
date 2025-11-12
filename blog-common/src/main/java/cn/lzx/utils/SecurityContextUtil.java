package cn.lzx.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import cn.lzx.constants.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 安全上下文工具类
 * 提供获取当前登录用户信息的工具方法
 *
 * @author lzx
 * @since 2025-10-31
 */
public class SecurityContextUtil {

    /**
     * 私有构造函数，防止实例化工具类
     */
    private SecurityContextUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID,未登录返回null
     */
    public static Long getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取认证对象
     *
     * @return Authentication对象，未认证时返回null
     */
    public static Authentication getAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();
        return context != null ? context.getAuthentication() : null;
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

    /**
     * 从当前请求中获取Token
     *
     * @return Token字符串,不包含Bearer前缀，获取失败返回null
     */
    public static String getToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String bearerToken = request.getHeader(SecurityConstants.TOKEN_HEADER);

        if (!StringUtils.hasText(bearerToken)) {
            return null;
        }

        if (bearerToken.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(SecurityConstants.TOKEN_PREFIX.length());
        }

        return null;
    }
}
