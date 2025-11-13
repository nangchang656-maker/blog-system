package cn.lzx.blog.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 密码更新DTO
 * 需要邮箱验证码校验（与注册相同安全级别）
 */
@Data
public class PasswordUpdateDTO {

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码为6位")
    private String code; // 邮箱验证码

    @NotBlank(message = "新密码不能为空")
    private String newPassword; // 新密码（当前HTTP明文传输，生产环境建议配置HTTPS）
}
