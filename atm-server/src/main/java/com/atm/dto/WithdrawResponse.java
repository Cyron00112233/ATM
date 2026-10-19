package com.atm.dto;


import lombok.Data;

import java.math.BigDecimal;

/**
 * 取款响应体
 */
@Data
public class WithdrawResponse {

    //交易流水号
    private String transNo;

    //取款金额
    private BigDecimal amount;

    //取款后金额
    private BigDecimal balanceAfter;

    //出钞明细
    private String cashDetail;

    private String message;
}
