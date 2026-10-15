package com.atm.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * {@code @RestControllerAdvice} = {@code @ControllerAdvice} + {@code @ResponseBody}
 * <p>
 * 作用：任何 Controller 抛出的异常都会被这里接住，统一转成 {@link Result} JSON，
 * 这样每个 Controller 里都不用写 try-catch。
 * <p>
 * 处理顺序：Spring 会优先匹配「最具体」的异常类型，所以 BusinessException
 * 不会被下面的 Exception 兜底方法抢走。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 1. 业务异常：我们自己主动抛的，属于「预期内」的错误，只记 warn */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常 -> code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 2. 参数校验失败：@RequestBody + @Valid 触发的异常 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(ResultCode.PARAM_ERROR.getMessage());
        log.warn("参数校验失败 -> {}", message);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /** 3. 参数绑定失败：表单/URL 参数类型不匹配等 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(ResultCode.PARAM_ERROR.getMessage());
        log.warn("参数绑定失败 -> {}", message);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /** 4. 缺少必填的请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        String message = "缺少必填参数：" + e.getParameterName();
        log.warn(message);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 5. 兜底：其它所有未预料到的异常
     * 这类异常说明代码有 bug，要把完整堆栈打到日志里（error 级别），
     * 但返回给前端的只给一句概括，不要把堆栈暴露出去。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.FAIL.getCode(), "系统异常：" + e.getMessage());
    }
}
