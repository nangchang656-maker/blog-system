package cn.lzx.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginVO {

    private String token; // JWT accessToken (向后兼容，等同于accessToken)

    private String accessToken; // JWT accessToken

    private String refreshToken; // RefreshToken

    private UserInfoVO userInfo; // 用户信息
}
