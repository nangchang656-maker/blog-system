package cn.lzx.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import cn.hutool.core.util.IdUtil;
import cn.lzx.constants.SecurityConstants;
import cn.lzx.enums.RedisKeyEnum;
import cn.lzx.utils.JwtTokenUtil;
import cn.lzx.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Token服务类
 * 负责Token的生成、验证、刷新、注销等操作
 *
 * 设计思路：JWT + Redis 混合方案（黑名单增强版）
 *
 * 1. AccessToken：JWT短期认证（15分钟）
 *    - 一般请求：纯JWT验证，不查Redis（高性能）
 *    - 敏感操作：JWT验证 + 黑名单检查（高安全）
 *
 * 2. RefreshToken：Redis存储（7天）
 *    - UUID存储在Redis
 *    - 用于刷新AccessToken
 *    - 注销时删除，阻止生成新Token
 *
 * 3. AccessToken黑名单：Redis Set存储（15分钟）
 *    - 注销/强制下线时加入黑名单
 *    - 敏感操作时检查黑名单
 *    - 自动过期，无需手动清理
 *
 * 应用场景：
 * - validateAccessToken(): 一般业务请求（文章列表、详情等）
 * - validateAccessTokenWithBlacklist(): 敏感操作（注销、修改密码、删除数据等）
 *
 * @author lzx
 * @since 2025-10-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtTokenUtil jwtTokenUtil;
    private final RedisUtil redisService;

    /**
     * 创建Token（AccessToken + RefreshToken）
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return Token信息
     */
    public Map<String, String> createToken(Long userId, String username) {
        // 1. 生成AccessToken（JWT，短期有效）
        String accessToken = jwtTokenUtil.generateAccessToken(userId, username);

        // 2. 生成RefreshToken（UUID，存储在Redis，长期有效）
        String refreshToken = IdUtil.simpleUUID();

        // 3. 将RefreshToken存入Redis
        String refreshTokenKey = RedisKeyEnum.KEY_REFRESH_TOKEN.getKey(userId);
        Map<String, Object> refreshTokenData = new HashMap<>();
        refreshTokenData.put("refreshToken", refreshToken);
        refreshTokenData.put("userId", userId);
        refreshTokenData.put("username", username);
        refreshTokenData.put("createTime", System.currentTimeMillis());

        redisService.set(
                refreshTokenKey,
                refreshTokenData,
                RedisKeyEnum.KEY_REFRESH_TOKEN.getExpire(),
                TimeUnit.SECONDS);

        // 4. 返回Token信息
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", accessToken);
        tokenMap.put("refreshToken", refreshToken);
        tokenMap.put("tokenType", "Bearer");
        tokenMap.put("expiresIn", String.valueOf(SecurityConstants.ACCESS_TOKEN_EXPIRATION / 1000));

        log.info("用户 {} 登录成功，生成Token", username);
        return tokenMap;
    }

    /**
     * 刷新AccessToken
     *
     * @param userId       用户ID
     * @param refreshToken RefreshToken
     * @return 新的Token信息，刷新失败返回null
     */
    public Map<String, String> refreshAccessToken(Long userId, String refreshToken) {
        // 1. 验证RefreshToken
        String refreshTokenKey = RedisKeyEnum.KEY_REFRESH_TOKEN.getKey(userId);
        Object redisValue = redisService.get(refreshTokenKey);

        if (redisValue == null) {
            log.warn("RefreshToken不存在或已过期，用户ID: {}", userId);
            return null;
        }

        // 类型安全检查
        if (!(redisValue instanceof Map)) {
            log.error("Redis数据类型异常，期望Map类型，实际类型: {}", redisValue.getClass().getName());
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> refreshTokenData = (Map<String, Object>) redisValue;

        String storedRefreshToken = (String) refreshTokenData.get("refreshToken");
        if (!refreshToken.equals(storedRefreshToken)) {
            log.warn("RefreshToken不匹配，用户ID: {}", userId);
            return null;
        }

        // 2. 生成新的AccessToken
        String username = (String) refreshTokenData.get("username");
        String newAccessToken = jwtTokenUtil.generateAccessToken(userId, username);

        // 3. 返回新的Token信息
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", newAccessToken);
        tokenMap.put("refreshToken", refreshToken); // RefreshToken不变
        tokenMap.put("tokenType", "Bearer");
        tokenMap.put("expiresIn", String.valueOf(SecurityConstants.ACCESS_TOKEN_EXPIRATION / 1000));

        log.info("用户 {} 刷新AccessToken成功", username);
        return tokenMap;
    }

    /**
     * 注销Token（删除RefreshToken + AccessToken加入黑名单）
     *
     * 设计说明：
     * - 删除RefreshToken，阻止用户刷新生成新的AccessToken
     * - 将当前AccessToken加入黑名单，立即失效
     * - 黑名单有效期15分钟（与AccessToken有效期一致）
     *
     * @param userId 用户ID
     * @param accessToken 当前AccessToken
     */
    public void logout(Long userId, String accessToken) {
        // 1. 删除RefreshToken，阻止刷新AccessToken
        String refreshTokenKey = RedisKeyEnum.KEY_REFRESH_TOKEN.getKey(userId);
        redisService.delete(refreshTokenKey);

        // 2. 将AccessToken加入黑名单
        if (accessToken != null && !accessToken.isEmpty()) {
            String blacklistKey = RedisKeyEnum.KEY_ACCESS_TOKEN_BLACKLIST.getKey(userId);
            long remainingTime = jwtTokenUtil.getTokenRemainingTime(accessToken);
            if (remainingTime > 0) {
                redisService.addToBlacklist(blacklistKey, accessToken, remainingTime, TimeUnit.SECONDS);
                log.info("用户ID {} AccessToken已加入黑名单，剩余时间: {}秒", userId, remainingTime);
            }
        }

        log.info("用户ID {} 注销成功", userId);
    }

    /**
     * 强制用户下线（修改密码、禁用账号等场景）
     *
     * 设计说明：
     * - 删除RefreshToken，阻止用户刷新生成新的AccessToken
     * - 将当前AccessToken加入黑名单，立即失效
     * - 用于敏感操作，确保用户立即无法继续操作
     *
     * @param userId 用户ID
     * @param accessToken 当前AccessToken（可选）
     */
    public void forceLogout(Long userId, String accessToken) {
        // 1. 删除RefreshToken，阻止刷新AccessToken
        String refreshTokenKey = RedisKeyEnum.KEY_REFRESH_TOKEN.getKey(userId);
        redisService.delete(refreshTokenKey);

        // 2. 将AccessToken加入黑名单
        if (accessToken != null && !accessToken.isEmpty()) {
            String blacklistKey = RedisKeyEnum.KEY_ACCESS_TOKEN_BLACKLIST.getKey(userId);
            long remainingTime = jwtTokenUtil.getTokenRemainingTime(accessToken);
            if (remainingTime > 0) {
                redisService.addToBlacklist(blacklistKey, accessToken, remainingTime, TimeUnit.SECONDS);
                log.info("用户ID {} AccessToken已强制加入黑名单，剩余时间: {}秒", userId, remainingTime);
            }
        }

        log.info("用户ID {} 被强制下线", userId);
    }

    /**
     * 验证AccessToken有效性（仅验证JWT签名和过期时间）
     *
     * 设计说明：
     * - 采用纯JWT验证，不查询Redis黑名单，保持无状态特性
     * - AccessToken有效期15分钟，注销后最多15分钟内失效
     * - 通过删除RefreshToken阻止生成新的AccessToken
     * - 适用场景：普通业务请求，追求高性能
     *
     * @param accessToken AccessToken
     * @return true-有效 false-无效
     */
    public boolean validateAccessToken(String accessToken) {
        // 验证JWT签名、过期时间等
        Long userId = jwtTokenUtil.getUserIdFromToken(accessToken);
        String username = jwtTokenUtil.getUsernameFromToken(accessToken);

        if (userId == null || username == null) {
            return false;
        }

        return jwtTokenUtil.validateToken(accessToken, userId, username);
    }

    /**
     * 验证AccessToken有效性（检查黑名单）
     *
     * 设计说明：
     * - 先验证JWT签名和过期时间
     * - 再检查Redis黑名单
     * - 适用场景：敏感操作（注销、修改密码、修改重要信息、删除数据等）
     * - 性能影响：每次需查询Redis，但保证了安全性
     *
     * @param accessToken AccessToken
     * @return true-有效 false-无效（过期或在黑名单中）
     */
    public boolean validateAccessTokenWithBlacklist(String accessToken) {
        // 1. 先验证JWT基本有效性
        if (!validateAccessToken(accessToken)) {
            return false;
        }

        // 2. 检查是否在黑名单中
        Long userId = jwtTokenUtil.getUserIdFromToken(accessToken);
        if (userId == null) {
            return false;
        }

        String blacklistKey = RedisKeyEnum.KEY_ACCESS_TOKEN_BLACKLIST.getKey(userId);
        Boolean isBlacklisted = redisService.isInBlacklist(blacklistKey, accessToken);

        if (Boolean.TRUE.equals(isBlacklisted)) {
            log.warn("AccessToken在黑名单中，用户ID: {}", userId);
            return false;
        }

        return true;
    }

    /**
     * 检查AccessToken是否在黑名单中
     *
     * @param accessToken AccessToken
     * @return true-在黑名单中 false-不在黑名单中
     */
    public boolean isTokenBlacklisted(String accessToken) {
        Long userId = jwtTokenUtil.getUserIdFromToken(accessToken);
        if (userId == null) {
            return false;
        }

        String blacklistKey = RedisKeyEnum.KEY_ACCESS_TOKEN_BLACKLIST.getKey(userId);
        Boolean isBlacklisted = redisService.isInBlacklist(blacklistKey, accessToken);
        return Boolean.TRUE.equals(isBlacklisted);
    }

    /**
     * 从AccessToken中获取用户ID
     *
     * @param accessToken AccessToken
     * @return 用户ID
     */
    public Long getUserIdFromToken(String accessToken) {
        return jwtTokenUtil.getUserIdFromToken(accessToken);
    }

    /**
     * 从AccessToken中获取用户名
     *
     * @param accessToken AccessToken
     * @return 用户名
     */
    public String getUsernameFromToken(String accessToken) {
        return jwtTokenUtil.getUsernameFromToken(accessToken);
    }
}
