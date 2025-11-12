package cn.lzx.blog.dto;

import lombok.Data;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户信息更新DTO
 * 注意：邮箱不能更新（唯一标识）
 */
@Data
public class UserUpdateDTO {

    @Size(min = 2, max = 20, message = "昵称长度为2-20个字符")
    private String nickname;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 200, message = "个人简介不能超过200字符")
    private String intro;
}
