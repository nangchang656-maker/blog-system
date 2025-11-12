package cn.lzx.filter;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import cn.lzx.constants.SecurityConstants;
import cn.lzx.service.TokenService;
import cn.lzx.utils.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT认证过滤器
 * 拦截请求，验证JWT Token，设置Security上下文
 *
 * @author lzx
 * @since 2025-10-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 从请求头中获取Token
            String token = extractTokenFromRequest(request);

            // 2. 如果Token存在且有效，设置认证信息
            if (StringUtils.hasText(token) && tokenService.validateAccessToken(token)) {
                // 3. 从Token中获取用户信息（如果token无效会抛出异常，被外层catch捕获）
                Long userId = tokenService.getUserIdFromToken(token);
                String username = tokenService.getUsernameFromToken(token);

                // 4. 创建认证对象
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.emptyList());

                // 5. 设置请求详情
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. 将认证信息设置到Security上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 7. Token自动续期：如果剩余时间少于阈值，返回新Token
                renewTokenIfNeeded(token, userId, username, response);

                log.debug("用户 {} 认证成功", username);
            }
        } catch (Exception e) {
            log.error("JWT认证失败: {}", e.getMessage());
        }

        // 8. 继续过滤链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取Token
     *
     * @param request HTTP请求
     * @return Token字符串
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(SecurityConstants.TOKEN_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * Token自动续期
     * 当Token剩余有效期少于阈值时，自动生成新Token并通过响应头返回
     *
     * @param token    当前Token
     * @param userId   用户ID
     * @param username 用户名
     * @param response HTTP响应
     */
    private void renewTokenIfNeeded(String token, Long userId, String username, HttpServletResponse response) {
        try {
            long remainingTime = jwtTokenUtil.getTokenRemainingTime(token);

            // 如果剩余时间少于续期阈值（默认5分钟），自动续期
            if (remainingTime > 0 && remainingTime < SecurityConstants.TOKEN_RENEW_THRESHOLD) {
                String newToken = jwtTokenUtil.generateAccessToken(userId, username);
                // 在响应头中返回新Token
                response.setHeader(SecurityConstants.NEW_TOKEN_HEADER, newToken);
                log.debug("Token自动续期成功，用户: {}, 剩余时间: {}秒", username, remainingTime);
            }
        } catch (Exception e) {
            log.warn("Token续期失败: {}", e.getMessage());
        }
    }
}
