package com.atm.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 余额查询请求体 */
@Data
public class BalanceRequest {

    @NotBlank(message = "会话令牌不能为空")
    private String sessionToken;
}
