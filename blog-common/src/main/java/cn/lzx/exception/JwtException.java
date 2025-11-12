package cn.lzx.exception;

import cn.lzx.constants.CommonConstants;

/**
 * JWT认证异常
 * 用于处理JWT相关的异常，如token无效、过期等
 *
 * @author lzx
 * @since 2025-10-31
 */
public class JwtException extends BaseException {

    private static final long serialVersionUID = 1L;

    public JwtException(String msg) {
        super(CommonConstants.UNAUTHORIZED, msg);
    }

    public JwtException(Integer code, String msg) {
        super(code, msg);
    }

    public JwtException(String msg, Throwable cause) {
        super(CommonConstants.UNAUTHORIZED, msg, cause);
    }
}