package com.atm.entity;


import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员实体，对应数据库表 sys_user
 */
@Data
public class SysUser {

    /** 主键 id */
    private Long id;

    /** 登录账号 */
    private String username;

    /** 密码（BCrypt 哈希，60w位） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 状态：1启用· 0禁用 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
