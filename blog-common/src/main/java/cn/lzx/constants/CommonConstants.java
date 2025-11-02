package cn.lzx.constants;

/**
 * 通用常量类
 *
 * @author lzx
 * @since 2025-10-31
 */
public class CommonConstants {

    /**
     * 成功状态码
     */
    public static final int SUCCESS = 200;

    /**
     * 失败状态码
     */
    public static final int FAIL = 500;

    /**
     * 未认证状态码
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 无权限状态码
     */
    public static final int FORBIDDEN = 403;

    /**
     * 资源不存在状态码
     */
    public static final int NOT_FOUND = 404;

    /**
     * 用户状态：正常
     */
    public static final Integer USER_STATUS_NORMAL = 1;

    /**
     * 用户状态：禁用
     */
    public static final Integer USER_STATUS_DISABLED = 0;

    /**
     * 文章状态：草稿
     */
    public static final Integer ARTICLE_STATUS_DRAFT = 0;

    /**
     * 文章状态：已发布
     */
    public static final Integer ARTICLE_STATUS_PUBLISHED = 1;

    /**
     * 是否置顶：否
     */
    public static final Integer NOT_TOP = 0;

    /**
     * 是否置顶：是
     */
    public static final Integer IS_TOP = 1;

    /**
     * 是否删除：未删除
     */
    public static final Integer NOT_DELETED = 0;

    /**
     * 是否删除：已删除
     */
    public static final Integer DELETED = 1;
}
