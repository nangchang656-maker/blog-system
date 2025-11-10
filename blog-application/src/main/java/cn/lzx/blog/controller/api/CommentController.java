package cn.lzx.blog.controller.api;

import cn.lzx.annotation.NoLogin;
import cn.lzx.blog.dto.CommentCreateDTO;
import cn.lzx.blog.service.CommentService;
import cn.lzx.blog.service.InteractionService;
import cn.lzx.blog.vo.CommentVO;
import cn.lzx.utils.R;
import cn.lzx.utils.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论API控制器
 *
 * @author lzx
 * @since 2025-11-05
 */
@Slf4j
@Tag(name = "评论模块")
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final InteractionService interactionService;

    /**
     * 创建评论
     */
    @Operation(summary = "创建评论")
    @PostMapping("/create")
    public R createComment(@RequestBody @Valid CommentCreateDTO dto) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        Long commentId = commentService.createComment(userId, dto);
        return R.success(commentId);
    }

    /**
     * 删除评论
     */
    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public R deleteComment(@PathVariable("id") Long id) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        commentService.deleteComment(userId, id);
        return R.success("删除成功");
    }

    /**
     * 获取文章评论列表
     */
    @NoLogin
    @Operation(summary = "获取文章评论列表")
    @GetMapping("/article/{articleId}")
    public R getCommentList(@PathVariable("articleId") Long articleId) {
        // 尝试获取当前用户ID（可能为空，表示未登录）
        Long userId = null;
        try {
            userId = SecurityContextUtil.getCurrentUserId();
        } catch (Exception e) {
            // 未登录，忽略异常
            log.debug("用户未登录，查看评论列表: articleId={}", articleId);
        }

        List<CommentVO> commentList = commentService.getCommentList(articleId, userId);
        return R.success(commentList);
    }

    /**
     * 点赞评论
     */
    @Operation(summary = "点赞评论")
    @PostMapping("/{id}/like")
    public R likeComment(@PathVariable("id") Long id) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        interactionService.likeComment(userId, id);
        return R.success("点赞成功");
    }

    /**
     * 取消点赞评论
     */
    @Operation(summary = "取消点赞评论")
    @PostMapping("/{id}/unlike")
    public R unlikeComment(@PathVariable("id") Long id) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        interactionService.unlikeComment(userId, id);
        return R.success("取消点赞成功");
    }
}

