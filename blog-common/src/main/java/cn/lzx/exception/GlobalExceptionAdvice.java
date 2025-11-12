package cn.lzx.exception;

import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import cn.lzx.constants.CommonConstants;
import cn.lzx.utils.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器
 *
 * @author lzx
 * @since 2025-10-31
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionAdvice {

    // ==================== 自定义业务异常 ====================

    /**
     * 处理自定义业务异常（BusinessException 和 JwtException）
     * 两者都继承自 BaseException，统一处理
     */
    @ExceptionHandler({ BusinessException.class, JwtException.class })
    public R handleBaseException(BaseException e) {
        String exceptionType = e instanceof JwtException ? "JWT异常" : "业务异常";
        log.warn("{}: {}", exceptionType, e.getMsg());
        return R.fail(e.getCode(), e.getMsg());
    }

    // ==================== 参数校验异常 ====================

    /**
     * 处理参数校验异常（@RequestBody + @Valid/@Validated）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = extractValidationErrors(e.getBindingResult().getAllErrors()
                .stream()
                .map(ObjectError::getDefaultMessage));
        log.warn("参数校验失败: {}", message);
        return R.fail(CommonConstants.FAIL, message);
    }

    /**
     * 处理约束违规异常（@PathVariable、@RequestParam参数）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R handleConstraintViolationException(ConstraintViolationException e) {
        String message = extractValidationErrors(e.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage));
        log.warn("参数校验失败: {}", message);
        return R.fail(CommonConstants.FAIL, message);
    }

    /**
     * 提取校验错误信息
     */
    private String extractValidationErrors(java.util.stream.Stream<String> errorStream) {
        return errorStream.collect(Collectors.joining("; "));
    }

    // ==================== 文件上传异常 ====================

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件上传大小超限: {}", e.getMessage());
        long maxSize = e.getMaxUploadSize();
        String message = maxSize > 0
                ? "上传文件大小超出限制，最大允许 " + (maxSize / 1024 / 1024) + "MB"
                : "上传文件大小超出限制，请上传更小的文件";
        return R.fail(message);
    }

    /**
     * 处理文件类型不支持异常
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public R handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("不支持的文件类型: {}", e.getContentType());
        return R.fail("不支持的文件类型，请上传正确格式的文件");
    }

    /**
     * 处理 multipart 解析异常
     */
    @ExceptionHandler(MultipartException.class)
    public R handleMultipartException(MultipartException e) {
        // 优先处理文件大小超限
        if (e.getCause() instanceof MaxUploadSizeExceededException) {
            return handleMaxUploadSizeExceededException((MaxUploadSizeExceededException) e.getCause());
        }

        // 根据异常消息判断具体原因
        String errorMsg = e.getMessage();
        if (errorMsg != null) {
            if (errorMsg.contains("size")) {
                log.warn("文件上传失败: 文件过大");
                return R.fail("上传文件过大，请压缩后重试");
            }
            if (errorMsg.contains("corrupt") || errorMsg.contains("invalid")) {
                log.warn("文件上传失败: 文件损坏或格式不正确");
                return R.fail("文件已损坏或格式不正确，请重新选择");
            }
            if (errorMsg.contains("permission") || errorMsg.contains("denied")) {
                log.error("文件上传失败: 服务器权限不足");
                return R.fail("服务器文件上传权限不足，请联系管理员");
            }
        }

        log.warn("文件上传失败: {}", errorMsg);
        return R.fail("文件上传失败，请检查文件是否正确");
    }

    // ==================== 权限相关异常 ====================

    /**
     * 处理访问拒绝异常（无权限）
     */
    @ExceptionHandler(AccessDeniedException.class)
    public R handleAccessDeniedException(AccessDeniedException e) {
        log.warn("访问拒绝: {}", e.getMessage());
        return R.forbidden("无权限访问该资源");
    }

    // ==================== 系统异常 ====================

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public R handleNullPointerException(NullPointerException e) {
        log.error("空指针异常: {}", e.getMessage() != null ? e.getMessage() : "未知位置", e);
        return R.fail("系统内部错误");
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return R.fail(e.getMessage());
    }

    /**
     * 处理静态资源未找到异常（过滤favicon等）
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public R handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        String path = e.getResourcePath();
        // 忽略favicon等静态资源404
        if (path != null && (path.contains("favicon") || path.contains("doc.html"))) {
            return R.notFound();
        }
        log.warn("资源未找到: {}", path);
        return R.notFound("请求的资源不存在");
    }

    // ==================== 兜底异常 ====================

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public R handleException(Exception e) {
        log.error("系统异常: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return R.fail("系统异常，请联系管理员");
    }
}
