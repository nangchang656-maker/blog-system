package cn.lzx.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 刷新Token请求对象
 */
@Data
public class RefreshTokenRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "RefreshToken不能为空")
    private String refreshToken;
}

