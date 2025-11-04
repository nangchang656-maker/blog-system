package cn.lzx.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    private String bio;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
