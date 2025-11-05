package cn.lzx.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类VO
 *
 * @author lzx
 * @since 2025-11-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryVO {

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 描述（可选）
     */
    private String description;

    /**
     * 文章数量（可选，用于统计）
     */
    private Integer articleCount;

    /**
     * 排序
     */
    private Integer sort;
}
