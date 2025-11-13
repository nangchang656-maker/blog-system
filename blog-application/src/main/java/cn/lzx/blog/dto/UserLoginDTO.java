package cn.lzx.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录DTO
 */
@Data
public class UserLoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username; // 可以是用户名/邮箱/手机号

    @NotBlank(message = "密码不能为空")
    private String password; // 密码（当前HTTP明文传输，生产环境建议配置HTTPS）
}
