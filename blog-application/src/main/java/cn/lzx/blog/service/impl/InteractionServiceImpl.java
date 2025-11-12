package cn.lzx.blog.service.impl;

import cn.lzx.blog.mapper.ArticleMapper;
import cn.lzx.blog.mapper.CategoryMapper;
import cn.lzx.blog.mapper.CollectMapper;
import cn.lzx.blog.mapper.CommentMapper;
import cn.lzx.blog.mapper.LikeRecordMapper;
import cn.lzx.blog.mapper.TagMapper;
import cn.lzx.blog.mapper.UserMapper;
import cn.lzx.blog.service.InteractionService;
import cn.lzx.blog.vo.ArticleListVO;
import cn.lzx.blog.vo.TagVO;
import cn.lzx.entity.Article;
import cn.lzx.entity.Category;
import cn.lzx.entity.Collect;
import cn.lzx.entity.Comment;
import cn.lzx.entity.LikeRecord;
import cn.lzx.entity.Tag;
import cn.lzx.entity.User;
import cn.lzx.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 互动Service实现类（点赞、收藏）
 *
 * @author lzx
 * @since 2025-11-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {

    private final ArticleMapper articleMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final CollectMapper collectMapper;
    private final CommentMapper commentMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeArticle(Long userId, Long articleId) {
        // 1. 验证文章是否存在
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 2. 查询是否存在点赞记录（包括已逻辑删除的）
        LikeRecord existRecord = likeRecordMapper.selectIncludeDeleted(userId, articleId, 1);

        if (existRecord != null) {
            // 2.1 如果记录存在且未删除，说明已经点赞过
            if (existRecord.getDeleted() == 0) {
                throw new BusinessException("您已经点赞过该文章");
            }

            // 2.2 如果记录存在但已删除，恢复该记录（将deleted改为0）
            int restored = likeRecordMapper.restoreDeleted(userId, articleId, 1);
            if (restored <= 0) {
                throw new BusinessException("点赞失败");
            }
            log.info("用户[{}]点赞文章[{}]成功（恢复已删除记录）", userId, articleId);
        } else {
            // 2.3 如果记录不存在，创建新记录
            LikeRecord likeRecord = LikeRecord.builder()
                    .userId(userId)
                    .targetId(articleId)
                    .type(1) // 1表示文章点赞
                    .build();

            int result = likeRecordMapper.insert(likeRecord);
            if (result <= 0) {
                throw new BusinessException("点赞失败");
            }
            log.info("用户[{}]点赞文章[{}]成功（新增记录）", userId, articleId);
        }

        // 3. 增加文章点赞数
        articleMapper.incrementLikeCount(articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeArticle(Long userId, Long articleId) {
        // 1. 查询点赞记录（只查未删除的）
        LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, articleId)
                .eq(LikeRecord::getType, 1); // 1表示文章点赞

        LikeRecord likeRecord = likeRecordMapper.selectOne(wrapper);
        if (likeRecord == null) {
            throw new BusinessException("您还未点赞该文章");
        }

        // 2. 逻辑删除点赞记录
        int result = likeRecordMapper.deleteById(likeRecord.getId());
        if (result <= 0) {
            throw new BusinessException("取消点赞失败");
        }

        // 3. 减少文章点赞数
        articleMapper.decrementLikeCount(articleId);

        log.info("用户[{}]取消点赞文章[{}]成功", userId, articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectArticle(Long userId, Long articleId) {
        // 1. 验证文章是否存在
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 2. 查询是否存在收藏记录（包括已逻辑删除的）
        Collect existCollect = collectMapper.selectIncludeDeleted(userId, articleId);

        if (existCollect != null) {
            // 2.1 如果记录存在且未删除，说明已经收藏过
            if (existCollect.getDeleted() == 0) {
                throw new BusinessException("您已经收藏过该文章");
            }

            // 2.2 如果记录存在但已删除，恢复该记录（将deleted改为0）
            int restored = collectMapper.restoreDeleted(userId, articleId);
            if (restored <= 0) {
                throw new BusinessException("收藏失败");
            }
            log.info("用户[{}]收藏文章[{}]成功（恢复已删除记录）", userId, articleId);
        } else {
            // 2.3 如果记录不存在，创建新记录
            Collect collect = Collect.builder()
                    .userId(userId)
                    .articleId(articleId)
                    .build();

            int result = collectMapper.insert(collect);
            if (result <= 0) {
                throw new BusinessException("收藏失败");
            }
            log.info("用户[{}]收藏文章[{}]成功（新增记录）", userId, articleId);
        }

        // 3. 增加文章收藏数
        articleMapper.incrementCollectCount(articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollectArticle(Long userId, Long articleId) {
        // 1. 查询收藏记录（只查未删除的）
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId)
                .eq(Collect::getArticleId, articleId);

        Collect collect = collectMapper.selectOne(wrapper);
        if (collect == null) {
            throw new BusinessException("您还未收藏该文章");
        }

        // 2. 逻辑删除收藏记录
        int result = collectMapper.deleteById(collect.getId());
        if (result <= 0) {
            throw new BusinessException("取消收藏失败");
        }

        // 3. 减少文章收藏数
        // TODO: 存放到redis中,通过定时任务更新数据库
        articleMapper.decrementCollectCount(articleId);

        log.info("用户[{}]取消收藏文章[{}]成功", userId, articleId);
    }

    @Override
    public boolean isLiked(Long userId, Long articleId) {
        LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, articleId)
                .eq(LikeRecord::getType, 1); // 1表示文章点赞
        return likeRecordMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean isCollected(Long userId, Long articleId) {
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId)
                .eq(Collect::getArticleId, articleId);
        return collectMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Page<ArticleListVO> getCollectedArticles(Long userId, int page, int size) {
        // 1. 创建分页对象
        Page<Collect> collectPage = new Page<>(page, size);

        // 2. 查询用户的收藏记录（按收藏时间倒序）
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId)
                .orderByDesc(Collect::getCreateTime);
        Page<Collect> collects = collectMapper.selectPage(collectPage, wrapper);

        // 3. 获取文章ID列表
        List<Long> articleIds = collects.getRecords().stream()
                .map(Collect::getArticleId)
                .collect(Collectors.toList());

        if (articleIds.isEmpty()) {
            return new Page<>(page, size, 0);
        }

        // 4. 查询文章信息（只查询已发布的文章）
        LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.in(Article::getId, articleIds)
                .eq(Article::getStatus, 1); // 只查询已发布的文章
        List<Article> articles = articleMapper.selectList(articleWrapper);

        // 5. 查询作者信息
        List<Long> authorIds = articles.stream()
                .map(Article::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<User> authors = userMapper.selectBatchIds(authorIds);
        Map<Long, User> authorMap = authors.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 6. 查询分类信息
        List<Long> categoryIds = articles.stream()
                .map(Article::getCategoryId)
                .distinct()
                .collect(Collectors.toList());
        List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
        Map<Long, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, category -> category));

        // 7. 查询标签信息
        Map<Long, List<Tag>> articleTagsMap = articles.stream()
                .collect(Collectors.toMap(
                        Article::getId,
                        article -> tagMapper.selectByArticleId(article.getId())
                ));

        // 8. 构建ArticleListVO列表
        List<ArticleListVO> articleVOList = articles.stream()
                .map(article -> {
                    User author = authorMap.get(article.getUserId());
                    Category category = categoryMap.get(article.getCategoryId());
                    List<Tag> tags = articleTagsMap.getOrDefault(article.getId(), List.of());

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
                            .authorName(author != null ? (author.getNickname() != null ? author.getNickname() : author.getUsername()) : null)
                            .authorAvatar(author != null ? author.getAvatar() : null)
                            .viewCount(article.getViewCount())
                            .likeCount(article.getLikeCount())
                            .commentCount(article.getCommentCount())
                            .createTime(article.getCreateTime())
                            .updateTime(article.getUpdateTime())
                            .build();
                })
                .collect(Collectors.toList());

        // 9. 构建返回分页对象
        Page<ArticleListVO> resultPage = new Page<>(page, size, collects.getTotal());
        resultPage.setRecords(articleVOList);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long userId, Long commentId) {
        // 1. 验证评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getDeleted() == 1) {
            throw new BusinessException("评论不存在");
        }

        // 2. 查询是否存在点赞记录（包括已逻辑删除的）
        LikeRecord existRecord = likeRecordMapper.selectIncludeDeleted(userId, commentId, 2);

        if (existRecord != null) {
            // 2.1 如果记录存在且未删除，说明已经点赞过
            if (existRecord.getDeleted() == 0) {
                throw new BusinessException("您已经点赞过该评论");
            }

            // 2.2 如果记录存在但已删除，恢复该记录（将deleted改为0）
            int restored = likeRecordMapper.restoreDeleted(userId, commentId, 2);
            if (restored <= 0) {
                throw new BusinessException("点赞失败");
            }
            log.info("用户[{}]点赞评论[{}]成功（恢复已删除记录）", userId, commentId);
        } else {
            // 2.3 如果记录不存在，创建新记录
            LikeRecord likeRecord = LikeRecord.builder()
                    .userId(userId)
                    .targetId(commentId)
                    .type(2) // 2表示评论点赞
                    .build();

            int result = likeRecordMapper.insert(likeRecord);
            if (result <= 0) {
                throw new BusinessException("点赞失败");
            }
            log.info("用户[{}]点赞评论[{}]成功（新增记录）", userId, commentId);
        }

        // 3. 增加评论点赞数
        commentMapper.incrementLikeCount(commentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long userId, Long commentId) {
        // 1. 查询点赞记录（只查未删除的）
        LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, commentId)
                .eq(LikeRecord::getType, 2); // 2表示评论点赞

        LikeRecord likeRecord = likeRecordMapper.selectOne(wrapper);
        if (likeRecord == null) {
            throw new BusinessException("您还未点赞该评论");
        }

        // 2. 逻辑删除点赞记录
        int result = likeRecordMapper.deleteById(likeRecord.getId());
        if (result <= 0) {
            throw new BusinessException("取消点赞失败");
        }

        // 3. 减少评论点赞数
        commentMapper.decrementLikeCount(commentId);

        log.info("用户[{}]取消点赞评论[{}]成功", userId, commentId);
    }

    @Override
    public boolean isCommentLiked(Long userId, Long commentId) {
        LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, commentId)
                .eq(LikeRecord::getType, 2); // 2表示评论点赞
        return likeRecordMapper.selectCount(wrapper) > 0;
    }
}
