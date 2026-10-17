package com.atm.mapper;

import com.atm.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 管理员表 Mapper
 */
@Mapper
public interface SysUserMapper {

    /** 按登录账号查询（登录时用） */
    SysUser selectByUsername(@Param("username") String username);

    /** 更新最后登录时间，返回影响的行数 */
    int updateLastLoginTime(@Param("id") Long id);
}
