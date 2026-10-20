package com.atm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 存款请求体
 */
@Data
public class DepositRequest {

    @NotBlank(message = "会话令牌不能为空")
    private String sessionToken;

    @NotNull(message = "存款金额不能为空")
    @DecimalMin(value = "100",message = "存款金额最少100元")
    @DecimalMax(value = "10000",message = "单次存款最多10000元")
    private BigDecimal amount;
}
