-- ============================================================================
-- ATM 自助管理系统 —— 数据库建表脚本
-- 数据库: atm_db        字符集: utf8mb4 / utf8mb4_general_ci
-- 共 13 张表，分 5 组：权限 / 客户与账户 / 交易 / 机具 / 审计对账
-- 本脚本可重复执行（每条 drop table if exists 保证幂等）
-- 注意：重复执行会清空这 13 张表的数据！
-- ============================================================================

use atm_db;

-- ============================================================================
-- A 组：权限（3 张）
-- ============================================================================

-- A1 管理员账号表
drop table if exists sys_user;
create table sys_user (
  id              bigint       not null auto_increment comment '主键',
  username        varchar(50)  not null                comment '登录账号',
  password        varchar(100) not null                comment '密码（BCrypt 哈希，60 位）',
  real_name       varchar(50)      null                comment '真实姓名',
  phone           varchar(11)      null                comment '手机号',
  status          tinyint      not null default 1      comment '状态：1 启用 0 禁用',
  last_login_time datetime         null                comment '最后登录时间',
  create_time     datetime     not null default current_timestamp comment '创建时间',
  update_time     datetime     not null default current_timestamp on update current_timestamp comment '更新时间',
  primary key (id),
  unique key uk_username (username)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='管理员账号表';

-- A2 角色表
drop table if exists sys_role;
create table sys_role (
  id          bigint       not null auto_increment comment '主键',
  role_code   varchar(50)  not null comment '角色编码：admin / operator / auditor',
  role_name   varchar(50)  not null comment '角色名称',
  description varchar(255)     null comment '角色说明',
  create_time datetime     not null default current_timestamp comment '创建时间',
  primary key (id),
  unique key uk_role_code (role_code)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='角色表';

-- A3 管理员-角色关联表
drop table if exists sys_user_role;
create table sys_user_role (
  id      bigint not null auto_increment comment '主键',
  user_id bigint not null comment '管理员 id',
  role_id bigint not null comment '角色 id',
  primary key (id),
  unique key uk_user_role (user_id, role_id),
  key idx_role_id (role_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='管理员-角色关联表';

-- ============================================================================
-- B 组：客户与账户（3 张）
-- ============================================================================

-- B1 客户表
drop table if exists customer;
create table customer (
  id          bigint       not null auto_increment comment '主键',
  customer_no varchar(20)  not null comment '客户号',
  real_name   varchar(50)  not null comment '姓名',
  id_card     varchar(18)  not null comment '身份证号',
  phone       varchar(11)  not null comment '手机号',
  gender      tinyint      not null default 1 comment '性别：1 男 2 女',
  address     varchar(200)     null comment '联系地址',
  status      tinyint      not null default 1 comment '状态：1 正常 0 注销',
  create_time datetime     not null default current_timestamp comment '创建时间',
  update_time datetime     not null default current_timestamp on update current_timestamp comment '更新时间',
  primary key (id),
  unique key uk_customer_no (customer_no),
  unique key uk_id_card (id_card),
  key idx_phone (phone)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='客户表';

-- B2 银行卡表
drop table if exists bank_card;
create table bank_card (
  id              bigint       not null auto_increment comment '主键',
  card_no         varchar(19)  not null comment '银行卡号',
  customer_id     bigint       not null comment '所属客户 id',
  card_type       tinyint      not null default 1 comment '卡类型：1 借记卡',
  password        varchar(100) not null comment '交易密码（BCrypt 哈希）',
  pwd_error_count int          not null default 0 comment '连续密码错误次数',
  card_status     tinyint      not null default 1 comment '卡状态：1 正常 2 锁定 3 挂失 4 注销',
  lock_time       datetime         null comment '锁定时间',
  open_time       datetime     not null default current_timestamp comment '开卡时间',
  expire_date     date             null comment '有效期',
  create_time     datetime     not null default current_timestamp comment '创建时间',
  update_time     datetime     not null default current_timestamp on update current_timestamp comment '更新时间',
  primary key (id),
  unique key uk_card_no (card_no),
  key idx_customer_id (customer_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='银行卡表';

-- B3 账户表（钱存在这里）
drop table if exists account;
create table account (
  id                bigint        not null auto_increment comment '主键',
  account_no        varchar(32)   not null comment '账号',
  card_id           bigint        not null comment '所属银行卡 id',
  customer_id       bigint        not null comment '所属客户 id（冗余字段，便于查询）',
  account_type      tinyint       not null default 1 comment '账户类型：1 活期 2 定期',
  currency          char(3)       not null default 'CNY' comment '币种',
  balance           decimal(15,2) not null default 0.00 comment '账面余额',
  available_balance decimal(15,2) not null default 0.00 comment '可用余额',
  frozen_amount     decimal(15,2) not null default 0.00 comment '冻结金额',
  status            tinyint       not null default 1 comment '状态：1 正常 2 冻结 3 销户',
  version           int           not null default 0 comment '乐观锁版本号',
  create_time       datetime      not null default current_timestamp comment '创建时间',
  update_time       datetime      not null default current_timestamp on update current_timestamp comment '更新时间',
  primary key (id),
  unique key uk_account_no (account_no),
  unique key uk_card_type_currency (card_id, account_type, currency),
  key idx_customer_id (customer_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='账户表';

-- ============================================================================
-- C 组：交易（2 张）
-- ============================================================================

-- C1 交易类型字典表
drop table if exists trans_type;
create table trans_type (
  id        int          not null auto_increment comment '主键',
  type_code varchar(20)  not null comment '类型编码',
  type_name varchar(50)  not null comment '类型名称',
  direction tinyint      not null default 0 comment '资金方向：1 收入（账户加钱） 2 支出（账户减钱） 0 无资金变动',
  status    tinyint      not null default 1 comment '状态：1 启用 0 停用',
  sort_no   int          not null default 0 comment '排序号',
  remark    varchar(255)     null comment '备注',
  primary key (id),
  unique key uk_type_code (type_code)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='交易类型字典表';

-- C2 交易流水表（核心表）
drop table if exists trans_record;
create table trans_record (
  id                bigint        not null auto_increment comment '主键',
  trans_no          varchar(32)   not null comment '交易流水号（唯一，防重复提交）',
  trans_type_code   varchar(20)   not null comment '交易类型编码',
  card_no           varchar(19)   not null comment '本方卡号',
  account_id        bigint        not null comment '本方账户 id',
  target_card_no    varchar(19)       null comment '对方卡号（转账用）',
  target_account_id bigint            null comment '对方账户 id（转账用）',
  amount            decimal(15,2) not null comment '交易金额',
  fee               decimal(15,2) not null default 0.00 comment '手续费',
  before_balance    decimal(15,2) not null comment '交易前余额',
  after_balance     decimal(15,2) not null comment '交易后余额',
  currency          char(3)       not null default 'CNY' comment '币种',
  status            tinyint       not null default 1 comment '状态：1 成功 2 失败 3 处理中 4 已冲正',
  fail_reason       varchar(200)      null comment '失败原因',
  device_no         varchar(20)       null comment 'ATM 机具编号',
  session_id        bigint            null comment 'ATM 会话 id',
  remark            varchar(255)      null comment '备注',
  trans_time        datetime      not null default current_timestamp comment '交易时间',
  create_time       datetime      not null default current_timestamp comment '创建时间',
  primary key (id),
  unique key uk_trans_no (trans_no),
  key idx_card_time (card_no, trans_time),
  key idx_account_id (account_id),
  key idx_trans_time (trans_time),
  key idx_device_no (device_no),
  key idx_type_code (trans_type_code)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='交易流水表';

-- ============================================================================
-- D 组：ATM 机具（2 张）
-- ============================================================================

-- D1 ATM 机具表
drop table if exists atm_device;
create table atm_device (
  id                 bigint       not null auto_increment comment '主键',
  device_no          varchar(20)  not null comment '机具编号',
  device_name        varchar(50)  not null comment '机具名称',
  location           varchar(200)     null comment '安装位置',
  ip_address         varchar(50)      null comment 'IP 地址',
  status             tinyint      not null default 1 comment '状态：1 正常 2 维护中 3 停用',
  last_maintain_time datetime         null comment '最后维护时间',
  create_time        datetime     not null default current_timestamp comment '创建时间',
  update_time        datetime     not null default current_timestamp on update current_timestamp comment '更新时间',
  primary key (id),
  unique key uk_device_no (device_no)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='ATM 机具表';

-- D2 ATM 钞箱表
drop table if exists atm_cash_box;
create table atm_cash_box (
  id           bigint        not null auto_increment comment '主键',
  device_no    varchar(20)   not null comment '机具编号',
  box_no       varchar(10)   not null comment '钞箱号',
  denomination int           not null comment '面额（元）：100 / 50',
  total_amount decimal(15,2) not null default 0.00 comment '钞箱余额',
  note_count   int           not null default 0 comment '钞票张数',
  max_amount   decimal(15,2) not null default 0.00 comment '钞箱容量上限',
  status       tinyint       not null default 1 comment '状态：1 正常 2 停用',
  update_time  datetime      not null default current_timestamp on update current_timestamp comment '更新时间',
  primary key (id),
  unique key uk_device_box (device_no, box_no)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='ATM 钞箱表';

-- ============================================================================
-- E 组：审计与对账（3 张）
-- ============================================================================

-- E1 ATM 会话表
drop table if exists atm_session;
create table atm_session (
  id               bigint      not null auto_increment comment '主键',
  session_no       varchar(32) not null comment '会话号',
  card_no          varchar(19) not null comment '卡号',
  device_no        varchar(20)     null comment '机具编号',
  token            varchar(64) not null comment '会话令牌',
  status           tinyint     not null default 1 comment '状态：1 进行中 2 已退卡 3 超时 4 异常',
  login_time       datetime    not null default current_timestamp comment '插卡时间',
  logout_time      datetime        null comment '退卡时间',
  last_active_time datetime        null comment '最后活动时间',
  create_time      datetime    not null default current_timestamp comment '创建时间',
  primary key (id),
  unique key uk_session_no (session_no),
  unique key uk_token (token),
  key idx_card_no (card_no),
  key idx_status (status)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='ATM 会话表';

-- E2 操作日志表
drop table if exists operation_log;
create table operation_log (
  id          bigint       not null auto_increment comment '主键',
  user_id     bigint           null comment '操作人 id',
  username    varchar(50)      null comment '操作人账号',
  module      varchar(50)      null comment '所属模块',
  operation   varchar(100)     null comment '操作描述',
  method      varchar(200)     null comment '目标方法',
  params      text             null comment '请求参数',
  result      text             null comment '返回结果',
  ip          varchar(50)      null comment '来源 IP',
  cost_time   bigint           null comment '耗时（毫秒）',
  status      tinyint      not null default 1 comment '状态：1 成功 0 失败',
  error_msg   varchar(500)     null comment '错误信息',
  create_time datetime     not null default current_timestamp comment '创建时间',
  primary key (id),
  key idx_create_time (create_time),
  key idx_username (username)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='操作日志表';

-- E3 日终轧账表
drop table if exists daily_settlement;
create table daily_settlement (
  id                 bigint        not null auto_increment comment '主键',
  settle_date        date          not null comment '轧账日期',
  device_no          varchar(20)   not null default 'ALL' comment '机具编号（ALL 表示全部机具汇总）',
  total_withdraw     decimal(15,2) not null default 0.00 comment '当日取款总额',
  total_deposit      decimal(15,2) not null default 0.00 comment '当日存款总额',
  total_transfer_out decimal(15,2) not null default 0.00 comment '当日转出总额',
  total_transfer_in  decimal(15,2) not null default 0.00 comment '当日转入总额',
  total_fee          decimal(15,2) not null default 0.00 comment '当日手续费总额',
  trans_count        int           not null default 0 comment '当日交易笔数',
  begin_balance      decimal(15,2) not null default 0.00 comment '期初余额合计',
  end_balance        decimal(15,2) not null default 0.00 comment '期末余额合计',
  expected_change    decimal(15,2) not null default 0.00 comment '应有变动额',
  actual_change      decimal(15,2) not null default 0.00 comment '实际变动额',
  status             tinyint       not null default 1 comment '状态：1 平衡 2 不平衡',
  settle_time        datetime      not null default current_timestamp comment '轧账时间',
  operator_id        bigint            null comment '操作人 id',
  remark             varchar(255)      null comment '备注',
  primary key (id),
  unique key uk_date_device (settle_date, device_no)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='日终轧账表';

-- ============================================================================
-- 建表完成：13 张表
-- ============================================================================
