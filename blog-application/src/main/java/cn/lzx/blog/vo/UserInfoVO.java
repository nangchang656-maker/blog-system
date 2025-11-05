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

    /**
     * 文章数
     */
    private Long articleCount;

    /**
     * 获赞数（用户所有文章的点赞数总和）
     */
    private Long likeCount;

    /**
     * 收藏数（用户收藏的文章数量）
     */
    private Long collectCount;

    /**
     * 关注数
     */
    private Long followCount;

    /**
     * 粉丝数
     */
    private Long fansCount;
}
