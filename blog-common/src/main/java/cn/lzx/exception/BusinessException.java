package cn.lzx.exception;

import cn.lzx.constants.CommonConstants;

/**
 * 业务异常类
 * 用于处理所有业务逻辑异常，如数据不存在、操作失败、用户注册、登录、文件上传等场景
 *
 * @author lzx
 * @since 2025-10-31
 */
public class BusinessException extends BaseException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String msg) {
        super(CommonConstants.FAIL, msg);
    }

    public BusinessException(Integer code, String msg) {
        super(code, msg);
    }

    public BusinessException(String msg, Throwable cause) {
        super(CommonConstants.FAIL, msg, cause);
    }
}
