package cn.lzx.exception;

import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        log.error("业务异常: {}", e.getMsg(), e);
        return R.fail(e.getCode(), e.getMsg());
    }

    /**
     * 处理JWT异常
     */
    @ExceptionHandler(JwtException.class)
    public R handleJwtException(JwtException e) {
        log.error("JWT异常: {}", e.getMsg(), e);
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
        log.error("空指针异常", e);
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
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public R handleException(Exception e) {
        log.error("未知异常", e);
        return R.fail("系统异常，请联系管理员");
    }
}
