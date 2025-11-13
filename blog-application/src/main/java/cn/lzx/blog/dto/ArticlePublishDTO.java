package cn.lzx.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 文章发布/编辑DTO
 *
 * @author lzx
 * @since 2025-11-04
 */
@Data
public class ArticlePublishDTO {

    /**
     * 文章ID（编辑时需要，发布时为空）
     */
    private Long id;

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    /**
     * 内容（Markdown格式）
     */
    @NotBlank(message = "内容不能为空")
    private String content;

    /**
     * 摘要
     */
    @Size(max = 500, message = "摘要长度不能超过500个字符")
    private String summary;

    /**
     * 大纲（Markdown格式）
     */
    private String outline;

    /**
     * 封面图URL
     */
    @Size(max = 255, message = "封面图URL长度不能超过255个字符")
    private String coverImage;

    /**
     * 分类ID（优先使用ID，如果为空则使用categoryName创建新分类）
     */
    private Long categoryId;

    /**
     * 分类名称（当categoryId为空时，使用此名称创建新分类）
     */
    private String categoryName;

    /**
     * 标签ID列表（优先使用ID，如果为空则使用tagNames创建新标签）
     */
    private List<Long> tagIds;

    /**
     * 标签名称列表（当tagIds为空时，使用这些名称创建新标签）
     */
    private List<String> tagNames;

    /**
     * 状态：0草稿，1发布
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
