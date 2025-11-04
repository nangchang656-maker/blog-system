package cn.lzx.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Redis服务类
 * 提供Redis常用操作的封装
 * 
 * RequiredArgsConstructor
 * - 自动生成构造函数：为所有带有 final修饰符或使用 @NonNull注解标记的字段自动生成一个构造函数
 * - 依赖注入支持：与 Spring 框架完美配合，可以替代手动编写构造函数进行依赖注入
 * 
 * @author lzx
 * @since 2025-10-31
 */
@Service
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    // ========== String操作 ==========

    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 判断key是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取过期时间
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    // ========== Hash操作 ==========

    /**
     * 设置Hash值
     */
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 获取Hash值
     */
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 删除Hash值
     */
    public Long hDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    /**
     * 获取整个Hash
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Hash递增
     */
    public Long hIncrement(String key, String hashKey, long delta) {
        return redisTemplate.opsForHash().increment(key, hashKey, delta);
    }

    // ========== List操作 ==========

    /**
     * List右侧推入
     */
    public Long lPush(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * List左侧弹出
     */
    public Object lPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 获取List范围
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取List大小
     */
    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    // ========== Set操作 ==========

    /**
     * Set添加元素
     */
    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * Set移除元素
     */
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    /**
     * 获取Set所有元素
     */
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 判断Set是否包含元素
     */
    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    // ========== ZSet操作 ==========

    /**
     * ZSet添加元素
     */
    public Boolean zAdd(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * ZSet移除元素
     */
    public Long zRemove(String key, Object... values) {
        return redisTemplate.opsForZSet().remove(key, values);
    }

    /**
     * ZSet递增分数
     */
    public Double zIncrementScore(String key, Object value, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    /**
     * ZSet获取排名（从小到大）
     */
    public Long zRank(String key, Object value) {
        return redisTemplate.opsForZSet().rank(key, value);
    }

    /**
     * ZSet获取范围（从小到大）
     */
    public Set<Object> zRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * ZSet获取范围（从大到小）
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * ZSet获取分数
     */
    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    // ========== 黑名单操作 ==========

    /**
     * 将Token加入黑名单（使用Set存储）
     *
     * @param key Redis键
     * @param token Token值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void addToBlacklist(String key, String token, long timeout, TimeUnit unit) {
        redisTemplate.opsForSet().add(key, token);
        redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 检查Token是否在黑名单中
     *
     * @param key Redis键
     * @param token Token值
     * @return true-在黑名单中 false-不在黑名单中
     */
    public Boolean isInBlacklist(String key, String token) {
        return redisTemplate.opsForSet().isMember(key, token);
    }

    /**
     * 从黑名单中移除Token
     *
     * @param key Redis键
     * @param token Token值
     */
    public Long removeFromBlacklist(String key, String token) {
        return redisTemplate.opsForSet().remove(key, token);
    }
}
