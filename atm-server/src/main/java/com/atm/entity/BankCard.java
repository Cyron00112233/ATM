package com.atm.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 银行卡实体，对应数据库表 bank_card
 */
@Data
public class BankCard {

    /** 主键 */
    private Long id;

    /** 银行卡号 */
    private String cardNo;

    /** 所属客户 id */
    private Long customerId;

    /** 卡类型：1 借记卡（数据库是 tinyint，所以必须用 Integer 不是 String） */
    private Integer cardType;

    /** 交易密码（BCrypt 哈希） */
    private String password;

    /** 连续密码错误次数 */
    private Integer pwdErrorCount;

    /** 卡状态：1 正常 2 锁定 3 挂失 4 注销 */
    private Integer cardStatus;

    /** 锁定时间 */
    private LocalDateTime lockTime;

    /** 开卡时间 */
    private LocalDateTime openTime;

    /** 有效期（数据库是 date 类型，只有年月日，所以用 LocalDate） */
    private LocalDate expireDate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
