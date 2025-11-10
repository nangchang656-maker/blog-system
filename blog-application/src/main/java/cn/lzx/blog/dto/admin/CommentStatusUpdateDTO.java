package cn.lzx.blog.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评论状态更新DTO
 *
 * @author lzx
 * @since 2025-11-06
 */
@Data
public class CommentStatusUpdateDTO {

    /**
     * 评论ID
     */
    @NotNull(message = "评论ID不能为空")
    private Long commentId;

    /**
     * 状态：1正常显示，2已隐藏-待审核
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}

