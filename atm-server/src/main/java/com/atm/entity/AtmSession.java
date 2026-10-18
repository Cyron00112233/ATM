package com.atm.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ATM 会话实体，对应数据库表 atm_session
 */
@Data
public class AtmSession {

    //主键（数据库是 bigint，必须用 Long 不是 String）
    private Long id;

    //会话号（给人看的编号）
    private String sessionNo;

    //卡号
    private String cardNo;

    //机具编号
    private String deviceNo;

    //会话令牌（给程序的随机串，客户端每次带它）
    private String token;

    //状态：1 进行中 2 已退卡 3 超时 4 异常
    private Integer status;

    //插卡时间
    private LocalDateTime loginTime;

    //退卡时间
    private LocalDateTime logoutTime;

    //最后活动时间（用于判断超时）
    private LocalDateTime lastActiveTime;

    private LocalDateTime createTime;
}
