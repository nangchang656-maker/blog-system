package cn.lzx.config;

import cn.lzx.annotation.NoLogin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 自定义请求匹配器 - 用于匹配标注了 @NoLogin 注解的接口
 *
 * @author lzx
 * @since 2025-11-01
 */
@Component
public class NoLoginRequestMatcher implements RequestMatcher {

    private final Set<String> noLoginPaths = new HashSet<>();

    public NoLoginRequestMatcher(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mapping) {
        // 直接注入 RequestMappingHandlerMapping，使用 @Qualifier 指定 bean 名称

        // 获取所有的映射关系
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();

        // 遍历所有的处理方法
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();

            // 检查方法上是否有 @NoLogin 注解
            if (handlerMethod.hasMethodAnnotation(NoLogin.class)) {
                RequestMappingInfo mappingInfo = entry.getKey();

                // 获取该方法的所有路径模式
                if (mappingInfo.getPathPatternsCondition() != null) {
                    mappingInfo.getPathPatternsCondition().getPatterns()
                            .forEach(pattern -> noLoginPaths.add(pattern.getPatternString()));
                }
            }
        }
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        // 精确匹配
        if (noLoginPaths.contains(requestUri)) {
            return true;
        }

        // 支持路径变量匹配 (简单实现)
        for (String path : noLoginPaths) {
            if (pathMatches(path, requestUri)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 简单的路径匹配,支持 {id} 这样的路径变量
     */
    private boolean pathMatches(String pattern, String path) {
        // 将 {xxx} 替换为正则表达式
        String regex = pattern.replaceAll("\\{[^/]+\\}", "[^/]+");
        return path.matches(regex);
    }
}
