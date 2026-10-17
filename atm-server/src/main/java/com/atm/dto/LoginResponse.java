package com.atm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应体
 * <p>
 * ⚠️ 注意：这个类里【绝对不能有 password 字段】！
 * 前端只需要知道"登录成功了、token 是什么"，不需要知道密码哈希。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    // JWT Token
    private String token;

    // Token 有效期（秒），前端可以用它做“提前续期”
    private Long expiresIn;

    //用户名
    private String username;

    //真实姓名
    private String realName;

    //角色（前端可以按角色决定显示哪些菜单）
    private String role;


}
