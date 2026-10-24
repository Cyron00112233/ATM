package com.atm.mapper;

import com.atm.entity.AtmSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * ATM 会话表 Mapper
 */
@Mapper
public interface AtmSessionMapper {

    //新建会话（插入后自增id 会回填到 session.id）
    int insert(AtmSession session);

    //按token 查会话
    AtmSession selectByToken(@Param("token") String token);

    //更新状态（退卡/超时/异常）
    int updateStatus(@Param("id") Long id,@Param("status") Integer status);

    //更新最后活动时间
    int updateLastActive(@Param("id") Long id);

    //标记会话已通过交易密码验证（verified 置 1）
    int markVerified(@Param("id") Long id);
}
