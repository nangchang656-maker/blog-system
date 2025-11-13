package cn.lzx.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.lzx.blog.vo.ArticleListVO;

/**
 * 文章搜索服务接口
 * 基于Elasticsearch实现全文搜索
 *
 * @author lzx
 * @since 2025-11-01
 */
public interface ArticleSearchService {

    /**
     * 初始化文章索引（如果不存在则创建）
     */
    void initArticleIndex();

    /**
     * 添加或更新文章到ES索引
     *
     * @param articleId 文章ID
     */
    void syncArticleToEs(Long articleId);

    /**
     * 从ES索引中删除文章
     *
     * @param articleId 文章ID
     */
    void deleteArticleFromEs(Long articleId);

    /**
     * 全文搜索文章
     *
     * @param keyword    搜索关键词
     * @param categoryId 分类ID（可选）
     * @param tagId      标签ID（可选）
     * @param orderBy    排序字段
     * @param orderType  排序方式
     * @param page       页码
     * @param size       每页大小
     * @return 搜索结果分页
     */
    Page<ArticleListVO> searchArticles(String keyword, Long categoryId, Long tagId,
            String orderBy, String orderType, Integer page, Integer size);
}
