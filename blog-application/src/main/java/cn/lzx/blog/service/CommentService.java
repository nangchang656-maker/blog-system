package cn.lzx.blog.service;

import cn.lzx.blog.dto.CommentCreateDTO;
import cn.lzx.blog.vo.CommentVO;

import java.util.List;

/**
 * 评论Service接口
 *
 * @author lzx
 * @since 2025-11-05
 */
public interface CommentService {

    /**
     * 创建评论
     *
     * @param userId 用户ID
     * @param dto    评论创建DTO
     * @return 评论ID
     */
    Long createComment(Long userId, CommentCreateDTO dto);

    /**
     * 删除评论
     *
     * @param userId    用户ID
     * @param commentId 评论ID
     */
    void deleteComment(Long userId, Long commentId);

    /**
     * 获取文章评论列表（树形结构）
     *
     * @param articleId 文章ID
     * @param userId     当前用户ID（可为空，用于判断是否点赞）
     * @return 评论列表
     */
    List<CommentVO> getCommentList(Long articleId, Long userId);
}

