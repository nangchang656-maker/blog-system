package cn.lzx.blog.service.impl;

import cn.lzx.blog.dto.PasswordUpdateDTO;
import cn.lzx.blog.dto.UserLoginDTO;
import cn.lzx.blog.dto.UserRegisterDTO;
import cn.lzx.blog.dto.UserUpdateDTO;
import cn.lzx.blog.mapper.UserMapper;
import cn.lzx.blog.service.UserService;
import cn.lzx.blog.vo.UserInfoVO;
import cn.lzx.blog.vo.UserLoginVO;
import cn.lzx.entity.User;
import cn.lzx.enums.RedisKeyEnum;
import cn.lzx.exception.BusinessException;
import cn.lzx.service.TokenService;
import cn.lzx.utils.AesUtil;
import cn.lzx.utils.EmailSendUtil;
import cn.lzx.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 用户Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RedisUtil redisUtil;
    private final EmailSendUtil emailSendUtil;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    public void sendEmailCode(String email) {
        // 1. 生成6位随机验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        // 2. 存储到Redis，5分钟过期
        String key = RedisKeyEnum.KEY_VERIFYCODE.getKey(email);
        redisUtil.set(key, code, RedisKeyEnum.KEY_VERIFYCODE.getExpire(), TimeUnit.SECONDS);

        // 3. 发送邮件
        emailSendUtil.sendVerificationCode(email, code);

        log.info("邮箱验证码发送成功: {}, 验证码: {}", email, code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO dto) {
        // 1. 验证用户名是否重复
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 2. 验证邮箱是否重复
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, dto.getEmail());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("邮箱已被注册");
        }

        // 3. 验证验证码
        String key = RedisKeyEnum.KEY_VERIFYCODE.getKey(dto.getEmail());
        Object redisCode = redisUtil.get(key);
        if (redisCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!redisCode.toString().equals(dto.getCode())) {
            throw new BusinessException("验证码错误");
        }

        // 4. AES解密密码
        String plainPassword;
        try {
            plainPassword = AesUtil.decrypt(dto.getPassword());
        } catch (Exception e) {
            log.error("密码解密失败", e);
            throw new BusinessException("密码格式错误");
        }

        // 5. BCrypt加密存储
        String encryptedPassword = passwordEncoder.encode(plainPassword);

        // 6. 创建用户
        User user = User.builder()
                .username(dto.getUsername())
                .password(encryptedPassword)
                .nickname(dto.getUsername()) // 默认昵称为用户名
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .status(1) // 默认正常状态
                .build();

        userMapper.insert(user);

        // 7. 验证码使用后删除
        redisUtil.delete(key);

        log.info("用户注册成功: {}", dto.getUsername());
    }

    @Override
    public UserLoginVO login(UserLoginDTO dto) {
        // 1. 查询用户（支持用户名/邮箱/手机号登录）
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername())
                .or().eq(User::getEmail, dto.getUsername());
                // .or().eq(User::getPhone, dto.getUsername()) // 当前手机号没进行短信验证,不具备一对一对应关系
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 3. AES解密密码
        String plainPassword;
        try {
            plainPassword = AesUtil.decrypt(dto.getPassword());
        } catch (Exception e) {
            log.error("密码解密失败", e);
            throw new BusinessException("密码格式错误");
        }

        // 4. 验证密码
        if (!passwordEncoder.matches(plainPassword, user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 5. 生成Token
        Map<String, String> tokenMap = tokenService.createToken(user.getId(), user.getUsername());

        // 6. 构建返回数据
        UserInfoVO userInfo = convertToUserInfoVO(user);
        UserLoginVO loginVO = UserLoginVO.builder()
                .token(tokenMap.get("accessToken"))  // 向后兼容
                .accessToken(tokenMap.get("accessToken"))
                .refreshToken(tokenMap.get("refreshToken"))
                .userInfo(userInfo)
                .build();

        log.info("用户登录成功: {}", user.getUsername());
        return loginVO;
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToUserInfoVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UserUpdateDTO dto) {
        // 1. 查询用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. 更新用户信息（邮箱不能更新）
        User updateUser = User.builder()
                .id(userId)
                .nickname(dto.getNickname())
                .phone(dto.getPhone())
                .avatar(dto.getAvatar())
                .intro(dto.getBio()) // DTO的bio对应数据库的intro
                .build();

        userMapper.updateById(updateUser);

        log.info("用户信息更新成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, PasswordUpdateDTO dto, String accessToken) {
        // 1. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. 验证邮箱验证码
        String key = RedisKeyEnum.KEY_VERIFYCODE.getKey(user.getEmail());
        Object redisCode = redisUtil.get(key);
        if (redisCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!redisCode.toString().equals(dto.getCode())) {
            throw new BusinessException("验证码错误");
        }

        // 3. AES解密新密码
        String plainPassword;
        try {
            plainPassword = AesUtil.decrypt(dto.getNewPassword());
        } catch (Exception e) {
            log.error("密码解密失败", e);
            throw new BusinessException("密码格式错误");
        }

        // 4. BCrypt加密并更新
        String encryptedPassword = passwordEncoder.encode(plainPassword);
        User updateUser = User.builder()
                .id(userId)
                .password(encryptedPassword)
                .build();
        userMapper.updateById(updateUser);

        // 5. 验证码使用后删除
        redisUtil.delete(key);

        // 6. 强制用户下线（修改密码后需要重新登录）
        tokenService.forceLogout(userId, accessToken);

        log.info("用户密码修改成功: userId={}", userId);
    }

    @Override
    public void logout(Long userId, String accessToken) {
        tokenService.logout(userId, accessToken);
        log.info("用户退出登录成功: userId={}", userId);
    }

    /**
     * 将User实体转换为UserInfoVO
     */
    private UserInfoVO convertToUserInfoVO(User user) {
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .bio(user.getIntro()) // 数据库的intro对应VO的bio
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }
}
