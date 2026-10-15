package com.atm.common;

import lombok.Data;

/**
 * 统一响应结果
 * <p>
 * 所有接口都返回这个格式，前端只需要处理一种结构：
 * <pre>
 * {
 *   "code": 200,
 *   "message": "操作成功",
 *   "data": { ... },
 *   "timestamp": 1760000000000
 * }
 * </pre>
 *
 * @param <T> 业务数据的类型
 */
@Data
public class Result<T> {

    /** 状态码：200 表示成功，其它为失败 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 业务数据（失败时通常为 null） */
    private T data;

    /** 服务器时间戳，方便前端排查问题 */
    private Long timestamp;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /** 成功，不带数据（用于删除、修改这类不需要返回内容的操作） */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 成功，带数据（最常用） */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 成功，自定义提示 + 数据 */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /** 失败，使用状态码枚举（推荐） */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 失败，自定义状态码和提示 */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
