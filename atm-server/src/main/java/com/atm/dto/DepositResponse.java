package com.atm.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 存款响应体
 */
@Data
public class DepositResponse {

    //交易流水号
    private String transNo;

    //存款金额
    private BigDecimal amount;

    //存款后金额
    private BigDecimal balanceAfter;

    //进钞明细
    private String cashDetail;

    private String message;
}
