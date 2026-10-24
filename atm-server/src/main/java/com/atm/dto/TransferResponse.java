package com.atm.dto;

import lombok.Data;

import java.math.BigDecimal;

//转账响应体
@Data
public class TransferResponse {

    //交易流水号
    private String transNo;

    //对方卡号
    private String targetCardNo;

    //对方客户姓名
    private String targetName;

    //转账金额
    private BigDecimal amount;

    //手续费
    private BigDecimal fee;

    //扣款合计 = amount + fee
     private BigDecimal totalAmount;

     //转账后本方金额
    private BigDecimal balanceAfter;

    private String message;
}
