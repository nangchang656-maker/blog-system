package cn.lzx.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import cn.lzx.annotation.RateLimit;
import cn.lzx.exception.BusinessException;
import cn.lzx.service.RateLimitService;
import cn.lzx.utils.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 接口限流切面
 * 拦截标注了@RateLimit注解的方法，实现基于Redis的接口限流
 * 
 * @author lzx
 * @since 2025-01-XX
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitService rateLimitService;

    /**
     * 环绕通知：拦截标注了@RateLimit注解的方法
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 这里"@annotation(rateLimit)"中的rateLimit是变量名，和注解类名无关，符合Spring AOP写法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 获取限流参数
        String key = rateLimit.key();
        int count = rateLimit.count();
        int time = rateLimit.time();
        String message = rateLimit.message();
        boolean byUser = rateLimit.byUser();

        // 构建限流key（如果为空，使用方法名）
        if (!StringUtils.hasText(key)) {
            key = signature.getName();
        }

        // 获取限流标识符（用户ID或IP地址）
        String identifier = getIdentifier(byUser);

        // 执行限流检查
        boolean allowed = rateLimitService.tryAcquire(key, identifier, count, time);

        if (!allowed) {
            log.warn("接口限流触发: key={}, identifier={}, count={}, time={}秒",
                    key, identifier, count, time);
            throw new BusinessException(message);
        }

        // 限流检查通过，执行原方法
        return joinPoint.proceed();
    }

    /**
     * 获取限流标识符
     * 
     * @param byUser true-按用户ID限流，false-按IP限流
     * @return 限流标识符
     */
    private String getIdentifier(boolean byUser) {
        if (byUser) {
            // 按用户ID限流
            Long userId = SecurityContextUtil.getCurrentUserId();
            if (userId != null) {
                return "user:" + userId;
            }
            // 如果用户未登录，降级为按IP限流
            log.debug("用户未登录，降级为按IP限流");
        }

        // 按IP限流
        return "ip:" + getClientIpAddress();
    }

    /**
     * 获取客户端IP地址
     * 支持从X-Forwarded-For、X-Real-IP等请求头获取真实IP（适用于反向代理场景）
     * 
     * @return IP地址
     */
    private String getClientIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = null;

        // 1. 尝试从X-Forwarded-For获取（适用于Nginx等反向代理）
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For可能包含多个IP，取第一个
            ip = xForwardedFor.split(",")[0].trim();
        }

        // 2. 尝试从X-Real-IP获取（适用于Nginx等反向代理）
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            String xRealIp = request.getHeader("X-Real-IP");
            if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
                ip = xRealIp;
            }
        }

        // 3. 尝试从Proxy-Client-IP获取
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            String proxyClientIp = request.getHeader("Proxy-Client-IP");
            if (StringUtils.hasText(proxyClientIp) && !"unknown".equalsIgnoreCase(proxyClientIp)) {
                ip = proxyClientIp;
            }
        }

        // 4. 尝试从WL-Proxy-Client-IP获取
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            String wlProxyClientIp = request.getHeader("WL-Proxy-Client-IP");
            if (StringUtils.hasText(wlProxyClientIp) && !"unknown".equalsIgnoreCase(wlProxyClientIp)) {
                ip = wlProxyClientIp;
            }
        }

        // 5. 最后从request.getRemoteAddr()获取
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理IPv6的本地地址
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip != null ? ip : "unknown";
    }
}
