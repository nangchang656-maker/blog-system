package cn.lzx.blog.service.impl;

import cn.lzx.blog.dto.admin.ArticleCategoryUpdateDTO;
import cn.lzx.blog.dto.admin.ArticleStatusUpdateDTO;
import cn.lzx.blog.dto.admin.ArticleTagUpdateDTO;
import cn.lzx.blog.dto.admin.CategoryManageDTO;
import cn.lzx.blog.dto.admin.CommentStatusUpdateDTO;
import cn.lzx.blog.dto.admin.TagManageDTO;
import cn.lzx.blog.vo.CategoryVO;
import cn.lzx.blog.vo.TagVO;
import cn.lzx.blog.mapper.*;
import cn.lzx.blog.service.AdminService;
import cn.lzx.blog.service.ArticleService;
import cn.lzx.blog.vo.admin.CommentManageVO;
import cn.lzx.blog.vo.admin.StatisticsVO;
import cn.lzx.blog.vo.admin.UserManageVO;
import cn.lzx.constants.CommonConstants;
import cn.lzx.entity.*;
import cn.lzx.exception.CommonException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员Service实现类
 *
 * @author lzx
 * @since 2025-11-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final ArticleService articleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticleCategory(ArticleCategoryUpdateDTO dto) {
        // 1. 验证文章是否存在
        Article article = articleMapper.selectById(dto.getArticleId());
        if (article == null) {
            throw new CommonException("文章不存在");
        }

        // 2. 验证分类是否存在
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new CommonException("分类不存在");
        }

        // 3. 更新文章分类
        Article updateArticle = Article.builder()
                .id(dto.getArticleId())
                .categoryId(dto.getCategoryId())
                .build();
        articleMapper.updateById(updateArticle);

        log.info("管理员更新文章[{}]分类为[{}]成功", dto.getArticleId(), dto.getCategoryId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticleTags(ArticleTagUpdateDTO dto) {
        // 1. 验证文章是否存在
        Article article = articleMapper.selectById(dto.getArticleId());
        if (article == null) {
            throw new CommonException("文章不存在");
        }

        // 2. 删除原有标签关联
        LambdaQueryWrapper<ArticleTag> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ArticleTag::getArticleId, dto.getArticleId());
        articleTagMapper.delete(deleteWrapper);

        // 3. 如果提供了新标签，则添加新的标签关联
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            // 验证所有标签是否存在
            List<Tag> tags = tagMapper.selectBatchIds(dto.getTagIds());
            if (tags.size() != dto.getTagIds().size()) {
                throw new CommonException("部分标签不存在");
            }

            // 添加新的标签关联
            List<ArticleTag> articleTags = new ArrayList<>();
            for (Long tagId : dto.getTagIds()) {
                ArticleTag articleTag = ArticleTag.builder()
                        .articleId(dto.getArticleId())
                        .tagId(tagId)
                        .build();
                articleTags.add(articleTag);
            }
            // 批量插入
            for (ArticleTag articleTag : articleTags) {
                articleTagMapper.insert(articleTag);
            }
        }

        log.info("管理员更新文章[{}]标签成功，新标签ID列表: {}", dto.getArticleId(), dto.getTagIds());
    }

    @Override
    public Page<cn.lzx.blog.vo.ArticleListVO> getArticleList(Long current, Long size, String keyword, Integer status) {
        Page<Article> page = new Page<>(current, size);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

        // 状态筛选
        if (status != null) {
            wrapper.eq(Article::getStatus, status);
        } else {
            // 如果不指定状态，排除已删除的（逻辑删除）
            // MyBatis-Plus会自动处理逻辑删除
        }

        // 关键词搜索（标题）
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Article::getTitle, keyword);
        }

        // 按创建时间倒序
        wrapper.orderByDesc(Article::getCreateTime);

        IPage<Article> articlePage = articleMapper.selectPage(page, wrapper);

        // 转换为VO
        List<cn.lzx.blog.vo.ArticleListVO> voList = convertToArticleListVO(articlePage.getRecords());
        Page<cn.lzx.blog.vo.ArticleListVO> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 转换为ArticleListVO列表（管理员用）
     */
    private List<cn.lzx.blog.vo.ArticleListVO> convertToArticleListVO(List<Article> articles) {
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
        List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
        Map<Long, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        // 批量查询文章标签
        List<Long> articleIds = articles.stream()
                .map(Article::getId)
                .collect(Collectors.toList());
        Map<Long, List<Tag>> articleTagsMap = new HashMap<>();
        for (Long articleId : articleIds) {
            List<Tag> tags = tagMapper.selectByArticleId(articleId);
            articleTagsMap.put(articleId, tags);
        }

        // 转换为VO
        return articles.stream()
                .map(article -> {
                    User author = userMap.get(article.getUserId());
                    Category category = categoryMap.get(article.getCategoryId());
                    List<Tag> tags = articleTagsMap.getOrDefault(article.getId(), new ArrayList<>());

                    List<cn.lzx.blog.vo.TagVO> tagVOList = tags.stream()
                            .map(tag -> cn.lzx.blog.vo.TagVO.builder()
                                    .id(tag.getId())
                                    .name(tag.getName())
                                    .build())
                            .collect(Collectors.toList());

                    return cn.lzx.blog.vo.ArticleListVO.builder()
                            .id(article.getId())
                            .title(article.getTitle())
                            .summary(article.getSummary())
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
                            .status(article.getStatus())
                            .createTime(article.getCreateTime())
                            .updateTime(article.getUpdateTime())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticleStatus(ArticleStatusUpdateDTO dto) {
        // 验证文章是否存在
        Article article = articleMapper.selectById(dto.getArticleId());
        if (article == null) {
            throw new CommonException("文章不存在");
        }

        // 验证状态值
        if (dto.getStatus() != CommonConstants.ARTICLE_STATUS_DRAFT
                && dto.getStatus() != CommonConstants.ARTICLE_STATUS_PUBLISHED
                && dto.getStatus() != CommonConstants.ARTICLE_STATUS_BLOCKED) {
            throw new CommonException("状态值无效");
        }

        // 更新文章状态
        Article updateArticle = Article.builder()
                .id(dto.getArticleId())
                .status(dto.getStatus())
                .build();
        articleMapper.updateById(updateArticle);

        String statusText = dto.getStatus() == CommonConstants.ARTICLE_STATUS_BLOCKED ? "屏蔽" : "取消屏蔽";
        log.info("管理员{}文章[{}]成功", statusText, dto.getArticleId());
    }

    @Override
    public Page<UserManageVO> getUserList(Long current, Long size, String keyword) {
        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索（用户名/昵称/邮箱）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }

        // 按创建时间倒序
        wrapper.orderByDesc(User::getCreateTime);

        IPage<User> userPage = userMapper.selectPage(page, wrapper);

        // 转换为VO
        List<UserManageVO> voList = userPage.getRecords().stream()
                .map(user -> {
                    // 查询用户的文章数
                    LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
                    articleWrapper.eq(Article::getUserId, user.getId());
                    Long articleCount = articleMapper.selectCount(articleWrapper).longValue();

                    // 查询用户的评论数
                    LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
                    commentWrapper.eq(Comment::getUserId, user.getId());
                    Long commentCount = commentMapper.selectCount(commentWrapper).longValue();

                    return UserManageVO.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .nickname(user.getNickname())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .avatar(user.getAvatar())
                            .intro(user.getIntro())
                            .status(user.getStatus())
                            .articleCount(articleCount)
                            .commentCount(commentCount)
                            .createTime(user.getCreateTime())
                            .updateTime(user.getUpdateTime())
                            .build();
                })
                .collect(Collectors.toList());

        Page<UserManageVO> resultPage = new Page<>(current, size, userPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new CommonException("用户不存在");
        }

        // 验证状态值
        if (status != CommonConstants.USER_STATUS_NORMAL && status != CommonConstants.USER_STATUS_DISABLED) {
            throw new CommonException("状态值无效");
        }

        // 更新用户状态
        User updateUser = User.builder()
                .id(userId)
                .status(status)
                .build();
        userMapper.updateById(updateUser);

        log.info("管理员更新用户[{}]状态为[{}]成功", userId, status);
    }

    @Override
    public Page<CommentManageVO> getCommentList(Long current, Long size, Integer status, String keyword) {
        Page<Comment> page = new Page<>(current, size);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();

        // 状态筛选
        if (status != null) {
            wrapper.eq(Comment::getStatus, status);
        }

        // 关键词搜索（评论内容）
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Comment::getContent, keyword);
        }

        // 按创建时间倒序
        wrapper.orderByDesc(Comment::getCreateTime);

        IPage<Comment> commentPage = commentMapper.selectPage(page, wrapper);

        // 获取所有文章ID和用户ID
        List<Long> articleIds = commentPage.getRecords().stream()
                .map(Comment::getArticleId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> userIds = commentPage.getRecords().stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> toUserIds = commentPage.getRecords().stream()
                .filter(c -> c.getToUserId() != null)
                .map(Comment::getToUserId)
                .distinct()
                .collect(Collectors.toList());
        userIds.addAll(toUserIds);

        // 批量查询文章和用户
        List<Article> articles = articleIds.isEmpty() ? new ArrayList<>() : articleMapper.selectBatchIds(articleIds);
        List<User> users = userIds.isEmpty() ? new ArrayList<>() : userMapper.selectBatchIds(userIds.stream().distinct().collect(Collectors.toList()));

        // 构建映射
        java.util.Map<Long, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, article -> article));
        java.util.Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 转换为VO
        List<CommentManageVO> voList = commentPage.getRecords().stream()
                .map(comment -> {
                    Article article = articleMap.get(comment.getArticleId());
                    User user = userMap.get(comment.getUserId());
                    User toUser = comment.getToUserId() != null ? userMap.get(comment.getToUserId()) : null;

                    return CommentManageVO.builder()
                            .id(comment.getId())
                            .articleId(comment.getArticleId())
                            .articleTitle(article != null ? article.getTitle() : null)
                            .userId(comment.getUserId())
                            .userNickname(user != null ? user.getNickname() : null)
                            .userAvatar(user != null ? user.getAvatar() : null)
                            .content(comment.getContent())
                            .parentId(comment.getParentId())
                            .rootId(comment.getRootId())
                            .toUserId(comment.getToUserId())
                            .toUserNickname(toUser != null ? toUser.getNickname() : null)
                            .likeCount(comment.getLikeCount())
                            .status(comment.getStatus())
                            .createTime(comment.getCreateTime())
                            .updateTime(comment.getUpdateTime())
                            .build();
                })
                .collect(Collectors.toList());

        Page<CommentManageVO> resultPage = new Page<>(current, size, commentPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCommentStatus(CommentStatusUpdateDTO dto) {
        // 验证评论是否存在
        Comment comment = commentMapper.selectById(dto.getCommentId());
        if (comment == null) {
            throw new CommonException("评论不存在");
        }

        // 验证状态值
        if (dto.getStatus() != CommonConstants.COMMENT_STATUS_NORMAL
                && dto.getStatus() != CommonConstants.COMMENT_STATUS_HIDDEN) {
            throw new CommonException("状态值无效");
        }

        // 更新评论状态
        Comment updateComment = Comment.builder()
                .id(dto.getCommentId())
                .status(dto.getStatus())
                .build();
        commentMapper.updateById(updateComment);

        log.info("管理员更新评论[{}]状态为[{}]成功", dto.getCommentId(), dto.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        // 验证评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new CommonException("评论不存在");
        }

        // 逻辑删除评论
        Comment updateComment = Comment.builder()
                .id(commentId)
                .deleted(CommonConstants.DELETED)
                .build();
        commentMapper.updateById(updateComment);

        // 减少文章评论数
        articleService.decrementCommentCount(comment.getArticleId());

        log.info("管理员删除评论[{}]成功", commentId);
    }

    @Override
    public StatisticsVO getStatistics() {
        // 文章统计
        LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
        Long articleCount = articleMapper.selectCount(articleWrapper).longValue();

        LambdaQueryWrapper<Article> publishedWrapper = new LambdaQueryWrapper<>();
        publishedWrapper.eq(Article::getStatus, CommonConstants.ARTICLE_STATUS_PUBLISHED);
        Long publishedArticleCount = articleMapper.selectCount(publishedWrapper).longValue();

        Long draftArticleCount = articleCount - publishedArticleCount;

        // 用户统计
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        Long userCount = userMapper.selectCount(userWrapper).longValue();

        LambdaQueryWrapper<User> normalUserWrapper = new LambdaQueryWrapper<>();
        normalUserWrapper.eq(User::getStatus, CommonConstants.USER_STATUS_NORMAL);
        Long normalUserCount = userMapper.selectCount(normalUserWrapper).longValue();

        Long disabledUserCount = userCount - normalUserCount;

        // 评论统计
        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        Long commentCount = commentMapper.selectCount(commentWrapper).longValue();

        LambdaQueryWrapper<Comment> normalCommentWrapper = new LambdaQueryWrapper<>();
        normalCommentWrapper.eq(Comment::getStatus, CommonConstants.COMMENT_STATUS_NORMAL);
        Long normalCommentCount = commentMapper.selectCount(normalCommentWrapper).longValue();

        Long hiddenCommentCount = commentCount - normalCommentCount;

        // 访问量、点赞数、收藏数统计（需要查询所有文章）
        List<Article> allArticles = articleMapper.selectList(new LambdaQueryWrapper<>());
        Long totalViewCount = allArticles.stream()
                .mapToLong(article -> article.getViewCount() != null ? article.getViewCount().longValue() : 0L)
                .sum();
        Long totalLikeCount = allArticles.stream()
                .mapToLong(article -> article.getLikeCount() != null ? article.getLikeCount().longValue() : 0L)
                .sum();
        Long totalCollectCount = allArticles.stream()
                .mapToLong(article -> article.getCollectCount() != null ? article.getCollectCount().longValue() : 0L)
                .sum();

        // 分类统计
        LambdaQueryWrapper<Category> categoryWrapper = new LambdaQueryWrapper<>();
        Long categoryCount = categoryMapper.selectCount(categoryWrapper).longValue();

        // 标签统计
        LambdaQueryWrapper<Tag> tagWrapper = new LambdaQueryWrapper<>();
        Long tagCount = tagMapper.selectCount(tagWrapper).longValue();

        return StatisticsVO.builder()
                .articleCount(articleCount)
                .publishedArticleCount(publishedArticleCount)
                .draftArticleCount(draftArticleCount)
                .userCount(userCount)
                .normalUserCount(normalUserCount)
                .disabledUserCount(disabledUserCount)
                .commentCount(commentCount)
                .normalCommentCount(normalCommentCount)
                .hiddenCommentCount(hiddenCommentCount)
                .totalViewCount(totalViewCount)
                .totalLikeCount(totalLikeCount)
                .totalCollectCount(totalCollectCount)
                .categoryCount(categoryCount)
                .tagCount(tagCount)
                .build();
    }

    // ==================== 分类管理 ====================

    @Override
    public java.util.List<CategoryVO> getCategoryList() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSort);
        List<Category> categories = categoryMapper.selectList(wrapper);

        // 查询每个分类的文章数
        return categories.stream()
                .map(category -> {
                    LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
                    articleWrapper.eq(Article::getCategoryId, category.getId());
                    Long articleCount = articleMapper.selectCount(articleWrapper).longValue();

                    return CategoryVO.builder()
                            .id(category.getId())
                            .name(category.getName())
                            .sort(category.getSort())
                            .articleCount(articleCount.intValue())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CategoryManageDTO dto) {
        // 检查分类名称是否已存在
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, dto.getName());
        Category existCategory = categoryMapper.selectOne(wrapper);
        if (existCategory != null) {
            throw new CommonException("分类名称已存在");
        }

        Category category = Category.builder()
                .name(dto.getName())
                .sort(dto.getSort())
                .build();
        categoryMapper.insert(category);

        log.info("管理员创建分类成功: {}, ID: {}", dto.getName(), category.getId());
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(CategoryManageDTO dto) {
        // 验证分类是否存在
        Category category = categoryMapper.selectById(dto.getId());
        if (category == null) {
            throw new CommonException("分类不存在");
        }

        // 检查分类名称是否与其他分类重复
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, dto.getName())
                .ne(Category::getId, dto.getId());
        Category existCategory = categoryMapper.selectOne(wrapper);
        if (existCategory != null) {
            throw new CommonException("分类名称已存在");
        }

        Category updateCategory = Category.builder()
                .id(dto.getId())
                .name(dto.getName())
                .sort(dto.getSort())
                .build();
        categoryMapper.updateById(updateCategory);

        log.info("管理员更新分类成功: ID={}, name={}", dto.getId(), dto.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long categoryId) {
        // 验证分类是否存在
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new CommonException("分类不存在");
        }

        // 检查是否有文章使用该分类
        LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.eq(Article::getCategoryId, categoryId);
        Long articleCount = articleMapper.selectCount(articleWrapper).longValue();
        if (articleCount > 0) {
            throw new CommonException("该分类下还有" + articleCount + "篇文章，无法删除");
        }

        // 逻辑删除分类
        categoryMapper.deleteById(categoryId);

        log.info("管理员删除分类成功: ID={}", categoryId);
    }

    // ==================== 标签管理 ====================

    @Override
    public java.util.List<TagVO> getTagList() {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getCreateTime);
        List<Tag> tags = tagMapper.selectList(wrapper);

        // 查询每个标签的文章数
        return tags.stream()
                .map(tag -> {
                    LambdaQueryWrapper<ArticleTag> articleTagWrapper = new LambdaQueryWrapper<>();
                    articleTagWrapper.eq(ArticleTag::getTagId, tag.getId());
                    Long articleCount = articleTagMapper.selectCount(articleTagWrapper).longValue();

                    return TagVO.builder()
                            .id(tag.getId())
                            .name(tag.getName())
                            .articleCount(articleCount.intValue())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTag(TagManageDTO dto) {
        // 检查标签名称是否已存在
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getName, dto.getName());
        Tag existTag = tagMapper.selectOne(wrapper);
        if (existTag != null) {
            throw new CommonException("标签名称已存在");
        }

        Tag tag = Tag.builder()
                .name(dto.getName())
                .build();
        tagMapper.insert(tag);

        log.info("管理员创建标签成功: {}, ID: {}", dto.getName(), tag.getId());
        return tag.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTag(TagManageDTO dto) {
        // 验证标签是否存在
        Tag tag = tagMapper.selectById(dto.getId());
        if (tag == null) {
            throw new CommonException("标签不存在");
        }

        // 检查标签名称是否与其他标签重复
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getName, dto.getName())
                .ne(Tag::getId, dto.getId());
        Tag existTag = tagMapper.selectOne(wrapper);
        if (existTag != null) {
            throw new CommonException("标签名称已存在");
        }

        Tag updateTag = Tag.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
        tagMapper.updateById(updateTag);

        log.info("管理员更新标签成功: ID={}, name={}", dto.getId(), dto.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long tagId) {
        // 验证标签是否存在
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new CommonException("标签不存在");
        }

        // 删除标签与文章的关联关系
        LambdaQueryWrapper<ArticleTag> articleTagWrapper = new LambdaQueryWrapper<>();
        articleTagWrapper.eq(ArticleTag::getTagId, tagId);
        articleTagMapper.delete(articleTagWrapper);

        // 逻辑删除标签
        tagMapper.deleteById(tagId);

        log.info("管理员删除标签成功: ID={}", tagId);
    }
}

