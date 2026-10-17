package com.atm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全相关配置
 * <p>
 * 注意：我们只用了 spring-security-crypto（密码工具），
 * 没有引 spring-boot-starter-security，所以不会自动拦截接口。
 */
@Configuration
public class SecurityConfig {

    /**
     * BCrypt 密码编码器
     * <p>
     * 注册成 Bean 后，任何地方注入 PasswordEncoder 就能用：
     * · encode(明文)               → 生成 BCrypt 哈希（60 位）
     * · matches(明文, 数据库哈希)   → 校验，返回 boolean
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
