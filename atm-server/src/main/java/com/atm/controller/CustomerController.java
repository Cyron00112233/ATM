package com.atm.controller;

import com.atm.common.Result;
import com.atm.entity.Customer;
import com.atm.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户接口
 * Controller 层只做三件事：
 * ① 接收请求参数  ② 调用 Service  ③ 包装成 Result 返回
 */
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * 查询全部客户
     * GET http://localhost:8080/api/customer/list
     */
    @GetMapping("/list")
    public Result<List<Customer>> list(){
        return Result.success(customerService.listAll());
    }

    /**
     * 按 id 查询单个客户
     * GET http://localhost:8080/api/customer/get/1
     */
    @GetMapping("/get/{id}")
    public Result<Customer> getById(@PathVariable("id") Long id){
        return Result.success(customerService.getById(id));
    }

    /**
     * 按姓名模糊查询
     * GET http://localhost:8080/api/customer/search?name=张
     */
    @GetMapping("/search")
    public Result<List<Customer>> search(@RequestParam("name") String name){
        return Result.success(customerService.searchByName(name));
    }

    /**
     * 统计客户总数
     * GET http://localhost:8080/api/customer/count
     */
    @GetMapping("/count")
    public Result<Long> count(){
        return Result.success(customerService.count());
    }
}
