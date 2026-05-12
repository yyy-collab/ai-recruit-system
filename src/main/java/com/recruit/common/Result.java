package com.recruit.common;

import lombok.Data;

/**
 * 统一响应结果
 * 所有人写接口必须返回这个类
 */
@Data
public class Result<T> {
    private int code;    // 0成功 1失败
    private String msg; // 提示信息
    private T data;     // 数据

    // 成功
    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMsg("操作成功");
        r.setData(data);
        return r;
    }

    // 失败
    public static <T> Result<T> fail(String msg) {
        Result<T> r = new Result<>();
        r.setCode(1);
        r.setMsg(msg);
        return r;
    }
}