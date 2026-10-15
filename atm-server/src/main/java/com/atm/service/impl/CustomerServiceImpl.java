package com.atm.service.impl;

import com.atm.common.BusinessException;
import com.atm.common.ResultCode;
import com.atm.entity.Customer;
import com.atm.mapper.CustomerMapper;
import com.atm.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 客户业务实现类
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;

    @Override
    public List<Customer> listAll() {
        return customerMapper.selectAll();
    }

    @Override
    public Customer getById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null){
            // 业务规则：客户不存在就抛异常，由全局异常处理器转成 JSON
            throw new BusinessException(ResultCode.CUSTOMER_NOT_FOUND,"客户不存在，id = " + id);

        }
        return customer;
    }

    @Override
    public List<Customer> searchByName(String name) {
        return customerMapper.selectByNameLike(name);
    }

    @Override
    public long count() {
        return customerMapper.count();
    }
}
