package cn.lzx.blog.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 文章标签综合修改DTO
 *
 * @author lzx
 * @since 2025-11-06
 */
@Data
public class ArticleTagUpdateDTO {

    /**
     * 文章ID
     */
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    /**
     * 标签ID列表
     */
    private List<Long> tagIds;
}

