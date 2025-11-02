package cn.lzx.utils;

import lombok.Data;

import java.io.Serializable;

import cn.lzx.constants.CommonConstants;

@Data
public class R implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private Object data;
    private Long timestamp;

    private R() {
        this.timestamp = System.currentTimeMillis();
    }

    // ========== 成功返回 ==========

    public static R success() {
        R r = new R();
        r.setCode(CommonConstants.SUCCESS);
        r.setMsg("操作成功");
        return r;
    }

    public static R success(Object data) {
        R r = new R();
        r.setCode(CommonConstants.SUCCESS);
        r.setMsg("操作成功");
        r.setData(data);
        return r;
    }

    public static R success(String msg, Object data) {
        R r = new R();
        r.setCode(CommonConstants.SUCCESS);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }

    // ========== 失败返回 ==========

    public static R fail() {
        R r = new R();
        r.setCode(CommonConstants.FAIL);
        r.setMsg("操作失败");
        return r;
    }

    public static R fail(String msg) {
        R r = new R();
        r.setCode(CommonConstants.FAIL);
        r.setMsg(msg);
        return r;
    }

    public static R fail(Integer code, String msg) {
        R r = new R();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    public static R fail(Integer code, String msg, Object data) {
        R r = new R();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }

    // ========== 未认证返回 ==========

    public static R unauthorized() {
        R r = new R();
        r.setCode(CommonConstants.UNAUTHORIZED);
        r.setMsg("未登录或登录已过期");
        return r;
    }

    public static R unauthorized(String msg) {
        R r = new R();
        r.setCode(CommonConstants.UNAUTHORIZED);
        r.setMsg(msg);
        return r;
    }

    // ========== 无权限返回 ==========

    public static R forbidden() {
        R r = new R();
        r.setCode(CommonConstants.FORBIDDEN);
        r.setMsg("无权限访问");
        return r;
    }

    public static R forbidden(String msg) {
        R r = new R();
        r.setCode(CommonConstants.FORBIDDEN);
        r.setMsg(msg);
        return r;
    }

    // ========== 资源不存在返回 ==========

    public static R notFound() {
        R r = new R();
        r.setCode(CommonConstants.NOT_FOUND);
        r.setMsg("资源不存在");
        return r;
    }

    public static R notFound(String msg) {
        R r = new R();
        r.setCode(CommonConstants.NOT_FOUND);
        r.setMsg(msg);
        return r;
    }
}
