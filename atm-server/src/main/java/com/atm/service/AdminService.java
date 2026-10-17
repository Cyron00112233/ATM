package com.atm.service;

import com.atm.dto.LoginRequest;
import com.atm.dto.LoginResponse;

/**
 * 管理员业务接口
 */
public interface AdminService {

    // 管理员登录：校验账号密码，成功返回Token
    LoginResponse login(LoginRequest request);
}
