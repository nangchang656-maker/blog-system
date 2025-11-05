package cn.lzx.blog.service;

import cn.lzx.blog.vo.ArticleListVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 互动Service接口（点赞、收藏）
 *
 * @author lzx
 * @since 2025-11-04
 */
public interface InteractionService {

    /**
     * 点赞文章
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     */
    void likeArticle(Long userId, Long articleId);

    /**
     * 取消点赞文章
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     */
    void unlikeArticle(Long userId, Long articleId);

    /**
     * 收藏文章
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     */
    void collectArticle(Long userId, Long articleId);

    /**
     * 取消收藏文章
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     */
    void uncollectArticle(Long userId, Long articleId);

    /**
     * 检查用户是否点赞了文章
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     * @return 是否点赞
     */
    boolean isLiked(Long userId, Long articleId);

    /**
     * 检查用户是否收藏了文章
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     * @return 是否收藏
     */
    boolean isCollected(Long userId, Long articleId);

    /**
     * 获取用户的收藏列表
     *
     * @param userId 用户ID
     * @param page   当前页码
     * @param size   每页数量
     * @return 收藏的文章列表
     */
    Page<ArticleListVO> getCollectedArticles(Long userId, int page, int size);
}
