package com.atm.common;

/**
 * 统一状态码
 * <p>
 * 约定：
 * 200      成功
 * 400~499  客户端/参数类错误
 * 500      服务端未知错误
 * 1001+    业务类错误（客户、银行卡、账户）
 * 2001+    交易类错误（余额、限额、密码、钞箱）
 */
public enum ResultCode {

    /* ========== 通用 ========== */
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    /* ========== 参数与权限 ========== */
    PARAM_ERROR(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限访问"),
    NOT_FOUND(404, "请求的资源不存在"),

    /* ========== 业务类 1xxx ========== */
    CUSTOMER_NOT_FOUND(1001, "客户不存在"),
    CARD_NOT_FOUND(1002, "银行卡不存在"),
    ACCOUNT_NOT_FOUND(1003, "账户不存在"),

    /* ========== 交易类 2xxx（阶段三会用到） ========== */
    BALANCE_NOT_ENOUGH(2001, "余额不足"),
    AMOUNT_LIMIT_EXCEED(2002, "超出限额"),
    AMOUNT_ILLEGAL(2003, "金额不合法"),
    PASSWORD_ERROR(2004, "密码错误"),
    CARD_LOCKED(2005, "银行卡已被锁定"),
    CARD_STATUS_ABNORMAL(2006, "银行卡状态异常"),
    CASH_NOT_ENOUGH(2007, "本机现金不足"),
    TRANSFER_TO_SELF(2008, "不能给自己转账");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
