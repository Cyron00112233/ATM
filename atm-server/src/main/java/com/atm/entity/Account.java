package com.atm.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户实体，对应数据库表 account
 */
@Data
public class Account {

    private Long id;

    //账号
    private String accountNo;

    //所属银行卡id
    private Long cardId;

    private Long customerId;

    //账户类型：1 活期 2 定期
    private Integer accountType;

    //币种
    private String currency;

    //账面余额
    private BigDecimal balance;

    //可用余额
    private BigDecimal availableBalance;

    //冻结金额
    private BigDecimal frozenAmount;

    //状态:  1 正常 2 冻结 3 销户
    private Integer status;

    //乐观锁版本号
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
