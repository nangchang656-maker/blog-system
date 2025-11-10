package cn.lzx.blog.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文章状态更新DTO（用于屏蔽/取消屏蔽）
 *
 * @author lzx
 * @since 2025-11-06
 */
@Data
public class ArticleStatusUpdateDTO {

    /**
     * 文章ID
     */
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    /**
     * 状态：0草稿，1已发布，4已屏蔽
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}

