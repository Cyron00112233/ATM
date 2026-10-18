package com.atm.config;

import com.atm.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置：注册拦截器
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")          // 拦截所有 /api 开头的接口
                .excludePathPatterns(                 // 白名单：这些不拦
                        "/api/admin/login",           // 必须有，否则登录会死循环
                        "/api/atm/**",        // ← 新增：ATM 端用自己的会话 token，不走管理员 JWT
                        "/api/hello",                 // 注意开头必须有斜杠，少了匹配不到
                        "/error"
                );
    }
}
