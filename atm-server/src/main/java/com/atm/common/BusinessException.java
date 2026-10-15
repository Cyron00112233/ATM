package com.atm.common;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 用法：在 Service 层遇到业务规则不满足时抛出，例如：
 * <pre>
 *     if (account.getBalance().compareTo(amount) &lt; 0) {
 *         throw new BusinessException(ResultCode.BALANCE_NOT_ENOUGH);
 *     }
 * </pre>
 * 抛出后会被 {@link GlobalExceptionHandler} 捕获，自动转成统一 JSON。
 * <p>
 * 特点：继承 RuntimeException（非受检异常），业务代码里不用到处写 try-catch。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务状态码 */
    private final Integer code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /** 使用状态码枚举，但覆盖提示信息（适合带具体数值的提示） */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
