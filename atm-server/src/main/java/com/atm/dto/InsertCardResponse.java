package com.atm.dto;

import lombok.Data;

/**
 * 插卡响应体
 */
@Data
public class InsertCardResponse {
    //会话令牌 —— 后续所有操作都要带上它
    private String sessionToken;

    //脱敏后的卡号
    private String cardNo;

    //客户姓名
    private String customerName;

    //提示语
    private String message;
}
