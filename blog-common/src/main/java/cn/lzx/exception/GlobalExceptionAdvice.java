package cn.lzx.exception;

import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(CommonException.class)
    public R handleCommonException(CommonException e) {
        log.warn("业务异常: {}", e.getMsg());
        return R.fail(e.getCode(), e.getMsg());
    }

    /**
     * 处理业务异常（用户注册、登录等业务场景）
     */
    @ExceptionHandler(BusinessException.class)
    public R handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理JWT异常
     */
    @ExceptionHandler(JwtException.class)
    public R handleJwtException(JwtException e) {
        log.warn("JWT异常: {}", e.getMsg());
        return R.fail(e.getCode(), e.getMsg());
    }

    /**
     * 处理参数校验异常（@RequestBody参数）
     * 
     * 获取绑定结果：通过调用e.getBindingResult() 获取到 BindingResult 对象，其中包含了所有验证错误的信息
     * 提取错误信息：使用 getAllErrors() 方法获取所有的 ObjectError 对象列表，每个 ObjectError 对应一个验证失败的字段。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return R.fail(CommonConstants.FAIL, message);
    }

    /**
     * 处理参数校验异常（@Validated参数）
     * 
     * 主要处理来自 Spring 数据绑定框架层面的验证错误
     * - 错误来源通常是请求参数与目标对象绑定过程中发生的验证失败
     */
    @ExceptionHandler(BindException.class)
    public R handleBindException(BindException e) {
        String message = e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return R.fail(CommonConstants.FAIL, message);
    }

    /**
     * 处理约束违规异常（@PathVariable、@RequestParam参数）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return R.fail(CommonConstants.FAIL, message);
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件上传大小超限: {}", e.getMessage());
        long maxSize = e.getMaxUploadSize();
        if (maxSize > 0) {
            long maxSizeMB = maxSize / 1024 / 1024;
            return R.fail("上传文件大小超出限制，最大允许 " + maxSizeMB + "MB");
        }
        return R.fail("上传文件大小超出限制，请上传更小的文件");
    }

    /**
     * 处理缺少文件参数异常
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public R handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        log.warn("缺少文件参数: {}", e.getRequestPartName());
        return R.fail("请选择要上传的文件");
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
     * 处理 multipart 解析异常（如文件损坏、格式错误等）
     */
    @ExceptionHandler(MultipartException.class)
    public R handleMultipartException(MultipartException e) {
        // 判断是否是文件大小超限引起的
        if (e.getCause() instanceof MaxUploadSizeExceededException) {
            return handleMaxUploadSizeExceededException((MaxUploadSizeExceededException) e.getCause());
        }

        // 其他 multipart 异常
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("size")) {
                log.warn("文件上传失败: 文件过大");
                return R.fail("上传文件过大，请压缩后重试");
            } else if (message.contains("corrupt") || message.contains("invalid")) {
                log.warn("文件上传失败: 文件损坏或格式不正确");
                return R.fail("文件已损坏或格式不正确，请重新选择");
            } else if (message.contains("permission") || message.contains("denied")) {
                log.error("文件上传失败: 服务器权限不足");
                return R.fail("服务器文件上传权限不足，请联系管理员");
            }
        }

        log.warn("文件上传失败: {}", e.getMessage());
        return R.fail("文件上传失败，请检查文件是否正确");
    }

    /**
     * 处理访问拒绝异常（无权限）
     */
    @ExceptionHandler(AccessDeniedException.class)
    public R handleAccessDeniedException(AccessDeniedException e) {
        log.warn("访问拒绝: {}", e.getMessage());
        return R.forbidden("无权限访问该资源");
    }

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
     * 
     * 处理应用程序级别因参数值不当而引发的问题
     * - 错误来源更多是业务代码中对参数合法性的判断，而非框架自动验证
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
        // 忽略favicon等静态资源404，不打印日志
        String path = e.getResourcePath();
        if (path.contains("favicon") || path.contains("doc.html")) {
            return R.notFound();
        }
        log.warn("资源未找到: {}", path);
        return R.notFound("请求的资源不存在");
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public R handleException(Exception e) {
        log.error("系统异常: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return R.fail("系统异常，请联系管理员");
    }
}
