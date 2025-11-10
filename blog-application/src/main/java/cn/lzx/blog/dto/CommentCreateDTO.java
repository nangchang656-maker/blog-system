package cn.lzx.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建评论DTO
 *
 * @author lzx
 * @since 2025-11-05
 */
@Data
public class CommentCreateDTO {

    /**
     * 文章ID
     */
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000个字符")
    private String content;

    /**
     * 父评论ID（0表示一级评论）
     */
    private Long parentId;

    /**
     * 回复的用户ID（回复评论时需要）
     */
    private Long toUserId;
}

