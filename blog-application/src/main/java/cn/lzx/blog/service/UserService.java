package cn.lzx.blog.service;

import cn.lzx.blog.dto.PasswordUpdateDTO;
import cn.lzx.blog.dto.UserLoginDTO;
import cn.lzx.blog.dto.UserRegisterDTO;
import cn.lzx.blog.dto.UserUpdateDTO;
import cn.lzx.blog.vo.UserInfoVO;
import cn.lzx.blog.vo.UserLoginVO;

/**
 * 用户Service接口
 */
public interface UserService {

    /**
     * 发送邮箱验证码
     * @param email 邮箱地址
     */
    void sendEmailCode(String email);

    /**
     * 用户注册
     * @param dto 注册信息
     */
    void register(UserRegisterDTO dto);

    /**
     * 用户登录
     * @param dto 登录信息
     * @return 登录响应（包含token和用户信息）
     */
    UserLoginVO login(UserLoginDTO dto);

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    UserInfoVO getUserInfo(Long userId);

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param dto 更新信息
     */
    void updateUserInfo(Long userId, UserUpdateDTO dto);

    /**
     * 修改密码
     * @param userId 用户ID
     * @param dto 密码更新信息
     * @param accessToken 当前访问令牌（用于加入黑名单）
     */
    void updatePassword(Long userId, PasswordUpdateDTO dto, String accessToken);

    /**
     * 退出登录
     * @param userId 用户ID
     * @param accessToken 当前访问令牌（用于加入黑名单）
     */
    void logout(Long userId, String accessToken);
}
