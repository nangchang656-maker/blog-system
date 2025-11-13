package cn.lzx.blog.service.impl;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.lzx.blog.dto.PasswordUpdateDTO;
import cn.lzx.blog.dto.UserLoginDTO;
import cn.lzx.blog.dto.UserRegisterDTO;
import cn.lzx.blog.dto.UserUpdateDTO;
import cn.lzx.blog.mapper.ArticleMapper;
import cn.lzx.blog.mapper.CollectMapper;
import cn.lzx.blog.mapper.UserMapper;
import cn.lzx.blog.service.FileUploadService;
import cn.lzx.blog.service.UserService;
import cn.lzx.blog.vo.UserInfoVO;
import cn.lzx.blog.vo.UserLoginVO;
import cn.lzx.constants.CommonConstants;
import cn.lzx.entity.Article;
import cn.lzx.entity.Collect;
import cn.lzx.entity.User;
import cn.lzx.enums.RedisKeyEnum;
import cn.lzx.exception.BusinessException;
import cn.lzx.service.TokenService;
import cn.lzx.utils.EmailSendUtil;
import cn.lzx.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final CollectMapper collectMapper;
    private final RedisUtil redisUtil;
    private final EmailSendUtil emailSendUtil;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final FileUploadService fileUploadService;

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

        // 4. BCrypt加密存储（密码当前为HTTP明文传输，生产环境建议配置HTTPS）
        String encryptedPassword = passwordEncoder.encode(dto.getPassword());

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

        // 3. 验证密码（密码当前为HTTP明文传输，生产环境建议配置HTTPS）
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 5. 生成Token
        Map<String, String> tokenMap = tokenService.createToken(user.getId(), user.getUsername());

        // 6. 构建返回数据
        UserInfoVO userInfo = convertToUserInfoVO(user);
        UserLoginVO loginVO = UserLoginVO.builder()
                .accessToken(tokenMap.get("accessToken"))
                .refreshToken(tokenMap.get("refreshToken"))
                .userInfo(userInfo)
                .build();

        log.info("用户登录成功: {}", user.getUsername());
        return loginVO;
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        // 尝试从缓存获取用户信息
        String cacheKey = RedisKeyEnum.KEY_USER_CACHE.getKey(userId);
        Object cached = redisUtil.get(cacheKey);
        if (cached != null && cached instanceof UserInfoVO) {
            log.debug("从缓存获取用户信息: userId={}", userId);
            return (UserInfoVO) cached;
        }

        // 缓存未命中，从数据库查询
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        UserInfoVO userInfo = convertToUserInfoVO(user);
        
        // 存入缓存（30分钟过期）
        redisUtil.set(cacheKey, userInfo, RedisKeyEnum.KEY_USER_CACHE.getExpire(), TimeUnit.SECONDS);
        log.debug("用户信息已存入缓存: userId={}", userId);
        
        return userInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UserUpdateDTO dto) {
        // 1. 查询用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. 更新用户信息（邮箱和头像不更新）
        User updateUser = User.builder()
                .id(userId)
                .nickname(dto.getNickname())
                .phone(dto.getPhone())
                .intro(dto.getIntro())
                .build();

        userMapper.updateById(updateUser);

        // 清除用户信息缓存
        String cacheKey = RedisKeyEnum.KEY_USER_CACHE.getKey(userId);
        redisUtil.delete(cacheKey);

        log.info("用户信息更新成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(MultipartFile file, Long userId) {
        // 1. 上传文件到MinIO
        String fileUrl = fileUploadService.uploadAvatar(file, userId);
        // 2. 更新数据库中的头像URL
        updateAvatar(userId, fileUrl);
        return fileUrl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAvatar(Long userId, String avatarUrl) {
        // 1. 查询用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. 更新头像URL
        User updateUser = User.builder()
                .id(userId)
                .avatar(avatarUrl)
                .build();

        userMapper.updateById(updateUser);

        // 清除用户信息缓存
        String cacheKey = RedisKeyEnum.KEY_USER_CACHE.getKey(userId);
        redisUtil.delete(cacheKey);

        log.info("用户头像更新成功: userId={}, avatarUrl={}", userId, avatarUrl);
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

        // 3. BCrypt加密并更新（密码当前为HTTP明文传输，生产环境建议配置HTTPS）
        String encryptedPassword = passwordEncoder.encode(dto.getNewPassword());
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
        // 1. 统计文章数（已发布的）
        LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.eq(Article::getUserId, user.getId())
                .eq(Article::getStatus, CommonConstants.ARTICLE_STATUS_PUBLISHED);
        Long articleCount = articleMapper.selectCount(articleWrapper);

        // 2. 统计收藏数（用户收藏的文章数量）
        LambdaQueryWrapper<Collect> collectWrapper = new LambdaQueryWrapper<>();
        collectWrapper.eq(Collect::getUserId, user.getId());
        Long collectCount = collectMapper.selectCount(collectWrapper);

        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .intro(user.getIntro())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .articleCount(articleCount)
                .collectCount(collectCount)
                .build();
    }
}
