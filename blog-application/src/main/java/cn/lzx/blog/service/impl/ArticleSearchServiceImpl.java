package cn.lzx.blog.service.impl;

import cn.lzx.blog.config.es.ElasticsearchProperties;
import cn.lzx.blog.integration.es.ElasticsearchUtil;
import cn.lzx.blog.mapper.ArticleMapper;
import cn.lzx.blog.mapper.TagMapper;
import cn.lzx.blog.mapper.UserMapper;
import cn.lzx.blog.service.ArticleSearchService;
import cn.lzx.blog.service.CategoryService;
import cn.lzx.blog.vo.ArticleListVO;
import cn.lzx.blog.vo.CategoryVO;
import cn.lzx.blog.vo.TagVO;
import cn.lzx.constants.ArticleOrderConstants;
import cn.lzx.constants.CommonConstants;
import cn.lzx.entity.Article;
import cn.lzx.entity.ArticleDocument;
import cn.lzx.entity.Tag;
import cn.lzx.entity.User;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文章搜索服务实现类
 * 基于Elasticsearch实现全文搜索
 *
 * @author lzx
 * @since 2025-11-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleSearchServiceImpl implements ArticleSearchService {

    private final ElasticsearchUtil elasticsearchUtil;
    private final ElasticsearchProperties elasticsearchProperties;
    private final ArticleMapper articleMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;
    private final CategoryService categoryService;

    /**
     * 文章索引映射配置（包含IK分词器）
     */
    private static final String ARTICLE_INDEX_MAPPING = """
            {
              "mappings": {
                "properties": {
                  "id": {
                    "type": "long"
                  },
                  "title": {
                    "type": "text",
                    "analyzer": "ik_max_word",
                    "search_analyzer": "ik_smart",
                    "fields": {
                      "keyword": {
                        "type": "keyword"
                      }
                    }
                  },
                  "content": {
                    "type": "text",
                    "analyzer": "ik_max_word",
                    "search_analyzer": "ik_smart"
                  },
                  "summary": {
                    "type": "text",
                    "analyzer": "ik_max_word",
                    "search_analyzer": "ik_smart"
                  },
                  "tags": {
                    "type": "text",
                    "analyzer": "ik_max_word",
                    "search_analyzer": "ik_smart",
                    "fields": {
                      "keyword": {
                        "type": "keyword"
                      }
                    }
                  },
                  "tagIds": {
                    "type": "long"
                  },
                  "categoryId": {
                    "type": "long"
                  },
                  "categoryName": {
                    "type": "keyword"
                  },
                  "userId": {
                    "type": "long"
                  },
                  "authorName": {
                    "type": "keyword"
                  },
                  "coverImage": {
                    "type": "keyword"
                  },
                  "viewCount": {
                    "type": "integer"
                  },
                  "likeCount": {
                    "type": "integer"
                  },
                  "commentCount": {
                    "type": "integer"
                  },
                  "status": {
                    "type": "integer"
                  },
                  "createTime": {
                    "type": "date",
                    "format": "yyyy-MM-dd HH:mm:ss"
                  },
                  "updateTime": {
                    "type": "date",
                    "format": "yyyy-MM-dd HH:mm:ss"
                  }
                }
              }
            }
            """;

    @Override
    public void initArticleIndex() {
        String indexName = elasticsearchProperties.getArticleIndex();
        
        if (elasticsearchUtil.indexExists(indexName)) {
            log.info("文章索引已存在: {}", indexName);
            return;
        }

        boolean success = elasticsearchUtil.createIndex(indexName, ARTICLE_INDEX_MAPPING);
        if (success) {
            log.info("文章索引创建成功: {}", indexName);
        } else {
            log.error("文章索引创建失败: {}", indexName);
        }
    }

    @Override
    public void syncArticleToEs(Long articleId) {
        try {
            // 1. 查询文章
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，无法同步到ES: articleId={}", articleId);
                return;
            }

            // 2. 只同步已发布的文章
            if (article.getStatus() != CommonConstants.ARTICLE_STATUS_PUBLISHED) {
                log.debug("文章未发布，不同步到ES: articleId={}, status={}", articleId, article.getStatus());
                // 如果文章不是已发布状态，从ES中删除
                deleteArticleFromEs(articleId);
                return;
            }

            // 3. 查询文章标签
            List<Tag> tags = tagMapper.selectByArticleId(articleId);
            List<String> tagNames = tags.stream()
                    .map(Tag::getName)
                    .collect(Collectors.toList());
            List<Long> tagIds = tags.stream()
                    .map(Tag::getId)
                    .collect(Collectors.toList());

            // 4. 查询分类
            CategoryVO category = categoryService.getCategoryById(article.getCategoryId());

            // 5. 查询作者
            User author = userMapper.selectById(article.getUserId());

            // 6. 构建ES文档实体
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            ArticleDocument document = ArticleDocument.builder()
                    .id(article.getId())
                    .title(article.getTitle())
                    .content(article.getContent())
                    .summary(article.getSummary())
                    .tags(String.join(" ", tagNames)) // 标签名称用空格连接，用于搜索
                    .tagIds(tagIds)
                    .categoryId(article.getCategoryId())
                    .categoryName(category != null ? category.getName() : null)
                    .userId(article.getUserId())
                    .authorName(author != null 
                            ? (author.getNickname() != null ? author.getNickname() : author.getUsername()) 
                            : null)
                    .coverImage(article.getCoverImage())
                    .viewCount(article.getViewCount())
                    .likeCount(article.getLikeCount())
                    .commentCount(article.getCommentCount())
                    .status(article.getStatus())
                    .createTime(article.getCreateTime() != null 
                            ? article.getCreateTime().format(formatter) 
                            : null)
                    .updateTime(article.getUpdateTime() != null 
                            ? article.getUpdateTime().format(formatter) 
                            : null)
                    .build();

            // 7. 添加到ES
            String indexName = elasticsearchProperties.getArticleIndex();
            boolean success = elasticsearchUtil.addDocument(indexName, String.valueOf(articleId), document);
            
            if (success) {
                log.info("文章同步到ES成功: articleId={}", articleId);
            } else {
                log.error("文章同步到ES失败: articleId={}", articleId);
            }
        } catch (Exception e) {
            log.error("文章同步到ES异常: articleId={}", articleId, e);
        }
    }

    @Override
    public void deleteArticleFromEs(Long articleId) {
        String indexName = elasticsearchProperties.getArticleIndex();
        boolean success = elasticsearchUtil.deleteDocument(indexName, String.valueOf(articleId));
        if (success) {
            log.info("从ES删除文章成功: articleId={}", articleId);
        } else {
            log.warn("从ES删除文章失败（可能不存在）: articleId={}", articleId);
        }
    }

    @Override
    public Page<ArticleListVO> searchArticles(String keyword, Long categoryId, Long tagId,
                                                String orderBy, String orderType, Integer page, Integer size) {
        String indexName = elasticsearchProperties.getArticleIndex();
        
        // 如果没有关键词，返回空结果
        if (!StringUtils.hasText(keyword)) {
            return new Page<>(page, size, 0);
        }

        try {
            // 构建搜索字段（标题、内容、摘要、标签）
            String[] searchFields = {"title^3", "summary^2", "content", "tags^2"}; // 权重：标题3，摘要和标签2，内容1
            String[] highlightFields = {"title", "summary", "content"}; // 高亮字段

            // 计算分页参数
            int from = (page - 1) * size;

            // 执行搜索
            ElasticsearchUtil.SearchResult searchResult = elasticsearchUtil.searchWithTotal(
                    indexName, keyword, searchFields, highlightFields, from, size);

            // 转换为ArticleListVO
            List<ArticleListVO> articleList = convertToArticleListVO(searchResult.getResults(), categoryId, tagId);

            // 如果指定了分类或标签筛选，需要过滤结果
            if (categoryId != null || tagId != null) {
                articleList = articleList.stream()
                        .filter(article -> {
                            if (categoryId != null && !categoryId.equals(article.getCategoryId())) {
                                return false;
                            }
                            if (tagId != null) {
                                boolean hasTag = article.getTags() != null && 
                                        article.getTags().stream()
                                                .anyMatch(tag -> tagId.equals(tag.getId()));
                                if (!hasTag) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .collect(Collectors.toList());
            }

            // 排序（ES已经按相关性排序，这里可以按其他字段排序）
            if (orderBy != null && !ArticleOrderConstants.OrderBy.CREATE_TIME.equals(orderBy)) {
                Comparator<ArticleListVO> comparator = null;
                if (ArticleOrderConstants.OrderBy.VIEW_COUNT.equals(orderBy)) {
                    comparator = Comparator.comparing(ArticleListVO::getViewCount);
                } else if (ArticleOrderConstants.OrderBy.LIKE_COUNT.equals(orderBy)) {
                    comparator = Comparator.comparing(ArticleListVO::getLikeCount);
                }

                if (comparator != null) {
                    if (ArticleOrderConstants.OrderType.DESC.equalsIgnoreCase(orderType)) {
                        comparator = comparator.reversed();
                    }
                    articleList.sort(comparator);
                }
            }

            // 构建分页结果
            Page<ArticleListVO> resultPage = new Page<>(page, size);
            resultPage.setRecords(articleList);
            resultPage.setTotal(searchResult.getTotal());

            return resultPage;
        } catch (Exception e) {
            log.error("ES搜索文章异常: keyword={}", keyword, e);
            return new Page<>(page, size, 0);
        }
    }

    /**
     * 将ES搜索结果转换为ArticleListVO
     */
    private List<ArticleListVO> convertToArticleListVO(List<Map<String, Object>> searchResults, 
                                                        Long filterCategoryId, Long filterTagId) {
        if (searchResults == null || searchResults.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取文章ID列表
        List<Long> articleIds = searchResults.stream()
                .map(result -> {
                    Object id = result.get("id");
                    if (id instanceof Number) {
                        return ((Number) id).longValue();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (articleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询文章详细信息（从数据库）
        List<Article> articles = articleMapper.selectBatchIds(articleIds);
        Map<Long, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a));

        // 批量查询分类
        List<Long> categoryIds = articles.stream()
                .map(Article::getCategoryId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, CategoryVO> categoryMap = categoryService.getCategoryMapByIds(categoryIds);

        // 批量查询标签
        Map<Long, List<Tag>> articleTagsMap = new HashMap<>();
        for (Long articleId : articleIds) {
            List<Tag> tags = tagMapper.selectByArticleId(articleId);
            articleTagsMap.put(articleId, tags);
        }

        // 批量查询作者
        List<Long> userIds = articles.stream()
                .map(Article::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 转换为VO（保持ES搜索结果的顺序）
        List<ArticleListVO> result = new ArrayList<>();
        for (Map<String, Object> searchResult : searchResults) {
            Object idObj = searchResult.get("id");
            if (idObj == null) {
                continue;
            }
            Long articleId = ((Number) idObj).longValue();
            Article article = articleMap.get(articleId);
            if (article == null) {
                continue;
            }

            // 只返回已发布的文章
            if (article.getStatus() != CommonConstants.ARTICLE_STATUS_PUBLISHED) {
                continue;
            }

            CategoryVO category = categoryMap.get(article.getCategoryId());
            List<Tag> tags = articleTagsMap.getOrDefault(articleId, new ArrayList<>());
            User author = userMap.get(article.getUserId());

            // 处理高亮标题和摘要
            String title = (String) searchResult.get("title");
            String summary = (String) searchResult.get("summary");

            ArticleListVO vo = ArticleListVO.builder()
                    .id(article.getId())
                    .title(title != null ? title : article.getTitle())
                    .summary(summary != null ? summary : article.getSummary())
                    .coverImage(article.getCoverImage())
                    .categoryId(article.getCategoryId())
                    .categoryName(category != null ? category.getName() : null)
                    .tags(tags.stream()
                            .map(tag -> TagVO.builder()
                                    .id(tag.getId())
                                    .name(tag.getName())
                                    .build())
                            .collect(Collectors.toList()))
                    .authorId(article.getUserId())
                    .authorName(author != null
                            ? (author.getNickname() != null ? author.getNickname() : author.getUsername())
                            : null)
                    .authorAvatar(author != null ? author.getAvatar() : null)
                    .viewCount(article.getViewCount())
                    .likeCount(article.getLikeCount())
                    .commentCount(article.getCommentCount())
                    .status(article.getStatus())
                    .createTime(article.getCreateTime())
                    .updateTime(article.getUpdateTime())
                    .build();

            result.add(vo);
        }

        return result;
    }
}

