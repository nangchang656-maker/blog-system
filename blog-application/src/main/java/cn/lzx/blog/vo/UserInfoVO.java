package cn.lzx.blog.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    /**
     * 个人简介
     */
    private String intro;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 文章数
     */
    private Long articleCount;

    /**
     * 收藏数（用户收藏的文章数量）
     */
    private Long collectCount;
}
