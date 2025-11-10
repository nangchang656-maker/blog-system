package cn.lzx.blog.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.lzx.blog.dto.CommentCreateDTO;
import cn.lzx.blog.mapper.ArticleMapper;
import cn.lzx.blog.mapper.CommentMapper;
import cn.lzx.blog.mapper.LikeRecordMapper;
import cn.lzx.blog.mapper.UserMapper;
import cn.lzx.blog.service.ArticleService;
import cn.lzx.blog.service.CommentService;
import cn.lzx.blog.vo.CommentVO;
import cn.lzx.constants.CommonConstants;
import cn.lzx.entity.Article;
import cn.lzx.entity.Comment;
import cn.lzx.entity.User;
import cn.lzx.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 评论Service实现类
 *
 * @author lzx
 * @since 2025-11-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final ArticleService articleService;
    private final UserMapper userMapper;
    private final LikeRecordMapper likeRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(Long userId, CommentCreateDTO dto) {
        // 1. 验证文章是否存在
        Article article = articleMapper.selectById(dto.getArticleId());
        if (article == null) {
            throw new CommonException("文章不存在");
        }

        // 2. 处理父评论和根评论
        Long parentId = dto.getParentId() != null ? dto.getParentId() : 0L;
        Long rootId = 0L;
        Long toUserId = dto.getToUserId();

        if (parentId > 0) {
            // 回复评论：验证父评论是否存在
            Comment parentComment = commentMapper.selectById(parentId);
            if (parentComment == null || parentComment.getDeleted() == 1) {
                throw new CommonException("父评论不存在");
            }

            // 验证父评论是否属于同一篇文章
            if (!parentComment.getArticleId().equals(dto.getArticleId())) {
                throw new CommonException("父评论不属于该文章");
            }

            // 确定根评论ID
            rootId = parentComment.getRootId() > 0 ? parentComment.getRootId() : parentComment.getId();

            // 验证评论深度（限制不能太深）
            int currentDepth = calculateDepth(parentId, rootId);
            if (currentDepth >= CommonConstants.COMMENT_MAX_DEPTH) {
                throw new CommonException("评论深度不能超过" + CommonConstants.COMMENT_MAX_DEPTH + "层");
            }

            // 如果没有指定toUserId，默认回复父评论的作者
            if (toUserId == null) {
                toUserId = parentComment.getUserId();
            }
        }

        // 3. 创建评论
        Comment comment = Comment.builder()
                .articleId(dto.getArticleId())
                .userId(userId)
                .content(dto.getContent())
                .parentId(parentId)
                .rootId(rootId)
                .toUserId(toUserId)
                .likeCount(0)
                .status(CommonConstants.COMMENT_STATUS_NORMAL)
                .build();

        int result = commentMapper.insert(comment);
        if (result <= 0) {
            throw new CommonException("评论失败");
        }

        // 4. 增加文章评论数
        articleService.incrementCommentCount(dto.getArticleId());

        log.info("用户[{}]在文章[{}]下创建评论[{}]成功", userId, dto.getArticleId(), comment.getId());
        return comment.getId();
    }

    /**
     * 计算评论深度
     * 从父评论开始向上查找，直到根评论
     *
     * @param parentId 父评论ID
     * @param rootId   根评论ID
     * @return 深度（从1开始，1表示根评论的直接回复）
     */
    private int calculateDepth(Long parentId, Long rootId) {
        if (parentId == 0 || parentId.equals(rootId)) {
            return 1; // 根评论的直接回复，深度为1
        }

        int depth = 1;
        Long currentId = parentId;

        // 向上查找父评论链，直到根评论
        while (currentId != null && !currentId.equals(rootId) && currentId > 0) {
            Comment currentComment = commentMapper.selectById(currentId);
            if (currentComment == null || currentComment.getDeleted() == 1) {
                break;
            }

            depth++;
            currentId = currentComment.getParentId();

            // 防止无限循环，最多查找COMMENT_MAX_DEPTH次
            if (depth > CommonConstants.COMMENT_MAX_DEPTH) {
                break;
            }
        }

        return depth;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long commentId) {
        // 1. 查询评论
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getDeleted() == 1) {
            throw new CommonException("评论不存在");
        }

        // 2. 验证权限（只能删除自己的评论）
        if (!comment.getUserId().equals(userId)) {
            throw new CommonException("无权限删除此评论");
        }

        // 3. 逻辑删除评论
        int result = commentMapper.deleteById(commentId);
        if (result <= 0) {
            throw new CommonException("删除评论失败");
        }

        // 4. 减少文章评论数
        articleService.decrementCommentCount(comment.getArticleId());

        log.info("用户[{}]删除评论[{}]成功", userId, commentId);
    }

    @Override
    public List<CommentVO> getCommentList(Long articleId, Long userId) {
        // 1. 查询根评论列表（一级评论）
        List<Comment> rootComments = commentMapper.selectRootCommentsByArticleId(articleId);
        if (rootComments.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 查询所有根评论的回复
        List<Long> rootIds = rootComments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());

        List<Comment> allReplies = new ArrayList<>();
        for (Long rootId : rootIds) {
            List<Comment> replies = commentMapper.selectRepliesByRootId(rootId);
            allReplies.addAll(replies);
        }

        // 3. 获取所有评论的用户信息
        List<Long> allUserIds = new ArrayList<>();
        rootComments.forEach(c -> {
            allUserIds.add(c.getUserId());
            if (c.getToUserId() != null) {
                allUserIds.add(c.getToUserId());
            }
        });
        allReplies.forEach(c -> {
            allUserIds.add(c.getUserId());
            if (c.getToUserId() != null) {
                allUserIds.add(c.getToUserId());
            }
        });

        List<User> users = userMapper.selectBatchIds(allUserIds.stream().distinct().collect(Collectors.toList()));
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 4. 获取当前用户点赞的评论ID列表（如果已登录）
        List<Long> likedCommentIds = new ArrayList<>();
        if (userId != null) {
            // 这里需要查询like_record表，但LikeRecordMapper没有批量查询方法
            // 暂时使用循环查询，后续可以优化
            List<Long> allCommentIds = new ArrayList<>();
            rootComments.forEach(c -> allCommentIds.add(c.getId()));
            allReplies.forEach(c -> allCommentIds.add(c.getId()));

            for (Long commentId : allCommentIds) {
                LambdaQueryWrapper<cn.lzx.entity.LikeRecord> likeWrapper = new LambdaQueryWrapper<>();
                likeWrapper.eq(cn.lzx.entity.LikeRecord::getUserId, userId)
                        .eq(cn.lzx.entity.LikeRecord::getTargetId, commentId)
                        .eq(cn.lzx.entity.LikeRecord::getType, 2); // 2表示评论点赞
                if (likeRecordMapper.selectCount(likeWrapper) > 0) {
                    likedCommentIds.add(commentId);
                }
            }
        }

        // 5. 构建评论树形结构
        List<CommentVO> rootCommentVOs = rootComments.stream()
                .map(rootComment -> buildCommentVO(rootComment, userMap, likedCommentIds, allReplies))
                .collect(Collectors.toList());

        return rootCommentVOs;
    }

    /**
     * 构建评论VO（包括子评论）
     *
     * @param comment         评论实体
     * @param userMap         用户信息Map
     * @param likedCommentIds 已点赞的评论ID列表
     * @param allReplies      所有回复列表
     * @return 评论VO
     */
    private CommentVO buildCommentVO(Comment comment, Map<Long, User> userMap,
            List<Long> likedCommentIds, List<Comment> allReplies) {
        User user = userMap.get(comment.getUserId());
        User toUser = comment.getToUserId() != null ? userMap.get(comment.getToUserId()) : null;

        CommentVO.CommentVOBuilder builder = CommentVO.builder()
                .id(comment.getId())
                .articleId(comment.getArticleId())
                .userId(comment.getUserId())
                .userNickname(
                        user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : null)
                .userAvatar(user != null ? user.getAvatar() : null)
                .content(comment.getContent())
                .parentId(comment.getParentId())
                .rootId(comment.getRootId())
                .toUserId(comment.getToUserId())
                .toUserNickname(
                        toUser != null ? (toUser.getNickname() != null ? toUser.getNickname() : toUser.getUsername())
                                : null)
                .likeCount(comment.getLikeCount())
                .isLiked(likedCommentIds.contains(comment.getId()))
                .createTime(comment.getCreateTime());

        // 如果是根评论，查找其所有子评论
        if (comment.getParentId() == 0) {
            List<Comment> replies = allReplies.stream()
                    .filter(reply -> reply.getRootId().equals(comment.getId()))
                    .collect(Collectors.toList());

            List<CommentVO> replyVOs = replies.stream()
                    .map(reply -> buildCommentVO(reply, userMap, likedCommentIds, new ArrayList<>()))
                    .collect(Collectors.toList());

            builder.replies(replyVOs);
        }

        return builder.build();
    }
}
