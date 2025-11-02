package cn.lzx.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import cn.lzx.constants.SecurityConstants;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 负责JWT的生成、解析、验证
 *
 */
@Slf4j
@Component
public class JwtTokenUtil {
    private final SecretKey secretKey = Keys.hmacShaKeyFor(SecurityConstants.JWT_SECRET.getBytes(StandardCharsets.UTF_8));

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
            return null;
        }
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
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
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get(SecurityConstants.JWT_CLAIM_USERNAME, String.class) : null;
    }

    /**
     * 验证Token是否过期
     *
     * @param token JWT Token
     * @return true-已过期 false-未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) {
                return true;
            }
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 验证Token是否有效
     *
     * @param token    JWT Token
     * @param userId   用户ID
     * @param username 用户名
     * @return true-有效 false-无效
     */
    public boolean validateToken(String token, Long userId, String username) {
        try {
            Long tokenUserId = getUserIdFromToken(token);
            String tokenUsername = getUsernameFromToken(token);
            return tokenUserId != null
                    && tokenUserId.equals(userId)
                    && tokenUsername != null
                    && tokenUsername.equals(username)
                    && !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取Token剩余有效时间（秒）
     *
     * @param token JWT Token
     * @return 剩余有效时间（秒），-1表示已过期或无效
     */
    public long getTokenRemainingTime(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) {
                return -1;
            }
            Date expiration = claims.getExpiration();
            long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return remaining > 0 ? remaining : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}
