package com.atm.controller;

import com.atm.common.Result;
import com.atm.dto.LoginRequest;
import com.atm.dto.LoginResponse;
import com.atm.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端接口
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 管理员登录
     * POST http://localhost:8080/api/admin/login
     * { "username": "admin", "password": "admin123" }
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request){
        return Result.success("登录成功",adminService.login(request));
    }
}
