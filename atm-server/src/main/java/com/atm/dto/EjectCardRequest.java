package com.atm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 退卡请求体
 */
@Data
public class EjectCardRequest {

    @NotBlank(message = "会话令牌不能为空")
    private String sessionToken;
}
