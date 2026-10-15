package com.atm.controller;

import com.atm.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口：用来验证服务是否正常启动
 * <p>
 * 已改造为使用统一响应格式 {@link Result}
 */
@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public Result<Map<String, Object>> hello() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "ATM 自助管理系统后端启动成功！");
        data.put("time", LocalDateTime.now().toString());
        return Result.success(data);
    }
}
