package cn.lzx.constants;

/**
 * 安全认证相关常量
 *
 * @author lzx
 * @since 2025-10-31
 */
public class SecurityConstants {

    /**
     * JWT密钥
     */
    public static final String JWT_SECRET = "Security_Key_lzx_Blog_abcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * AccessToken过期时间(15分钟)单位:毫秒
     * 说明:采用较短的有效期,配合RefreshToken实现安全的无状态认证
     * - 注销后Token最多15分钟内失效(可接受的安全风险)
     * - 避免每次请求都访问Redis检查黑名单,保持JWT无状态特性
     */
    public static final Long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000L;

    /**
     * RefreshToken过期时间(7天)单位:秒
     */
    public static final Long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60L;

    /**
     * Token续期阈值(5分钟)单位:秒
     * 说明:当Token剩余有效期少于此阈值时,自动续期返回新Token
     */
    public static final Long TOKEN_RENEW_THRESHOLD = 5 * 60L;

    /**
     * Token请求头名称
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * Token前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 新Token响应头名称（用于自动续期）
     */
    public static final String NEW_TOKEN_HEADER = "X-New-Token";

    /**
     * JWT Claims中的用户ID字段
     */
    public static final String JWT_CLAIM_USER_ID = "userId";

    /**
     * JWT Claims中的用户名字段
     */
    public static final String JWT_CLAIM_USERNAME = "username";

    /**
     * 用户状态：正常
     */
    public static final Integer USER_STATUS_NORMAL = 1;

    /**
     * 用户状态：禁用
     */
    public static final Integer USER_STATUS_DISABLED = 0;
}
