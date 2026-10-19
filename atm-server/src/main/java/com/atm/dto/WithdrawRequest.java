package com.atm.dto;


import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 取款请求体
 */
@Data
public class WithdrawRequest {

    @NotBlank(message = "回话令牌不能为空")
    private String sessionToken;

    @NotNull(message = "取款金额不能为空")
    @DecimalMin(value = "100",message = "取款金额最少100元")
    @DecimalMax(value = "5000",message = "单笔取款最多 5000 元")
    private BigDecimal amount;
}
