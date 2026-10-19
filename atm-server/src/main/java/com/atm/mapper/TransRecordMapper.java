package com.atm.mapper;

import com.atm.entity.TransRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 交易流水表 Mapper
 */
@Mapper
public interface TransRecordMapper {

    //插入流水（自增id回填到 record.id）
    int insert(TransRecord record);

    //按流水号查（幂等检查：重复提交时能查到已有记录）
    TransRecord selectByTransNo(@Param("transNo") String transNo);

}
