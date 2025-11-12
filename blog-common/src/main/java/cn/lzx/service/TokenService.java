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
 * 负责Token的生成、验证、刷新、注销和黑名单管理
 *
 * 设计思路：JWT + Redis 混合方案
 * - AccessToken：JWT短期认证（15分钟），一般请求仅验证JWT，敏感操作需检查黑名单
 * - RefreshToken：Redis存储（7天），用于刷新AccessToken
 * - 黑名单：Redis Set存储，注销/强制下线时加入，自动过期
 *
 * @author lzx
 * @since 2025-10-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtTokenUtil jwtTokenUtil;
    private final RedisUtil redisUtil;

    // ==================== Token创建 ====================

    /**
     * 创建Token（AccessToken + RefreshToken）
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return Token信息
     */
    public Map<String, String> createToken(Long userId, String username) {
        // 生成AccessToken（JWT，15分钟有效）
        String accessToken = jwtTokenUtil.generateAccessToken(userId, username);

        // 生成RefreshToken（UUID，存储在Redis，7天有效）
        String refreshToken = IdUtil.simpleUUID();

        // 存储RefreshToken到Redis
        saveRefreshToken(userId, username, refreshToken);

        log.info("用户 {} 登录成功，生成Token", username);
        return buildTokenResponse(accessToken, refreshToken);
    }

    // ==================== Token刷新 ====================

    /**
     * 刷新AccessToken
     *
     * @param userId       用户ID
     * @param refreshToken RefreshToken
     * @return 新的Token信息，刷新失败返回null
     */
    public Map<String, String> refreshAccessToken(Long userId, String refreshToken) {
        // 验证并获取RefreshToken数据
        Map<String, Object> refreshTokenData = validateAndGetRefreshToken(userId, refreshToken);
        if (refreshTokenData == null) {
            return null;
        }

        // 生成新的AccessToken
        String username = (String) refreshTokenData.get(SecurityConstants.JWT_CLAIM_USERNAME);
        String newAccessToken = jwtTokenUtil.generateAccessToken(userId, username);

        log.info("用户 {} 刷新AccessToken成功", username);
        return buildTokenResponse(newAccessToken, refreshToken);
    }

    // ==================== Token验证 ====================

    /**
     * 验证AccessToken有效性（仅验证JWT签名和过期时间）
     * 适用于一般业务请求，追求高性能
     *
     * @param accessToken AccessToken
     * @return true-有效 false-无效
     */
    public boolean validateAccessToken(String accessToken) {
        try {
            // 解析token会验证签名，如果无效会抛出异常
            jwtTokenUtil.getUserIdFromToken(accessToken);
            // 检查是否过期
            return !jwtTokenUtil.isTokenExpired(accessToken);
        } catch (Exception e) {
            log.debug("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证AccessToken有效性（检查黑名单）
     * 适用于敏感操作（注销、修改密码、删除数据等）
     *
     * @param accessToken AccessToken
     * @return true-有效 false-无效（过期或在黑名单中）
     */
    public boolean validateAccessTokenWithBlacklist(String accessToken) {
        try {
            // 先验证JWT基本有效性
            if (!validateAccessToken(accessToken)) {
                return false;
            }

            // 检查是否在黑名单中
            Long userId = jwtTokenUtil.getUserIdFromToken(accessToken);
            return !isTokenInBlacklist(userId, accessToken);
        } catch (Exception e) {
            log.debug("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    // ==================== Token注销 ====================

    /**
     * 注销Token（删除RefreshToken + AccessToken加入黑名单）
     *
     * @param userId      用户ID
     * @param accessToken 当前AccessToken
     */
    public void logout(Long userId, String accessToken) {
        invalidateUserToken(userId, accessToken);
        log.info("用户ID {} 注销成功", userId);
    }

    /**
     * 强制用户下线（修改密码、禁用账号等场景）
     *
     * @param userId      用户ID
     * @param accessToken 当前AccessToken（可选）
     */
    public void forceLogout(Long userId, String accessToken) {
        invalidateUserToken(userId, accessToken);
        log.info("用户ID {} 被强制下线", userId);
    }

    /**
     * 使Token失效（删除RefreshToken + AccessToken加入黑名单）
     *
     * @param userId      用户ID
     * @param accessToken 当前AccessToken
     */
    private void invalidateUserToken(Long userId, String accessToken) {
        // 删除RefreshToken，阻止刷新AccessToken
        deleteRefreshToken(userId);
        // 将AccessToken加入黑名单
        addTokenToBlacklist(userId, accessToken);
    }

    // ==================== 黑名单管理 ====================

    /**
     * 将AccessToken加入黑名单
     *
     * @param userId      用户ID
     * @param accessToken AccessToken
     */
    private void addTokenToBlacklist(Long userId, String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return;
        }

        try {
            long remainingTime = jwtTokenUtil.getTokenRemainingTime(accessToken);
            if (remainingTime > 0) {
                String blacklistKey = RedisKeyEnum.KEY_ACCESS_TOKEN_BLACKLIST.getKey(userId);
                addToBlacklist(blacklistKey, accessToken, remainingTime, TimeUnit.SECONDS);
                log.info("用户ID {} AccessToken已加入黑名单，剩余时间: {}秒", userId, remainingTime);
            }
        } catch (Exception e) {
            log.warn("Token无效，无法加入黑名单，用户ID: {}", userId);
        }
    }

    /**
     * 将Token加入黑名单
     *
     * @param key     Redis键
     * @param token   Token值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    private void addToBlacklist(String key, String token, long timeout, TimeUnit unit) {
        redisUtil.sAdd(key, token);
        redisUtil.expire(key, timeout, unit);
    }

    /**
     * 检查AccessToken是否在黑名单中
     *
     * @param userId      用户ID
     * @param accessToken AccessToken
     * @return true-在黑名单中 false-不在黑名单中
     */
    private boolean isTokenInBlacklist(Long userId, String accessToken) {
        String blacklistKey = RedisKeyEnum.KEY_ACCESS_TOKEN_BLACKLIST.getKey(userId);
        Boolean result = redisUtil.sIsMember(blacklistKey, accessToken);
        if (Boolean.TRUE.equals(result)) {
            log.warn("AccessToken在黑名单中，用户ID: {}", userId);
            return true;
        }
        return false;
    }

    /**
     * 从黑名单中移除Token
     *
     * @param key   Redis键
     * @param token Token值
     * @return 移除的元素数量
     */
    public Long removeFromBlacklist(String key, String token) {
        return redisUtil.sRemove(key, token);
    }

    // ==================== 工具方法 ====================

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

    // ==================== 私有辅助方法 ====================

    /**
     * 构建Token响应结果
     *
     * @param accessToken  AccessToken
     * @param refreshToken RefreshToken
     * @return Token响应Map
     */
    private Map<String, String> buildTokenResponse(String accessToken, String refreshToken) {
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", accessToken);
        tokenMap.put("refreshToken", refreshToken);
        tokenMap.put("tokenType", SecurityConstants.TOKEN_TYPE);
        tokenMap.put("expiresIn", String.valueOf(SecurityConstants.ACCESS_TOKEN_EXPIRATION / 1000));
        return tokenMap;
    }

    /**
     * 保存RefreshToken到Redis
     *
     * @param userId       用户ID
     * @param username     用户名
     * @param refreshToken RefreshToken
     */
    private void saveRefreshToken(Long userId, String username, String refreshToken) {
        String refreshTokenKey = RedisKeyEnum.KEY_REFRESH_TOKEN.getKey(userId);
        Map<String, Object> refreshTokenData = buildRefreshTokenData(userId, username, refreshToken);
        redisUtil.set(refreshTokenKey, refreshTokenData,
                RedisKeyEnum.KEY_REFRESH_TOKEN.getExpire(), TimeUnit.SECONDS);
    }

    /**
     * 构建RefreshToken数据
     *
     * @param userId       用户ID
     * @param username     用户名
     * @param refreshToken RefreshToken
     * @return RefreshToken数据Map
     */
    private Map<String, Object> buildRefreshTokenData(Long userId, String username, String refreshToken) {
        Map<String, Object> data = new HashMap<>();
        data.put(SecurityConstants.KEY_REFRESH_TOKEN, refreshToken);
        data.put(SecurityConstants.JWT_CLAIM_USER_ID, userId);
        data.put(SecurityConstants.JWT_CLAIM_USERNAME, username);
        data.put(SecurityConstants.KEY_CREATE_TIME, System.currentTimeMillis());
        return data;
    }

    /**
     * 验证并获取RefreshToken数据
     *
     * @param userId       用户ID
     * @param refreshToken RefreshToken
     * @return RefreshToken数据Map，验证失败返回null
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> validateAndGetRefreshToken(Long userId, String refreshToken) {
        String refreshTokenKey = RedisKeyEnum.KEY_REFRESH_TOKEN.getKey(userId);
        Object redisValue = redisUtil.get(refreshTokenKey);

        if (redisValue == null) {
            log.warn("RefreshToken不存在或已过期，用户ID: {}", userId);
            return null;
        }

        if (!(redisValue instanceof Map)) {
            log.error("Redis数据类型异常，期望Map类型，实际类型: {}", redisValue.getClass().getName());
            return null;
        }

        Map<String, Object> refreshTokenData = (Map<String, Object>) redisValue;
        String storedRefreshToken = (String) refreshTokenData.get(SecurityConstants.KEY_REFRESH_TOKEN);

        if (!refreshToken.equals(storedRefreshToken)) {
            log.warn("RefreshToken不匹配，用户ID: {}", userId);
            return null;
        }

        return refreshTokenData;
    }

    /**
     * 删除RefreshToken
     *
     * @param userId 用户ID
     */
    private void deleteRefreshToken(Long userId) {
        String refreshTokenKey = RedisKeyEnum.KEY_REFRESH_TOKEN.getKey(userId);
        redisUtil.delete(refreshTokenKey);
    }
}
