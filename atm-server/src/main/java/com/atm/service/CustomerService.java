package com.atm.service;

import com.atm.entity.Customer;

import java.util.List;

/**
 * 客户业务接口
 */
public interface CustomerService {

    /** 查询全部客户 */
    List<Customer> listAll();

    /** 按id查询客户，查不到抛业务异常 */
    Customer getById(Long id);

    /** 按名字模糊查询 */
    List<Customer> searchByName(String name);

    /** 统计客户总数 */
    long count();
}
