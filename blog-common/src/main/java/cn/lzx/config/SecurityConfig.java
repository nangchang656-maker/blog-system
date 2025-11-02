package cn.lzx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import cn.lzx.filter.JwtAuthenticationFilter;
import cn.lzx.handler.AuthenticationEntryPointImpl;
import lombok.RequiredArgsConstructor;

/**
 * Spring Security配置类
 * 配置JWT认证、授权、跨域等
 * 
 * '@EnableMethodSecurity' 启用方法级别的安全控制(支持 @PreAuthorize、@Secured 等注解)
 *
 * @author lzx
 * @since 2025-10-31
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * 公开访问路径(无需认证即可访问)
     */
    private static final String[] PUBLIC_URLS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/doc.html",
            "/doc.html/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/druid/**",
            "/favicon.ico",
            "/webjars/**",
            "/static/**",
            "/css/**",
            "/js/**",
            "/images/**"
    };

    /*
     * JWT 认证过滤器,负责从请求头中提取并验证 JWT Token
     * 验证通过后将用户信息设置到 Spring Security 上下文中
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    /*
     * 认证失败入口处理器,当用户未认证或认证失败时触发
     * 用于返回统一的 401 错误响应(如 JSON 格式错误信息)
     */
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    /*
     * 自定义请求匹配器,用于匹配标注了 @NoLogin 注解的接口
     */
    private final NoLoginRequestMatcher noLoginRequestMatcher;

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（使用JWT不需要CSRF保护）
                .csrf(AbstractHttpConfigurer::disable)

                // 禁用CORS（根据需要可以启用）
                .cors(AbstractHttpConfigurer::disable)

                // 禁用Session（使用JWT无状态认证）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置URL访问权限
                .authorizeHttpRequests(auth -> auth
                        // 放行的接口（登录、注册、公开接口等）
                        .requestMatchers(PUBLIC_URLS)
                        .permitAll()
                        // 放行标注了 @NoLogin 注解的接口
                        .requestMatchers(noLoginRequestMatcher)
                        .permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated())

                // 异常处理
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint))

                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
