package com.atm.service;

import com.atm.dto.InsertCardRequest;
import com.atm.dto.InsertCardResponse;
import com.atm.dto.VerifyPasswordRequest;

/**
 * ATM 端业务接口
 */
public interface AtmService {

    //插卡：校验卡状态，创建会话
    InsertCardResponse insertCard(InsertCardRequest request);

    //验证交易密码（连续错三次锁卡）
    void verifyPassword(VerifyPasswordRequest request);

    //退卡：结束会话
    void ejectCard(String sessionToken);
}
