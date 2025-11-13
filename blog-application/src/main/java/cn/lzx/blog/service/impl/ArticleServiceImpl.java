package cn.lzx.blog.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.lzx.blog.dto.ArticlePublishDTO;
import cn.lzx.blog.dto.ArticleQueryDTO;
import cn.lzx.blog.mapper.ArticleMapper;
import cn.lzx.blog.mapper.ArticleTagMapper;
import cn.lzx.blog.mapper.CollectMapper;
import cn.lzx.blog.mapper.LikeRecordMapper;
import cn.lzx.blog.mapper.TagMapper;
import cn.lzx.blog.mapper.UserMapper;
import cn.lzx.blog.service.ArticleSearchService;
import cn.lzx.blog.service.ArticleService;
import cn.lzx.blog.service.CategoryService;
import cn.lzx.blog.service.TagService;
import cn.lzx.blog.vo.ArticleDetailVO;
import cn.lzx.blog.vo.ArticleListVO;
import cn.lzx.blog.vo.CategoryVO;
import cn.lzx.blog.vo.TagVO;
import cn.lzx.constants.AdminConstants;
import cn.lzx.constants.ArticleOrderConstants;
import cn.lzx.constants.CommonConstants;
import cn.lzx.entity.Article;
import cn.lzx.entity.ArticleTag;
import cn.lzx.entity.Collect;
import cn.lzx.entity.LikeRecord;
import cn.lzx.entity.Tag;
import cn.lzx.entity.User;
import cn.lzx.enums.RedisKeyEnum;
import cn.lzx.exception.BusinessException;
import cn.lzx.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;

/**
 * 文章Service实现类
 *
 * @author lzx
 * @since 2025-11-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final CollectMapper collectMapper;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final ArticleSearchService articleSearchService;
    private final cn.lzx.blog.integration.ai.ZhipuAIService zhipuAIService;
    private final RedisUtil redisUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishArticle(Long userId, ArticlePublishDTO dto) {
        // 0. 校验用户ID
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }

        // 1. 处理分类（优先使用ID，如果ID为空则使用名称创建或获取）
        Long categoryId = resolveCategoryId(dto.getCategoryId(), dto.getCategoryName());

        // 2. 创建文章
        Article article = Article.builder()
                .userId(userId)
                .title(dto.getTitle())
                .content(dto.getContent())
                .summary(dto.getSummary())
                .outline(dto.getOutline())
                .coverImage(dto.getCoverImage())
                .categoryId(categoryId)
                .status(dto.getStatus())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .collectCount(0)
                .isTop(0)
                .build();

        log.info("准备插入文章: userId={}, title={}, article={}", userId, dto.getTitle(), article);

        int result = articleMapper.insert(article);
        if (result <= 0) {
            throw new BusinessException("发布文章失败");
        }

        // 3. 处理标签（同时支持ID列表和名称列表）
        List<Long> tagIds = resolveTagIds(dto.getTagIds(), dto.getTagNames());

        // 4. 保存文章标签关联(不考虑异步,该操作目前不频繁)
        if (!tagIds.isEmpty()) {
            saveArticleTags(article.getId(), tagIds);
        }

        // 5. 同步文章到ES（如果已发布）
        if (article.getStatus() == CommonConstants.ARTICLE_STATUS_PUBLISHED) {
            articleSearchService.syncArticleToEs(article.getId());
        }

        // 6. 清除文章缓存（如果存在）
        String cacheKey = RedisKeyEnum.KEY_ARTICLE_CACHE.getKey(article.getId());
        redisUtil.delete(cacheKey);

        log.info("用户[{}]发布文章成功，文章ID: {}", userId, article.getId());
        return article.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(Long userId, Long articleId, ArticlePublishDTO dto) {
        // 1. 查询文章
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 2. 验证权限（只能编辑自己的文章）
        if (!article.getUserId().equals(userId)) {
            throw new BusinessException("无权限编辑此文章");
        }

        // 2.1 验证文章状态
        if (article.getStatus() == CommonConstants.ARTICLE_STATUS_BLOCKED) {
            throw new BusinessException("已屏蔽文章不能编辑");
        }

        // 3. 处理分类（优先使用ID，如果ID为空则使用名称创建或获取）
        Long categoryId = resolveCategoryId(dto.getCategoryId(), dto.getCategoryName());

        // 4. 更新文章
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setSummary(dto.getSummary());
        article.setOutline(dto.getOutline());
        article.setCoverImage(dto.getCoverImage());
        article.setCategoryId(categoryId);
        article.setStatus(dto.getStatus());
        article.setUpdateTime(LocalDateTime.now());

        int result = articleMapper.updateById(article);
        if (result <= 0) {
            throw new BusinessException("更新文章失败");
        }

        // 5. 处理标签（同时支持ID列表和名称列表）
        List<Long> tagIds = resolveTagIds(dto.getTagIds(), dto.getTagNames());

        // 6. 更新文章标签关联（先删除旧的，再插入新的）
        LambdaQueryWrapper<ArticleTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleTag::getArticleId, articleId);
        articleTagMapper.delete(wrapper);

        if (!tagIds.isEmpty()) {
            saveArticleTags(articleId, tagIds);
        }

        // 7. 同步文章到ES（如果已发布）
        if (article.getStatus() == CommonConstants.ARTICLE_STATUS_PUBLISHED) {
            articleSearchService.syncArticleToEs(articleId);
        } else {
            // 如果不是已发布状态，从ES中删除
            articleSearchService.deleteArticleFromEs(articleId);
        }

        // 8. 清除文章缓存
        String cacheKey = RedisKeyEnum.KEY_ARTICLE_CACHE.getKey(articleId);
        redisUtil.delete(cacheKey);

        log.info("用户[{}]更新文章成功，文章ID: {}", userId, articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long userId, Long articleId) {
        // 1. 查询文章
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 2. 验证权限（只能删除自己的文章）
        if (!article.getUserId().equals(userId)) {
            throw new BusinessException("无权限删除此文章");
        }

        // 3. 逻辑删除文章
        int result = articleMapper.deleteById(articleId);
        if (result <= 0) {
            throw new BusinessException("删除文章失败");
        }

        // 4. 删除文章标签关联
        LambdaQueryWrapper<ArticleTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleTag::getArticleId, articleId);
        articleTagMapper.delete(wrapper);

        // 5. 从ES中删除文章
        articleSearchService.deleteArticleFromEs(articleId);

        // 6. 清除文章缓存
        String cacheKey = RedisKeyEnum.KEY_ARTICLE_CACHE.getKey(articleId);
        redisUtil.delete(cacheKey);

        log.info("用户[{}]删除文章成功，文章ID: {}", userId, articleId);
    }

    @Override
    public Page<ArticleListVO> getArticleList(ArticleQueryDTO queryDTO) {
        // 如果有关键词，使用ES搜索；否则使用数据库查询
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            // 使用Elasticsearch全文搜索
            return articleSearchService.searchArticles(
                    queryDTO.getKeyword(),
                    queryDTO.getCategoryId(),
                    queryDTO.getTagId(),
                    queryDTO.getOrderBy(),
                    queryDTO.getOrderType(),
                    queryDTO.getPage(),
                    queryDTO.getSize());
        }

        // 无关键词时，使用数据库查询
        // 1. 构建分页对象
        Page<Article> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        // 2. 构建查询条件
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1); // 只查询已发布的文章

        // 分类筛选
        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(Article::getCategoryId, queryDTO.getCategoryId());
        }

        // 排序
        String orderBy = queryDTO.getOrderBy();
        boolean isAsc = ArticleOrderConstants.OrderType.ASC.equalsIgnoreCase(queryDTO.getOrderType());

        if (ArticleOrderConstants.OrderBy.VIEW_COUNT.equals(orderBy)) {
            wrapper.orderBy(true, isAsc, Article::getViewCount);
        } else if (ArticleOrderConstants.OrderBy.LIKE_COUNT.equals(orderBy)) {
            wrapper.orderBy(true, isAsc, Article::getLikeCount);
        } else {
            wrapper.orderBy(true, isAsc, Article::getCreateTime);
        }

        // 3. 查询文章列表
        IPage<Article> articlePage = articleMapper.selectPage(page, wrapper);

        // 4. 转换为VO
        Page<ArticleListVO> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(),
                articlePage.getTotal());
        List<ArticleListVO> voList = convertToArticleListVO(articlePage.getRecords(), queryDTO.getTagId());
        voPage.setRecords(voList);

        return voPage;
    }

    @SuppressWarnings("null")
    @Override
    public ArticleDetailVO getArticleDetail(Long articleId, Long userId) {
        // 0. 尝试从缓存获取（仅对已发布的文章使用缓存，草稿和屏蔽文章不缓存）
        String cacheKey = RedisKeyEnum.KEY_ARTICLE_CACHE.getKey(articleId);
        ArticleDetailVO cachedDetail = null;
        
        // 先查询文章状态，判断是否可以使用缓存
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 只有已发布的文章才使用缓存
        boolean useCache = article.getStatus() == CommonConstants.ARTICLE_STATUS_PUBLISHED;
        if (useCache) {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null && cached instanceof ArticleDetailVO) {
                cachedDetail = (ArticleDetailVO) cached;
                log.debug("从缓存获取文章详情: articleId={}", articleId);
            }
        }

        ArticleDetailVO articleDetail;
        
        if (cachedDetail != null) {
            // 使用缓存数据，但需要更新用户相关的点赞和收藏状态
            articleDetail = cachedDetail;
            
            // 更新浏览量（缓存中的数据可能不是最新的）
            if (article.getViewCount() != null) {
                articleDetail.setViewCount(article.getViewCount());
            }
            
            // 增加浏览量（异步更新到数据库，这里先更新内存中的值）
            articleMapper.incrementViewCount(articleId);
            if (articleDetail.getViewCount() != null) {
                articleDetail.setViewCount(articleDetail.getViewCount() + 1);
            }
        } else {
            // 缓存未命中，从数据库查询
            // 1. 草稿文章权限判断：只有作者本人可以查看草稿
            if (article.getStatus() == CommonConstants.ARTICLE_STATUS_DRAFT) {
                if (userId == null || !article.getUserId().equals(userId)) {
                    throw new BusinessException("无权限查看此文章");
                }
            }

            // 2. 屏蔽文章权限判断：被屏蔽的文章不对外显示（作者本人和管理员可以查看）
            if (article.getStatus() == CommonConstants.ARTICLE_STATUS_BLOCKED) {
                if (userId == null
                        || (!article.getUserId().equals(userId) && !AdminConstants.isAdmin(userId))) {
                    throw new BusinessException("文章不存在或已被屏蔽");
                }
            }

            // 3. 增加浏览量（草稿和屏蔽文章不增加浏览量）
            if (article.getStatus() == CommonConstants.ARTICLE_STATUS_PUBLISHED) {
                articleMapper.incrementViewCount(articleId);
                article.setViewCount(article.getViewCount() + 1);
            }

            // 4. 查询文章作者信息（尝试从缓存获取）
            User author = getUserFromCacheOrDb(article.getUserId());

            // 5. 查询文章标签
            List<Tag> tags = tagMapper.selectByArticleId(articleId);
            List<TagVO> tagVOList = tags.stream()
                    .map(tag -> TagVO.builder()
                            .id(tag.getId())
                            .name(tag.getName())
                            .build())
                    .collect(Collectors.toList());

            // 6. 查询分类
            CategoryVO category = categoryService.getCategoryById(article.getCategoryId());

            // 7. 如果摘要为空，自动生成摘要（仅对已发布的文章）
            String summary = article.getSummary();
            if ((summary == null || summary.trim().isEmpty()) 
                    && article.getStatus() == CommonConstants.ARTICLE_STATUS_PUBLISHED) {
                try {
                    log.info("文章摘要为空，自动生成摘要: articleId={}", articleId);
                    summary = zhipuAIService.generateSummary(article.getContent());
                } catch (Exception e) {
                    log.warn("自动生成摘要失败: articleId={}", articleId, e);
                    summary = "这是一篇精心编写的技术文章，欢迎阅读。";
                }
            }

            // 8. 构建ArticleDetailVO（先不设置isLiked和isCollected）
            articleDetail = ArticleDetailVO.builder()
                    .id(article.getId())
                    .title(article.getTitle())
                    .content(article.getContent())
                    .summary(summary)
                    .outline(article.getOutline())
                    .coverImage(article.getCoverImage())
                    .categoryId(article.getCategoryId())
                    .categoryName(category != null ? category.getName() : null)
                    .tags(tagVOList)
                    .authorId(article.getUserId())
                    .authorName(author != null ? (author.getNickname() != null ? author.getNickname() : author.getUsername()) : null)
                    .authorAvatar(author != null ? author.getAvatar() : null)
                    .viewCount(article.getViewCount())
                    .likeCount(article.getLikeCount())
                    .commentCount(article.getCommentCount())
                    .collectCount(article.getCollectCount())
                    .isLiked(false) // 临时值，下面会更新
                    .isCollected(false) // 临时值，下面会更新
                    .createTime(article.getCreateTime())
                    .updateTime(article.getUpdateTime())
                    .build();
            
            // 9. 如果是已发布的文章，存入缓存（10分钟过期）
            if (useCache) {
                redisUtil.set(cacheKey, articleDetail, RedisKeyEnum.KEY_ARTICLE_CACHE.getExpire(), TimeUnit.SECONDS);
                log.debug("文章详情已存入缓存: articleId={}", articleId);
            }
        }

        // 10. 查询当前用户是否点赞和收藏（如果用户已登录，使用Redis Set优化查询）
        Boolean isLiked = false;
        Boolean isCollected = false;
        if (userId != null) {
            // 使用Redis Set查询点赞状态
            String likeSetKey = RedisKeyEnum.KEY_ARTICLE_LIKES.getKey(articleId);
            isLiked = redisUtil.sIsMember(likeSetKey, userId);
            
            // 如果Redis中没有，查询数据库并同步到Redis
            if (!isLiked) {
                LambdaQueryWrapper<LikeRecord> likeWrapper = new LambdaQueryWrapper<>();
                likeWrapper.eq(LikeRecord::getUserId, userId)
                        .eq(LikeRecord::getTargetId, articleId)
                        .eq(LikeRecord::getType, 1); // 1表示文章点赞
                long likeCount = likeRecordMapper.selectCount(likeWrapper);
                isLiked = likeCount > 0;
                
                // 如果数据库中有记录，同步到Redis
                if (isLiked) {
                    redisUtil.sAdd(likeSetKey, userId);
                }
            }

            // 查询是否收藏（收藏功能暂时不使用Redis缓存）
            LambdaQueryWrapper<Collect> collectWrapper = new LambdaQueryWrapper<>();
            collectWrapper.eq(Collect::getUserId, userId)
                    .eq(Collect::getArticleId, articleId);
            long collectCount = collectMapper.selectCount(collectWrapper);
            isCollected = collectCount > 0;
        }
        
        // 11. 设置用户相关的点赞和收藏状态
        articleDetail.setIsLiked(isLiked);
        articleDetail.setIsCollected(isCollected);

        return articleDetail;
    }

    /**
     * 从缓存或数据库获取用户信息
     */
    private User getUserFromCacheOrDb(Long userId) {
        String userCacheKey = RedisKeyEnum.KEY_USER_CACHE.getKey(userId);
        Object cached = redisUtil.get(userCacheKey);
        if (cached != null && cached instanceof User) {
            log.debug("从缓存获取用户信息: userId={}", userId);
            return (User) cached;
        }
        
        User user = userMapper.selectById(userId);
        if (user != null) {
            // 存入缓存（30分钟过期）
            redisUtil.set(userCacheKey, user, RedisKeyEnum.KEY_USER_CACHE.getExpire(), TimeUnit.SECONDS);
            log.debug("用户信息已存入缓存: userId={}", userId);
        }
        return user;
    }

    @Override
    public Page<ArticleListVO> getMyArticles(Long userId, ArticleQueryDTO queryDTO) {
        // 1. 构建分页对象
        Page<Article> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        // 2. 构建查询条件
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getUserId, userId);

        // 状态筛选（草稿、已发布[包含已屏蔽])
        if (queryDTO.getStatus() != null) {
            if (queryDTO.getStatus() == CommonConstants.ARTICLE_STATUS_PUBLISHED) {
                wrapper.and(w -> w.eq(Article::getStatus, CommonConstants.ARTICLE_STATUS_PUBLISHED)
                        .or().eq(Article::getStatus, CommonConstants.ARTICLE_STATUS_BLOCKED));
            } else {
                // 其他状态直接查询(草稿)
                wrapper.eq(Article::getStatus, queryDTO.getStatus());
            }
        }

        // 排序（默认按创建时间倒序）
        wrapper.orderByDesc(Article::getCreateTime);

        // 3. 查询文章列表
        IPage<Article> articlePage = articleMapper.selectPage(page, wrapper);

        // 4. 转换为VO
        Page<ArticleListVO> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(),
                articlePage.getTotal());
        List<ArticleListVO> voList = convertToArticleListVO(articlePage.getRecords(), null);
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 解析分类ID（优先使用ID，如果ID为空则使用名称创建或获取）
     * 
     * @param categoryId   分类ID
     * @param categoryName 分类名称
     * @return 分类ID
     */
    private Long resolveCategoryId(Long categoryId, String categoryName) {
        if (categoryId != null) {
            CategoryVO category = categoryService.getCategoryById(categoryId);
            if (category == null) {
                throw new BusinessException("分类不存在");
            }
            return categoryId;
        }

        if (StringUtils.hasText(categoryName)) {
            return categoryService.getOrCreateCategoryByName(categoryName.trim());
        }

        throw new BusinessException("分类不能为空");
    }

    /**
     * 解析标签ID列表（同时支持ID列表和名称列表）
     * 
     * @param tagIds   标签ID列表
     * @param tagNames 标签名称列表
     * @return 标签ID列表
     */
    private List<Long> resolveTagIds(List<Long> tagIds, List<String> tagNames) {
        List<Long> result = new ArrayList<>();

        // 添加已存在的标签ID
        if (tagIds != null && !tagIds.isEmpty()) {
            result.addAll(tagIds);
        }

        // 添加新创建的标签ID
        // TODO: 定时任务清理长期不使用的标签
        if (tagNames != null && !tagNames.isEmpty()) {
            List<Long> newTagIds = tagService.getOrCreateTagsByNames(tagNames);
            result.addAll(newTagIds);
        }

        return result;
    }

    /**
     * 保存文章标签关联
     */
    private void saveArticleTags(Long articleId, List<Long> tagIds) {
        for (Long tagId : tagIds) {
            // 验证标签是否存在
            Tag tag = tagMapper.selectById(tagId);
            if (tag == null) {
                throw new BusinessException("标签ID[" + tagId + "]不存在");
            }

            ArticleTag articleTag = ArticleTag.builder()
                    .articleId(articleId)
                    .tagId(tagId)
                    .createTime(LocalDateTime.now())
                    .build();
            articleTagMapper.insert(articleTag);
        }
    }

    /**
     * 转换为ArticleListVO列表
     */
    private List<ArticleListVO> convertToArticleListVO(List<Article> articles, Long filterTagId) {
        if (articles == null || articles.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询作者信息
        List<Long> userIds = articles.stream()
                .map(Article::getUserId)
                .distinct()
                .collect(Collectors.toList());

        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 批量查询分类信息
        List<Long> categoryIds = articles.stream()
                .map(Article::getCategoryId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, CategoryVO> categoryMap = categoryService.getCategoryMapByIds(categoryIds);

        // 批量查询文章标签
        List<Long> articleIds = articles.stream()
                .map(Article::getId)
                .collect(Collectors.toList());

        Map<Long, List<Tag>> articleTagsMap = getArticleTagsMap(articleIds);

        // 转换为VO
        return articles.stream()
                .filter(article -> {
                    // 如果指定了标签筛选，则只返回包含该标签的文章
                    if (filterTagId != null) {
                        List<Tag> tags = articleTagsMap.get(article.getId());
                        return tags != null && tags.stream().anyMatch(tag -> tag.getId().equals(filterTagId));
                    }
                    return true;
                })
                .map(article -> {
                    User author = userMap.get(article.getUserId());
                    CategoryVO category = categoryMap.get(article.getCategoryId());
                    List<Tag> tags = articleTagsMap.getOrDefault(article.getId(), new ArrayList<>());

                    List<TagVO> tagVOList = tags.stream()
                            .map(tag -> TagVO.builder()
                                    .id(tag.getId())
                                    .name(tag.getName())
                                    .build())
                            .collect(Collectors.toList());

                    return ArticleListVO.builder()
                            .id(article.getId())
                            .title(article.getTitle())
                            .summary(article.getSummary())
                            .coverImage(article.getCoverImage())
                            .categoryId(article.getCategoryId())
                            .categoryName(category != null ? category.getName() : null)
                            .tags(tagVOList)
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
                })
                .collect(Collectors.toList());
    }

    /**
     * 批量查询文章标签映射
     */
    private Map<Long, List<Tag>> getArticleTagsMap(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }

        // 查询所有文章标签关联
        LambdaQueryWrapper<ArticleTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ArticleTag::getArticleId, articleIds);
        List<ArticleTag> articleTags = articleTagMapper.selectList(wrapper);

        if (articleTags.isEmpty()) {
            return Map.of();
        }

        // 批量查询标签信息
        List<Long> tagIds = articleTags.stream()
                .map(ArticleTag::getTagId)
                .distinct()
                .collect(Collectors.toList());

        List<Tag> tags = tagMapper.selectBatchIds(tagIds);
        Map<Long, Tag> tagMap = tags.stream()
                .collect(Collectors.toMap(Tag::getId, t -> t));

        // 构建文章ID -> 标签列表的映射
        return articleTags.stream()
                .collect(Collectors.groupingBy(
                        ArticleTag::getArticleId,
                        Collectors.mapping(
                                at -> tagMap.get(at.getTagId()),
                                Collectors.toList())));
    }

    @Override
    public void incrementCommentCount(Long articleId) {
        articleMapper.incrementCommentCount(articleId);
    }

    @Override
    public void decrementCommentCount(Long articleId) {
        articleMapper.decrementCommentCount(articleId);
    }

    @Override
    public List<ArticleListVO> getHotArticles(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10; // 默认返回10篇
        }
        if (limit > 100) {
            limit = 100; // 最多返回100篇
        }

        // 1. 从Redis ZSet中获取热门文章ID（按浏览量降序）
        String hotArticlesKey = RedisKeyEnum.KEY_HOT_ARTICLES.getKey();
        Set<Object> articleIdSet = redisUtil.zReverseRange(hotArticlesKey, 0, limit - 1);

        if (articleIdSet == null || articleIdSet.isEmpty()) {
            log.debug("热门文章排行榜为空，返回空列表");
            return new ArrayList<>();
        }

        // 2. 将Object转换为Long类型的文章ID列表
        List<Long> articleIds = articleIdSet.stream()
                .map(id -> {
                    if (id instanceof String) {
                        return Long.parseLong((String) id);
                    } else if (id instanceof Long) {
                        return (Long) id;
                    } else if (id instanceof Number) {
                        return ((Number) id).longValue();
                    }
                    return null;
                })
                .filter(id -> id != null)
                .collect(Collectors.toList());

        if (articleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 批量查询文章信息
        List<Article> articles = articleMapper.selectBatchIds(articleIds);
        if (articles.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. 按照Redis返回的顺序排序
        Map<Long, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a));
        List<Article> sortedArticles = articleIds.stream()
                .map(articleMap::get)
                .filter(a -> a != null && a.getStatus() == CommonConstants.ARTICLE_STATUS_PUBLISHED)
                .collect(Collectors.toList());

        // 5. 转换为ArticleListVO
        return convertToArticleListVO(sortedArticles, null);
    }
}
