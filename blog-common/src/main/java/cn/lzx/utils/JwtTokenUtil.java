package cn.lzx.utils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import cn.lzx.constants.SecurityConstants;
import cn.lzx.exception.JwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT工具类
 * 负责JWT的生成、解析、验证
 *
 */
@Slf4j
@Component
public class JwtTokenUtil {
    private final SecretKey secretKey = Keys
            .hmacShaKeyFor(SecurityConstants.JWT_SECRET.getBytes(StandardCharsets.UTF_8));

    /**
     * 生成AccessToken
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return JWT Token
     */
    public String generateAccessToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(SecurityConstants.JWT_CLAIM_USER_ID, userId);
        claims.put(SecurityConstants.JWT_CLAIM_USERNAME, username);
        return createToken(claims, SecurityConstants.ACCESS_TOKEN_EXPIRATION);
    }

    /**
     * 创建Token
     *
     * @param claims     自定义声明
     * @param expiration 过期时间（毫秒）
     * @return JWT Token
     */
    private String createToken(Map<String, Object> claims, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析JWT Token
     *
     * @param token JWT Token
     * @return Claims
     * @throws JwtException 解析失败时抛出异常
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("JWT解析失败: {}", e.getMessage());
            throw new JwtException("Token无效或已过期", e);
        }
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     * @throws JwtException Token无效时抛出异常
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get(SecurityConstants.JWT_CLAIM_USER_ID);
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    /**
     * 从Token中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     * @throws JwtException Token无效时抛出异常
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get(SecurityConstants.JWT_CLAIM_USERNAME, String.class);
    }

    /**
     * 验证Token是否过期
     *
     * @param token JWT Token
     * @return true-已过期 false-未过期
     * @throws JwtException Token无效时抛出异常
     */
    public boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

    /**
     * 获取Token剩余有效时间（秒）
     *
     * @param token JWT Token
     * @return 剩余有效时间（秒），已过期返回0
     * @throws JwtException Token无效时抛出异常
     */
    public long getTokenRemainingTime(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(remaining, 0);
    }
}
