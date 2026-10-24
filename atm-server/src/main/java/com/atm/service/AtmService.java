package com.atm.service;

import com.atm.dto.InsertCardRequest;
import com.atm.dto.InsertCardResponse;
import com.atm.dto.VerifyPasswordRequest;
import com.atm.dto.WithdrawRequest;
import com.atm.dto.WithdrawResponse;
import com.atm.dto.DepositRequest;
import com.atm.dto.DepositResponse;
import com.atm.dto.BalanceRequest;
import com.atm.dto.BalanceResponse;
import com.atm.dto.TransDetailItem;
import com.atm.dto.TransDetailRequest;
import java.util.List;
import com.atm.dto.TransferRequest;
import com.atm.dto.TransferResponse;
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

    //取款
    WithdrawResponse withdraw(WithdrawRequest request);

    //存款
    DepositResponse deposit(DepositRequest request);

    //余额查询
    BalanceResponse queryBalance(BalanceRequest request);

    //交易明细
    List<TransDetailItem> queryTransactions(TransDetailRequest request);

    //转账
    TransferResponse transfer(TransferRequest request);
}
