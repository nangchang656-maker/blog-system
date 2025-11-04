package cn.lzx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * 跨域配置
 * 支持Windows浏览器访问WSL中的后端服务
 *
 * @author lzx
 * @since 2025-11-03
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源（前端地址）
        // 允许所有来源（开发环境），生产环境应该指定具体域名
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // 允许的请求头
        configuration.setAllowedHeaders(Collections.singletonList("*"));

        // 允许携带Cookie和认证信息
        configuration.setAllowCredentials(true);

        // 暴露的响应头（前端可以访问）
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-New-Token",  // Token自动续期
                "Content-Type"
        ));

        // 预检请求的有效期（秒）
        configuration.setMaxAge(3600L);

        // 应用到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
