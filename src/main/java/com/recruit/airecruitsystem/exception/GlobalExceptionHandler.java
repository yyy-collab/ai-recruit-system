package com.recruit.airecruitsystem.exception;

import com.recruit.airecruitsystem.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice   // 统一处理所有Controller抛出的异常，自动返回JSON格式
public class GlobalExceptionHandler {

    // 捕获所有未被处理的RuntimeException，返回500错误及异常信息
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        e.printStackTrace();          // 打印堆栈，便于排查
        return Result.error(500, e.getMessage());
    }

    // 可以按需添加其他异常处理方法，如参数校验异常、自定义业务异常等
}