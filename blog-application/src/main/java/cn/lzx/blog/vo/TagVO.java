package cn.lzx.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签VO
 *
 * @author lzx
 * @since 2025-11-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagVO {

    /**
     * 标签ID
     */
    private Long id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 文章数量（可选，用于热门标签统计）
     */
    private Integer articleCount;
}
