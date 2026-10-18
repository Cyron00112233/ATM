package com.atm.mapper;

import com.atm.entity.BankCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 银行卡表 Mapper
 */
@Mapper
public interface BankCardMapper {

    //按卡号查（插卡时用）
    BankCard selectByCardNo(@Param("cardNo") String cardNo);

    //密码错误次数+1（SQL 原子自增）
    int increasePwdErrorCount(@Param("id") Long id);

    //密码错误次数归零
    int resetPwdErrorCount(@Param("id") Long id);

    //锁卡：状态改 2，记录锁定时间
    int lockCard(@Param("id") Long id);
}
