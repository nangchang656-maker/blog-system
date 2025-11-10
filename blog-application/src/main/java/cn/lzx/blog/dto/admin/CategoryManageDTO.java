package cn.lzx.blog.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分类管理DTO
 *
 * @author lzx
 * @since 2025-11-06
 */
@Data
public class CategoryManageDTO {

    /**
     * 分类ID（更新时需要，新增时为空）
     */
    private Long id;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /**
     * 排序
     */
    @NotNull(message = "排序不能为空")
    private Integer sort;
}

