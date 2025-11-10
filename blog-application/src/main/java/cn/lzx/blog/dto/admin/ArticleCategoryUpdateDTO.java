package cn.lzx.blog.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文章分类重划分DTO
 *
 * @author lzx
 * @since 2025-11-06
 */
@Data
public class ArticleCategoryUpdateDTO {

    /**
     * 文章ID
     */
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    /**
     * 新分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
}

