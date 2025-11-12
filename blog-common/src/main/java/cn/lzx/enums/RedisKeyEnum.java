package cn.lzx.enums;

/**
 * Redis Key枚举类
 *
 * @author lzx
 * @since 2025-10-31
 */
public enum RedisKeyEnum {
    // ======================== 认证相关 ========================
    /**
     * 验证码 - KEY格式: blog:verifycode:{phone/email} 过期时间: 5分钟
     */
    KEY_VERIFYCODE("blog:verifycode:%s", 300),

    /**
     * 用户Token(已废弃) - KEY格式: blog:token:{userId} 过期时间: 30分钟
     */
    @Deprecated
    KEY_TOKEN("blog:token:%s", 1800),

    /**
     * RefreshToken - KEY格式: blog:auth:refresh_token:{userId} 过期时间: 7天
     */
    KEY_REFRESH_TOKEN("blog:auth:refresh_token:%s", 7 * 24 * 60 * 60),

    /**
     * AccessToken黑名单 - KEY格式: blog:auth:token_blacklist:{userId} 过期时间: 15分钟
     */
    KEY_ACCESS_TOKEN_BLACKLIST("blog:auth:token_blacklist:%s", 15 * 60);

    
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
