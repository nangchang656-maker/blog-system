package cn.lzx.blog.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 标签管理DTO
 *
 * @author lzx
 * @since 2025-11-06
 */
@Data
public class TagManageDTO {

    /**
     * 标签ID（更新时需要，新增时为空）
     */
    private Long id;

    /**
     * 标签名称
     */
    @NotBlank(message = "标签名称不能为空")
    private String name;
}

