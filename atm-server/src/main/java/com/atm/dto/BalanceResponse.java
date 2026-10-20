package com.atm.dto;

import lombok.Data;

import java.math.BigDecimal;

//余额查询响应体
@Data
public class BalanceResponse {

    //脱敏卡号
    private String cardNo;

    //客户姓名
    private String customerName;

    //账户
    private String accountNo;

    //账面余额
    private BigDecimal balance;

    //可用余额
    private BigDecimal availableBalance;

    //币种
    private String currency;
}
