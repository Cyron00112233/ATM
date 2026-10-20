package com.atm.dto;

import lombok.Data;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易明细中的一条
 * <p>
 * 只保留前端要展示的字段 —— accountId、sessionId、deviceNo 这些内部字段不返回
 */
@Data
public class TransDetailItem {

    private String transNo;

    //类型代码
    private String transTypeCode;

    //类型名称
    private String transTypeName;

    private BigDecimal amount;

    private BigDecimal fee;

    private BigDecimal beforeBalance;

    private BigDecimal afterBalance;

    //1.成功 2。失败
    private Integer status;

    private LocalDateTime transTime;
}
