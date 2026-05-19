package com.recruit.airecruitsystem.result;

import lombok.Data;

@Data
public class Result<T> {       // 泛型类，T表示返回的数据类型
    private Integer code;      // 业务状态码，0为成功，其他为错误码
    private String msg;        // 提示信息
    private T data;            // 实际返回的数据

    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // 静态工厂方法：成功无数据
    public static <T> Result<T> success() {
        return new Result<>(0, "操作成功", null);
    }

    // 成功有数据
    public static <T> Result<T> success(T data) {
        return new Result<>(0, "操作成功", data);
    }

    // 成功自定义消息+数据
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(0, msg, data);
    }

    // 失败：自定义错误码和消息
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}