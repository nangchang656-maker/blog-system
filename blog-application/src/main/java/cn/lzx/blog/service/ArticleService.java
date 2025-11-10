package cn.lzx.blog.service;

import cn.lzx.blog.dto.ArticlePublishDTO;
import cn.lzx.blog.dto.ArticleQueryDTO;
import cn.lzx.blog.vo.ArticleDetailVO;
import cn.lzx.blog.vo.ArticleListVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 文章Service接口
 *
 * @author lzx
 * @since 2025-11-04
 */
public interface ArticleService {

    /**
     * 发布文章
     *
     * @param userId 用户ID
     * @param dto    文章发布DTO
     * @return 文章ID
     */
    Long publishArticle(Long userId, ArticlePublishDTO dto);

    /**
     * 更新文章
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     * @param dto       文章更新DTO
     */
    void updateArticle(Long userId, Long articleId, ArticlePublishDTO dto);

    /**
     * 删除文章
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     */
    void deleteArticle(Long userId, Long articleId);

    /**
     * 分页查询文章列表
     *
     * @param queryDTO 查询条件
     * @return 文章列表
     */
    Page<ArticleListVO> getArticleList(ArticleQueryDTO queryDTO);

    /**
     * 获取文章详情
     *
     * @param articleId 文章ID
     * @param userId    当前用户ID（可为空）
     * @return 文章详情
     */
    ArticleDetailVO getArticleDetail(Long articleId, Long userId);

    /**
     * 获取我的文章列表
     *
     * @param userId   用户ID
     * @param queryDTO 查询条件
     * @return 文章列表
     */
    Page<ArticleListVO> getMyArticles(Long userId, ArticleQueryDTO queryDTO);

    /**
     * 增加文章评论数
     *
     * @param articleId 文章ID
     */
    void incrementCommentCount(Long articleId);

    /**
     * 减少文章评论数
     *
     * @param articleId 文章ID
     */
    void decrementCommentCount(Long articleId);
}
