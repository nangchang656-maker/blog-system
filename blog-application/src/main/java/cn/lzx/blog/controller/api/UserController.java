package cn.lzx.blog.controller.api;

import cn.lzx.annotation.NoLogin;
import cn.lzx.blog.dto.PasswordUpdateDTO;
import cn.lzx.blog.dto.UserLoginDTO;
import cn.lzx.blog.dto.UserRegisterDTO;
import cn.lzx.blog.dto.UserUpdateDTO;
import cn.lzx.blog.integration.storage.MinioUtil;
import cn.lzx.blog.service.UserService;
import cn.lzx.blog.vo.UserInfoVO;
import cn.lzx.blog.vo.UserLoginVO;
import cn.lzx.constants.SecurityConstants;
import cn.lzx.exception.BusinessException;
import cn.lzx.service.TokenService;
import cn.lzx.utils.R;
import cn.lzx.utils.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户API控制器
 */
@Slf4j
@Tag(name = "用户模块")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final MinioUtil minioUtil;
    private final TokenService tokenService;

    /**
     * 发送邮箱验证码
     */
    @NoLogin
    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/code/email")
    public R sendEmailCode(@RequestBody @Valid EmailCodeRequest request) {
        userService.sendEmailCode(request.getEmail());
        return R.success("验证码发送成功");
    }

    /**
     * 用户注册
     */
    @NoLogin
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R register(@RequestBody @Valid UserRegisterDTO dto) {
        userService.register(dto);
        return R.success("注册成功");
    }

    /**
     * 用户登录
     */
    @NoLogin
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R login(@RequestBody @Valid UserLoginDTO dto) {
        UserLoginVO loginVO = userService.login(dto);
        return R.success(loginVO);
    }

    /**
     * 刷新AccessToken
     */
    @NoLogin
    @Operation(summary = "刷新AccessToken")
    @PostMapping("/refresh-token")
    public R refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        Map<String, String> tokenMap = tokenService.refreshAccessToken(request.getUserId(), request.getRefreshToken());

        if (tokenMap == null) {
            throw new BusinessException("RefreshToken无效或已过期，请重新登录");
        }

        return R.success(tokenMap);
    }

    /**
     * 获取用户信息
     */
    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public R getUserInfo() {
        Long userId = SecurityContextUtil.getCurrentUserId();
        UserInfoVO userInfo = userService.getUserInfo(userId);
        return R.success(userInfo);
    }

    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户信息")
    @PutMapping("/info")
    public R updateUserInfo(@RequestBody @Valid UserUpdateDTO dto) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        userService.updateUserInfo(userId, dto);
        return R.success("更新成功");
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public R updatePassword(@RequestBody @Valid PasswordUpdateDTO dto, HttpServletRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        String token = extractToken(request);
        userService.updatePassword(userId, dto, token);
        return R.success("密码修改成功，请重新登录");
    }

    /**
     * 退出登录
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R logout(HttpServletRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        String token = extractToken(request);
        userService.logout(userId, token);
        return R.success("退出登录成功");
    }

    /**
     * 上传头像
     */
    @Operation(summary = "上传头像")
    @PostMapping("/avatar/upload")
    public R uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityContextUtil.getCurrentUserId();

        // 1. 校验文件
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 2. 校验文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isImageFile(originalFilename)) {
            throw new BusinessException("只支持上传 jpg、jpeg、png 格式的图片");
        }

        // 3. 校验文件大小（2MB）
        long maxSize = 2 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException("图片大小不能超过 2MB");
        }

        try {
            // 4. 生成固定文件名：avatars/user_{userId}_avatar.{ext}
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = String.format("avatars/user_%d_avatar%s", userId, fileExtension);

            // 5. 删除旧头像（如果存在不同格式的旧文件）
            deleteOldAvatars(userId, fileExtension);

            // 6. 上传到 MinIO（覆盖式上传）
            String fileUrl = minioUtil.upload(file.getInputStream(), fileName);

            // 7. 返回文件 URL
            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);

            log.info("用户头像上传成功: userId={}, url={}", userId, fileUrl);
            return R.success(result);
        } catch (Exception e) {
            log.error("头像上传失败: userId={}", userId, e);
            throw new BusinessException("头像上传失败，请重试");
        }
    }

    /**
     * 删除用户的旧头像文件（处理不同格式的情况）
     */
    private void deleteOldAvatars(Long userId, String currentExtension) {
        // 常见图片格式
        String[] extensions = {".jpg", ".jpeg", ".png"};

        for (String ext : extensions) {
            // 跳过当前要上传的格式（MinIO会自动覆盖）
            if (ext.equalsIgnoreCase(currentExtension)) {
                continue;
            }

            // 删除其他格式的旧头像
            String oldFileName = String.format("avatars/user_%d_avatar%s", userId, ext);
            minioUtil.deleteFile(oldFileName);
        }
    }

    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerCaseFilename = filename.toLowerCase();
        return lowerCaseFilename.endsWith(".jpg")
            || lowerCaseFilename.endsWith(".jpeg")
            || lowerCaseFilename.endsWith(".png");
    }

    /**
     * 从请求头中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(SecurityConstants.TOKEN_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 邮箱验证码请求对象
     */
    @lombok.Data
    public static class EmailCodeRequest {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
    }

    /**
     * 刷新Token请求对象
     */
    @lombok.Data
    public static class RefreshTokenRequest {
        @jakarta.validation.constraints.NotNull(message = "用户ID不能为空")
        private Long userId;

        @NotBlank(message = "RefreshToken不能为空")
        private String refreshToken;
    }
}
