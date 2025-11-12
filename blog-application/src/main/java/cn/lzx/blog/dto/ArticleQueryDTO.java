package cn.lzx.blog.dto;

import cn.lzx.constants.ArticleOrderConstants;
import lombok.Data;

/**
 * 文章查询DTO
 *
 * @author lzx
 * @since 2025-11-04
 */
@Data
public class ArticleQueryDTO {

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 排序字段：create_time, view_count, like_count
     */
    private String orderBy = ArticleOrderConstants.OrderBy.CREATE_TIME;

    /**
     * 排序方式：asc, desc
     */
    private String orderType = ArticleOrderConstants.OrderType.DESC;

    /**
     * 当前页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    /**
     * 文章状态：0草稿，1已发布（可选，用于查询我的文章时过滤）
     */
    private Integer status;

    /**
     * 作者ID（可选，用于查询特定作者的文章）
     */
    private Long userId;
}
