package com.atm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 验证密码请求体
 */
@Data
public class VerifyPasswordRequest {

    @NotBlank(message = "会话令牌不能为空")
    private String sessionToken;

    @NotBlank(message = "密码不能为空")
    private String password;
}
