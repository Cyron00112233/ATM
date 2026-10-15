package com.atm.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Customer {
    /**主键id*/
     private Long id;

    /**客户号*/
    private String customerNo;

    /**姓名*/
    private String realName;

    /**身份证号*/
    private String idCard;

    /**手机号*/
    private String phone;

    /**性别：1男2女*/
    private Integer gender;

    /**联系地址*/
    private String address;

    /**状态：1 正常 0 注销*/
    private Integer status;

    /**创建时间*/
    private LocalDateTime createTime;

    /**更新时间*/
    private LocalDateTime updateTime;
}
