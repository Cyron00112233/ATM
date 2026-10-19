package com.atm.entity;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ATM 钞箱实体，对应数据库表 atm_cash_box
 */
@Data
public class AtmCashBox {

    private Long id;

    //机具编号
    private String deviceNo;

    //钞箱编号
    private String boxNo;

    //面额：100/50
    private Integer denomination;

    //箱内总金额
    private Integer totalAmount;

    //钞票张数
    private Integer noteCount;

    //最大容量金额
    private BigDecimal maxAmount;

    //状态：1 正常
    private Integer status;

    private LocalDateTime updateTime;
}
