package com.atm.mapper;

import com.atm.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 账户表 Mapper
 */
@Mapper
public interface AccountMapper {

    //按银行卡 id 查活期账户（取款时用）
    Account selectByCardId(@Param("cardId") Long cardId);

    Account selectById(@Param("id") Long id);

    /**
     * 扣款：余额充足才会更新
     *
     * @return 影响行数；返回 0 表示余额不足，一分钱都没扣
     */
    int decreaseBalance(@Param("id") Long id, @Param("amount")BigDecimal amount);

    //入账：存款/转账转入
    int increaseBalance(@Param("id") Long id,@Param("amount") BigDecimal amount);
}
