package cn.lzx.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * 基于Redis实现固定窗口限流算法
 * 
 * 使用示例：
 * 
 * <pre>
 * {@code
 * @RateLimit(key = "login", count = 5, time = 60) // 60秒内最多5次
 * public R login(@RequestBody LoginDTO dto) {
 *     // ...
 * }
 * }
 * </pre>
 *
 * @author lzx
 * @since 2025-01-XX
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流key（必填）
     * 用于区分不同的限流场景，如：login、register、comment等
     * 如果为空，则使用"方法名"作为key
     */
    String key() default "";

    /**
     * 时间窗口（单位：秒）
     * 默认60秒
     */
    int time() default 60;

    /**
     * 时间窗口内允许的最大请求次数
     * 默认10次
     */
    int count() default 10;

    /**
     * 限流提示信息
     * 默认提示信息
     */
    String message() default "请求过于频繁，请稍后再试";

    /**
     * 是否按用户限流
     * true: 按用户ID限流（需要用户登录）
     * false: 按IP限流（适用于未登录场景）
     * 默认false
     */
    boolean byUser() default false;
}
