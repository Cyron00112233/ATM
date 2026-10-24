package com.atm.dto;


import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

//转账请求体
@Data
public class TransferRequest {

    @NotBlank(message = "会话令牌不能为空")
    private String sessionToken;

    @NotBlank(message = "对方卡号不能为空")
    private String targetCardNo;

    @NotNull(message = "转账金额不能为空")
    @DecimalMin(value ="1",message = "转账金额最少1元")
    @DecimalMax(value = "50000",message = "单笔转账最多50000元")
    private BigDecimal amount;
}
