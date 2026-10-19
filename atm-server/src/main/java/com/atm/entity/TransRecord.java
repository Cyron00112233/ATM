package com.atm.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易流水实体，对应数据库表 trans_record
 * <p>
 * ⚠️ trans_no 上有唯一索引 uk_trans_no，是「幂等」的保证：
 * 同一笔请求重复提交，第二次会因唯一约束插入失败
 */
@Data
public class TransRecord {

    private Long id;

    //交易流水号（唯一索引）
    private String transNo;

    //交易类型：WITHDRAW/DEPOSIT/TRANSFER_OUT/TRANSFER_IN/FEE/QUERY
    private String transTypeCode;

    //本方卡号
    private String cardNo;

    private Long accountId;

    //对方卡号（转账才有）
    private String targetCardNo;

    private Long targetAccountId;

    //交易金额
    private BigDecimal amount;

    //手续费
    private BigDecimal fee;

    //交易前金额
    private BigDecimal beforeBalance;

    //交易后金额
    private BigDecimal afterBalance;

    private String currency;

    //状态：1 成功 2 失败
    private Integer status;

    //失败原因
    private String failReason;

    private String deviceNo;

    //关联的ATM会话id
    private Long sessionId;

    private String remark;

    private LocalDateTime transTime;

    private LocalDateTime createTime;
}
