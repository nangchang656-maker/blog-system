package cn.lzx.service;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import cn.lzx.enums.RedisKeyEnum;
import cn.lzx.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 接口限流服务
 * 基于Redis实现固定窗口限流算法
 * 
 * 限流原理：
 * 1. 使用Redis的INCR命令实现计数器
 * 2. 使用EXPIRE设置过期时间，实现时间窗口
 * 3. 每次请求时，计数器+1，如果超过限制则拒绝请求
 * 
 * @author lzx
 * @since 2025-01-XX
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisUtil redisUtil;

    /**
     * 检查是否允许请求（固定窗口限流）
     * 
     * @param key   限流key（如：login、register等）
     * @param identifier 限流标识符（用户ID或IP地址）
     * @param count 时间窗口内允许的最大请求次数
     * @param time  时间窗口（单位：秒）
     * @return true-允许请求 false-超过限流，拒绝请求
     */
    public boolean tryAcquire(String key, String identifier, int count, int time) {
        // 构建Redis key: blog:rate_limit:{key}:{identifier}
        String redisKey = RedisKeyEnum.KEY_RATE_LIMIT.getKey(key, identifier);

        try {
            // 获取当前计数
            Long currentCount = redisUtil.increment(redisKey);

            // 如果是第一次请求（计数为1），设置过期时间
            if (currentCount == 1) {
                redisUtil.expire(redisKey, time, TimeUnit.SECONDS);
                log.debug("限流key首次请求: {}, 设置过期时间: {}秒", redisKey, time);
            }

            // 检查是否超过限制
            if (currentCount > count) {
                log.warn("接口限流触发: key={}, identifier={}, 当前计数={}, 限制={}", 
                        key, identifier, currentCount, count);
                return false;
            }

            log.debug("接口限流检查通过: key={}, identifier={}, 当前计数={}, 限制={}", 
                    key, identifier, currentCount, count);
            return true;

        } catch (Exception e) {
            // Redis异常时，为了不影响业务，默认允许请求
            log.error("限流检查异常，默认允许请求: key={}, identifier={}, error={}", 
                    key, identifier, e.getMessage());
            return true;
        }
    }

    /**
     * 获取当前请求计数
     * 
     * @param key       限流key
     * @param identifier 限流标识符
     * @return 当前计数，key不存在返回0
     */
    public Long getCurrentCount(String key, String identifier) {
        String redisKey = RedisKeyEnum.KEY_RATE_LIMIT.getKey(key, identifier);
        Object value = redisUtil.get(redisKey);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("限流计数格式异常: key={}, value={}", redisKey, value);
            return 0L;
        }
    }
}

