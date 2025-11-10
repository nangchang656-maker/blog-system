package cn.lzx.blog.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据统计VO
 *
 * @author lzx
 * @since 2025-11-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsVO {

    /**
     * 文章总数
     */
    private Long articleCount;

    /**
     * 已发布文章数
     */
    private Long publishedArticleCount;

    /**
     * 草稿文章数
     */
    private Long draftArticleCount;

    /**
     * 用户总数
     */
    private Long userCount;

    /**
     * 正常用户数
     */
    private Long normalUserCount;

    /**
     * 禁用用户数
     */
    private Long disabledUserCount;

    /**
     * 评论总数
     */
    private Long commentCount;

    /**
     * 正常评论数
     */
    private Long normalCommentCount;

    /**
     * 待审核评论数
     */
    private Long hiddenCommentCount;

    /**
     * 总访问量（所有文章的浏览量总和）
     */
    private Long totalViewCount;

    /**
     * 总点赞数（所有文章的点赞数总和）
     */
    private Long totalLikeCount;

    /**
     * 总收藏数（所有文章的收藏数总和）
     */
    private Long totalCollectCount;

    /**
     * 分类总数
     */
    private Long categoryCount;

    /**
     * 标签总数
     */
    private Long tagCount;
}

