package cn.lzx.blog.controller.api;

import cn.lzx.annotation.NoLogin;
import cn.lzx.blog.dto.PasswordUpdateDTO;
import cn.lzx.blog.dto.UserLoginDTO;
import cn.lzx.blog.dto.UserRegisterDTO;
import cn.lzx.blog.dto.UserUpdateDTO;
import cn.lzx.blog.service.FileUploadService;
import cn.lzx.blog.service.UserService;
import cn.lzx.blog.vo.UserInfoVO;
import cn.lzx.blog.vo.UserLoginVO;
import cn.lzx.exception.BusinessException;
import cn.lzx.service.TokenService;
import cn.lzx.utils.R;
import cn.lzx.utils.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final FileUploadService fileUploadService;
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
            throw new BusinessException("RefreshToken无效或已过期,请重新登录");
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
    public R updatePassword(@RequestBody @Valid PasswordUpdateDTO dto) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        String token = SecurityContextUtil.getToken();
        userService.updatePassword(userId, dto, token);
        return R.success("密码修改成功,请重新登录");
    }

    /**
     * 退出登录
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R logout() {
        Long userId = SecurityContextUtil.getCurrentUserId();
        String token = SecurityContextUtil.getToken();
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
        String fileUrl = fileUploadService.uploadAvatar(file, userId);

        Map<String, String> result = new HashMap<>();
        result.put("url", fileUrl);

        return R.success(result);
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
