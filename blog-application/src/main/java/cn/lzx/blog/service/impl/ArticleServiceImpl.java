package cn.lzx.blog.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import cn.lzx.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

        log.info("用户[{}]删除文章成功，文章ID: {}", userId, articleId);
    }

    @Override
    public Page<ArticleListVO> getArticleList(ArticleQueryDTO queryDTO) {
        // 1. 构建分页对象
        Page<Article> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        // 2. 构建查询条件
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1); // 只查询已发布的文章

        // 分类筛选
        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(Article::getCategoryId, queryDTO.getCategoryId());
        }

        // TODO: 采用es+ik分词,提高查询性能
        // 关键词搜索（标题或摘要）
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Article::getTitle, queryDTO.getKeyword())
                    .or()
                    .like(Article::getSummary, queryDTO.getKeyword()));
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
        // 1. 查询文章
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 2. 草稿文章权限判断：只有作者本人可以查看草稿
        if (article.getStatus() == CommonConstants.ARTICLE_STATUS_DRAFT) {
            if (userId == null || !article.getUserId().equals(userId)) {
                throw new BusinessException("无权限查看此文章");
            }
        }

        // 3. 屏蔽文章权限判断：被屏蔽的文章不对外显示（作者本人和管理员可以查看）
        // TODO: 使用缓存时,注意文章被屏蔽后缓存的可见性
        if (article.getStatus() == CommonConstants.ARTICLE_STATUS_BLOCKED) {
            if (userId == null
                    || (!article.getUserId().equals(userId) && !AdminConstants.isAdmin(userId))) {
                throw new BusinessException("文章不存在或已被屏蔽");
            }
        }

        // 4. 增加浏览量（草稿和屏蔽文章不增加浏览量）
        if (article.getStatus() == CommonConstants.ARTICLE_STATUS_PUBLISHED) {
            // TODO: 文章的点赞等信息单独缓存,处理缓存中的数据,通过定时任务更新数据库
            articleMapper.incrementViewCount(articleId);
            article.setViewCount(article.getViewCount() + 1);
        }

        // 5. 查询文章作者信息
        User author = userMapper.selectById(article.getUserId());

        // 6. 查询文章标签
        List<Tag> tags = tagMapper.selectByArticleId(articleId);
        List<TagVO> tagVOList = tags.stream()
                .map(tag -> TagVO.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .build())
                .collect(Collectors.toList());

        // 7. 查询分类
        CategoryVO category = categoryService.getCategoryById(article.getCategoryId());

        // 8. 查询当前用户是否点赞和收藏（如果用户已登录）
        // TODO: 这些信息也单独缓存
        Boolean isLiked = false;
        Boolean isCollected = false;
        if (userId != null) {
            // 查询是否点赞
            LambdaQueryWrapper<LikeRecord> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(LikeRecord::getUserId, userId)
                    .eq(LikeRecord::getTargetId, articleId)
                    .eq(LikeRecord::getType, 1); // 1表示文章点赞
            long likeCount = likeRecordMapper.selectCount(likeWrapper);
            isLiked = likeCount > 0;
            log.debug("查询点赞状态 - 用户ID: {}, 文章ID: {}, 点赞记录数: {}, isLiked: {}", userId, articleId, likeCount, isLiked);

            // 查询是否收藏
            LambdaQueryWrapper<Collect> collectWrapper = new LambdaQueryWrapper<>();
            collectWrapper.eq(Collect::getUserId, userId)
                    .eq(Collect::getArticleId, articleId);
            long collectCount = collectMapper.selectCount(collectWrapper);
            isCollected = collectCount > 0;
            log.debug("查询收藏状态 - 用户ID: {}, 文章ID: {}, 收藏记录数: {}, isCollected: {}", userId, articleId, collectCount,
                    isCollected);
        } else {
            log.debug("用户未登录，不查询点赞和收藏状态 - 文章ID: {}", articleId);
        }

        // 9. 构建ArticleDetailVO
        return ArticleDetailVO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(article.getContent())
                .summary(article.getSummary())
                .coverImage(article.getCoverImage())
                .categoryId(article.getCategoryId())
                .categoryName(category != null ? category.getName() : null)
                .tags(tagVOList)
                .authorId(article.getUserId())
                .authorName(author != null ? author.getNickname() : author.getUsername())
                .authorAvatar(author != null ? author.getAvatar() : null)
                .viewCount(article.getViewCount())
                .likeCount(article.getLikeCount())
                .commentCount(article.getCommentCount())
                .collectCount(article.getCollectCount())
                .isLiked(isLiked)
                .isCollected(isCollected)
                .createTime(article.getCreateTime())
                .updateTime(article.getUpdateTime())
                .build();
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
}
