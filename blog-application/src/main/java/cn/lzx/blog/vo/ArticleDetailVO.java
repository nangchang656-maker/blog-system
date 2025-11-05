package cn.lzx.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章详情VO
 *
 * @author lzx
 * @since 2025-11-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDetailVO {

    /**
     * 文章ID
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容（Markdown格式）
     */
    private String content;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 封面图
     */
    private String coverImage;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 标签列表
     */
    private List<TagVO> tags;

    /**
     * 作者ID
     */
    private Long authorId;

    /**
     * 作者名称
     */
    private String authorName;

    /**
     * 作者头像
     */
    private String authorAvatar;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 收藏数
     */
    private Integer collectCount;

    /**
     * 当前用户是否已点赞
     */
    private Boolean isLiked;

    /**
     * 当前用户是否已收藏
     */
    private Boolean isCollected;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
