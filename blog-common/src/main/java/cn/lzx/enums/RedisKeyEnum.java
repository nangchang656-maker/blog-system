package cn.lzx.enums;

/**
 * Redis Key枚举类
 *
 * @author lzx
 * @since 2025-10-31
 */
public enum RedisKeyEnum {

    /**
     * 验证码 - KEY格式: blog:verifycode:{phone/email} 过期时间: 5分钟
     */
    KEY_VERIFYCODE("blog:verifycode:%s", 300),

    /**
     * 用户Token(已废弃,改用RefreshToken) - KEY格式: blog:token:{userId} 过期时间: 30分钟
     */
    @Deprecated
    KEY_TOKEN("blog:token:%s", 1800),

    /**
     * RefreshToken - KEY格式: blog:auth:refresh_token:{userId} 过期时间: 7天
     */
    KEY_REFRESH_TOKEN("blog:auth:refresh_token:%s", 7 * 24 * 60 * 60),

    /**
     * JWT黑名单(已废弃,改用短期AccessToken) - KEY格式: blog:auth:jwt_blacklist:{token} 过期时间: 动态(token剩余时间)
     * 废弃原因：采用15分钟短期AccessToken，注销后自然过期，保持JWT无状态特性
     */
    @Deprecated
    KEY_JWT_BLACKLIST("blog:auth:jwt_blacklist:%s", -1),

    /**
     * 用户信息缓存 - KEY格式: blog:user:info:{userId} 过期时间: 30分钟
     */
    KEY_USER_INFO("blog:user:info:%s", 1800),

    /**
     * 文章详情缓存 - KEY格式: blog:article:detail:{articleId} 过期时间: 10分钟
     */
    KEY_ARTICLE_DETAIL("blog:article:detail:%s", 600),

    /**
     * 热门文章排行榜 - KEY格式: blog:article:hot 过期时间: 1小时
     */
    KEY_ARTICLE_HOT("blog:article:hot", 3600),

    /**
     * 文章点赞用户集合 - KEY格式: blog:article:like:{articleId} 过期时间: 24小时
     */
    KEY_ARTICLE_LIKE("blog:article:like:%s", 86400),

    /**
     * 文章浏览量 - KEY格式: blog:article:view:{articleId} 过期时间: 永久
     */
    KEY_ARTICLE_VIEW("blog:article:view:%s", -1),

    /**
     * 限流KEY - KEY格式: blog:rate_limit:{ip}:{uri} 过期时间: 1分钟
     */
    KEY_RATE_LIMIT("blog:rate_limit:%s:%s", 60);

    private final String key;
    private final int expireTime;

    RedisKeyEnum(String key, int expireTime) {
        this.key = key;
        this.expireTime = expireTime;
    }

    public String getKey() {
        return key;
    }

    /**
     * 获取格式化的Key
     *
     * @param params 参数
     * @return 格式化后的Key
     */
    public String getKey(Object... params) {
        return String.format(key, params);
    }

    public int getExpire() {
        return expireTime;
    }
}
