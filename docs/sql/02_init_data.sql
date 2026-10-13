-- ============================================================================
-- ATM 自助管理系统 —— 初始数据脚本
-- 数据库: atm_db
-- 依赖: 先执行 01_schema.sql 建好 13 张表
-- 本脚本可重复执行（先清理旧数据再插入，每次都是干净的演示状态）
-- ============================================================================
--
-- 【演示账号一览】（密码以 BCrypt 哈希存储，下面是明文，仅供开发测试）
--   管理员端：admin / operator / auditor      密码：admin123
--   银行卡  ：6222020000000001 张三
--             6222020000000002 李四
--             6222020000000003 王五          交易密码统一为：123456
--   ATM 机具：ATM001（含 100 元箱 50000 元、50 元箱 10000 元）
--
-- ============================================================================

use atm_db;

-- ============================================================================
-- 0. 清理旧数据（让脚本可重复执行，每次都是干净的演示状态）
-- ============================================================================
delete from atm_session;
delete from trans_record;
delete from atm_cash_box;
delete from atm_device;
delete from account;
delete from bank_card;
delete from customer;
delete from trans_type;
delete from sys_user_role;
delete from sys_user;
delete from sys_role;

-- ============================================================================
-- 1. 角色（sys_role）
-- ============================================================================
insert into sys_role (id, role_code, role_name, description) values
(1, 'admin',    '系统管理员', '拥有系统全部权限'),
(2, 'operator', '柜员',       '负责日常业务操作，如开卡、加钞'),
(3, 'auditor',  '审计员',     '只能查看数据与报表，不能修改');

-- ============================================================================
-- 2. 管理员账号（sys_user）
--    密码明文 admin123，存储的是 BCrypt 哈希（每次加密自带随机盐）
-- ============================================================================
insert into sys_user (id, username, password, real_name, phone, status) values
(1, 'admin',    '$2a$10$cqGTuUxQN9Etp9bSZE0aPefDwQ4vUj.YM.HBA9CM3S5uDqTb/pB2y', '张伟', '13800000001', 1),
(2, 'operator', '$2a$10$cqGTuUxQN9Etp9bSZE0aPefDwQ4vUj.YM.HBA9CM3S5uDqTb/pB2y', '李静', '13800000002', 1),
(3, 'auditor',  '$2a$10$cqGTuUxQN9Etp9bSZE0aPefDwQ4vUj.YM.HBA9CM3S5uDqTb/pB2y', '王强', '13800000003', 1);

-- ============================================================================
-- 3. 管理员-角色关联（sys_user_role）
-- ============================================================================
insert into sys_user_role (id, user_id, role_id) values
(1, 1, 1),
(2, 2, 2),
(3, 3, 3);

-- ============================================================================
-- 4. 交易类型字典（trans_type）
--    direction 资金方向：1 收入（账户加钱） 2 支出（账户减钱） 0 无资金变动
-- ============================================================================
insert into trans_type (id, type_code, type_name, direction, status, sort_no, remark) values
(1, 'WITHDRAW',     '取款',   2, 1, 10, 'ATM 取现，账户减钱、钞箱减钱'),
(2, 'DEPOSIT',      '存款',   1, 1, 20, 'ATM 存款，账户加钱、钞箱加钱'),
(3, 'TRANSFER_OUT', '转出',   2, 1, 30, '转账转出方，账户减钱'),
(4, 'TRANSFER_IN',  '转入',   1, 1, 40, '转账转入方，账户加钱'),
(5, 'FEE',          '手续费', 2, 1, 50, '转账手续费，0.5%，最低 1 元最高 50 元'),
(6, 'QUERY',        '余额查询', 0, 1, 60, '只查询，无资金变动');

-- ============================================================================
-- 5. 客户（customer）
--    身份证号与手机号均为虚构测试数据
-- ============================================================================
insert into customer (id, customer_no, real_name, id_card, phone, gender, address, status) values
(1, 'C20260001', '张三', '110101199001010011', '13800000101', 1, '北京市朝阳区建国路 1 号',   1),
(2, 'C20260002', '李四', '110101199002020022', '13800000102', 2, '上海市浦东新区世纪大道 2 号', 1),
(3, 'C20260003', '王五', '110101199003030033', '13800000103', 1, '广州市天河区体育西路 3 号',   1);

-- ============================================================================
-- 6. 银行卡（bank_card）
--    交易密码明文 123456，存储 BCrypt 哈希
--    pwd_error_count = 0（连续密码错误次数）；card_status = 1（正常）
-- ============================================================================
insert into bank_card (id, card_no, customer_id, card_type, password, pwd_error_count, card_status, expire_date) values
(1, '6222020000000001', 1, 1, '$2a$10$zJsrN0H6ntV2iimmiA5zAuXtoPyAk9cPNBJL0e2F47GcWUH26t5GC', 0, 1, '2030-12-31'),
(2, '6222020000000002', 2, 1, '$2a$10$zJsrN0H6ntV2iimmiA5zAuXtoPyAk9cPNBJL0e2F47GcWUH26t5GC', 0, 1, '2030-12-31'),
(3, '6222020000000003', 3, 1, '$2a$10$zJsrN0H6ntV2iimmiA5zAuXtoPyAk9cPNBJL0e2F47GcWUH26t5GC', 0, 1, '2030-12-31');

-- ============================================================================
-- 7. 账户（account）—— 钱存在这里
--    初始余额：张三 10000 元、李四 5000 元、王五 1000 元
--    version = 0（乐观锁版本号）
-- ============================================================================
insert into account (id, account_no, card_id, customer_id, account_type, currency, balance, available_balance, frozen_amount, status, version) values
(1, '622202000000000101', 1, 1, 1, 'CNY', 10000.00, 10000.00, 0.00, 1, 0),
(2, '622202000000000201', 2, 2, 1, 'CNY',  5000.00,  5000.00, 0.00, 1, 0),
(3, '622202000000000301', 3, 3, 1, 'CNY',  1000.00,  1000.00, 0.00, 1, 0);

-- ============================================================================
-- 8. ATM 机具（atm_device）
-- ============================================================================
insert into atm_device (id, device_no, device_name, location, ip_address, status) values
(1, 'ATM001', '1 号 ATM 机', '北京市朝阳区建国路 1 号 一层大堂', '192.168.1.101', 1);

-- ============================================================================
-- 9. ATM 钞箱（atm_cash_box）—— 取款时判断"机器里还有没有钱"
--    BOX100：100 元面额 500 张 = 50000 元
--    BOX050： 50 元面额 200 张 = 10000 元
--    合计现金 60000 元
-- ============================================================================
insert into atm_cash_box (id, device_no, box_no, denomination, total_amount, note_count, max_amount, status) values
(1, 'ATM001', 'BOX100', 100, 50000.00, 500, 200000.00, 1),
(2, 'ATM001', 'BOX050',  50, 10000.00, 200, 100000.00, 1);

-- ============================================================================
-- 10. 初始数据验收自检
-- ============================================================================
select '角色 sys_role'           as 数据表, count(*) as 行数 from sys_role
union all select '管理员 sys_user',       count(*) from sys_user
union all select '角色关联 sys_user_role', count(*) from sys_user_role
union all select '交易类型 trans_type',    count(*) from trans_type
union all select '客户 customer',         count(*) from customer
union all select '银行卡 bank_card',      count(*) from bank_card
union all select '账户 account',          count(*) from account
union all select '机具 atm_device',       count(*) from atm_device
union all select '钞箱 atm_cash_box',     count(*) from atm_cash_box;

select c.real_name as 客户, k.card_no as 卡号, a.balance as 余额, k.card_status as 卡状态
from customer c
join bank_card k on k.customer_id = c.id
join account   a on a.card_id = k.id
order by c.id;

select device_no as 机具, box_no as 钞箱, denomination as 面额,
       note_count as 张数, total_amount as 金额
from atm_cash_box
order by denomination desc;

-- ============================================================================
-- 初始数据完成
-- ============================================================================
