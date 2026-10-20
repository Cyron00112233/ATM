package com.atm.mapper;

import com.atm.entity.AtmCashBox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * ATM 钞箱表 Mapper
 */
@Mapper
public interface AtmCashBoxMapper {

    //按机具+面额查钞箱（名字必须和 XML 里的 #{deviceNo} 一致）
    AtmCashBox selectByDeviceAndDenomination(@Param("deviceNo") String deviceNo,
                                             @Param("denomination") Integer denomination);

    /**
     * 出钞：扣张数，返回 0 表示钞不够
     */
    int decreaseNoteCount(@Param("id") Long id,@Param("count") Integer count);

    //进钞：存款用
    int increaseNoteCount(@Param("id") Long id,@Param("count") Integer count);
}
