package com.recruit.airecruitsystem.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.recruit.airecruitsystem.result.Result;
import com.recruit.airecruitsystem.constant.ResultCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 @Valid 校验失败（JSON请求体）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.error(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理 @Validated 校验失败（表单、Query参数）
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.error(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理请求参数类型不匹配（如需要int却传了字符串）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        String msg = "参数 '" + e.getName() + "' 类型应为 " + e.getRequiredType().getSimpleName();
        return Result.error(ResultCode.PARAM_ERROR, msg);
    }

    /**
     * 处理缺少必要请求参数（@RequestParam 标记 required=true 但未传）
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        String msg = "缺少必要参数: " + e.getParameterName();
        return Result.error(ResultCode.PARAM_ERROR, msg);
    }

    /**
     * 处理请求体 JSON 格式错误（如缺少必要字段、类型不匹配等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.error(ResultCode.PARAM_ERROR, "请求体格式错误，请检查JSON格式和字段类型");
    }

    /**
     * 处理请求路径不存在（404）—— 需要配置 spring.mvc.throw-exception-if-no-handler-found=true
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        return Result.error(ResultCode.NOT_FOUND, "请求路径不存在: " + e.getRequestURL());
    }

    /**
     * 处理其他未捕获的运行时异常（防止敏感信息泄露）
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        // 记录日志（建议使用 log.error）
        e.printStackTrace();  // 开发环境打印，生产环境建议用日志库
        return Result.error(ResultCode.PARAM_ERROR, "系统繁忙，请稍后再试");
    }
}