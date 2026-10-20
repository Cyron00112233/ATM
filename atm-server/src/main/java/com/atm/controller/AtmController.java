package com.atm.controller;

import com.atm.common.Result;
import com.atm.dto.EjectCardRequest;
import com.atm.dto.InsertCardRequest;
import com.atm.dto.InsertCardResponse;
import com.atm.dto.VerifyPasswordRequest;
import com.atm.service.AtmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.atm.dto.WithdrawRequest;
import com.atm.dto.WithdrawResponse;

/**
 * ATM 机具端接口
 * <p>
 * 注意：这一组接口不走管理员 JWT，用的是 ATM 自己的会话 token（sessionToken）
 */
@RestController
@RequestMapping("/api/atm")
@RequiredArgsConstructor
public class AtmController {

    private final AtmService atmService;

    /**
     * ① 插卡
     * POST http://localhost:8080/api/atm/insert-card
     * { "cardNo": "6222020000000001", "deviceNo": "ATM001" }
     */
    @PostMapping("/insert-card")
    public Result<InsertCardResponse> insertCard(@Valid @RequestBody InsertCardRequest request){
        return Result.success("插卡成功",atmService.insertCard(request));
    }

    /**
     * ② 验证交易密码
     * POST http://localhost:8080/api/atm/verify-password
     * { "sessionToken": "xxx", "password": "123456" }
     */
    @PostMapping("/verify-password")
    public Result<Void> verifyPassword(@Valid @RequestBody VerifyPasswordRequest request){
        atmService.verifyPassword(request);
        return Result.success("密码验证通过",null);
    }

    /**
     * ③ 退卡
     * POST http://localhost:8080/api/atm/eject-card
     * { "sessionToken": "xxx" }
     */
    @PostMapping("/eject-card")
    public Result<Void> ejectCard(@Valid @RequestBody EjectCardRequest request){
        atmService.ejectCard(request.getSessionToken());
        return Result.success("请取走你的银行卡",null);
    }

    /**
     * ④ 取款
     * POST http://localhost:8080/api/atm/withdraw
     * { "sessionToken": "xxx", "amount": 500 }
     */
    @PostMapping("/withdraw")
    public Result<WithdrawResponse> withdraw(@Valid @RequestBody WithdrawRequest request){
        return Result.success("取款成功",atmService.withdraw(request));
    }
}
