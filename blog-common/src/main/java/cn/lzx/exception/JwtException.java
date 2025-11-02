package cn.lzx.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
// 考虑父类的属性,正确处理继承关系中的对象相等性判断
@EqualsAndHashCode(callSuper = true)
public class JwtException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;

    public JwtException(String msg) {
        super(msg);
        this.code = 401;
        this.msg = msg;
    }

    public JwtException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public JwtException(String msg, Throwable cause) {
        super(msg, cause);
        this.code = 401;
        this.msg = msg;
    }
}