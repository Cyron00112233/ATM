package com.atm.mapper;

import com.atm.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 客户表 Mapper（数据访问层）
 */
@Mapper
public interface CustomerMapper {

    /** 查询全部客户 */
    List<Customer> selectAll();

    /** 按主键查询（查不到返回 null） */
    Customer selectById(@Param("id") Long id);

    /** 按名字模糊查询 */
    List<Customer> selectByNameLike(@Param("name") String name);

    /** 统计客户总数 */
    long count();
}
