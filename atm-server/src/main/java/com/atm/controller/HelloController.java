package com.atm.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HelloController {
    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", 200);
        data.put("message", "ATM 自助管理系统后端启动成功！");
        data.put("time", LocalDateTime.now().toString());
        return data;
    }
}
