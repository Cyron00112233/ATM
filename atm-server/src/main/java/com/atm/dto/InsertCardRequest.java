package com.atm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 插卡请求体
 * POST /api/atm/insert-card
 * { "cardNo": "6222020000000001", "deviceNo": "ATM001" }
 */
@Data
public class InsertCardRequest {

    @NotBlank(message = "卡号不能为空")
    private String cardNo;

    //机具编号,可以不传
    private String deviceNo;
}
