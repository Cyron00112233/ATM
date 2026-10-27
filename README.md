# ATM 自助管理系统

基于 **Spring Boot 3 + MyBatis + Flutter** 的 ATM 自助服务系统，覆盖插卡、交易密码验证、取款、存款、转账、余额查询、交易明细、退卡等完整业务闭环。

后端采用经典三层架构（Controller / Service / Mapper），前端为 Flutter Windows 桌面应用，通过 HTTP + JSON 与后端通信。

---

## 功能

### ATM 机具端（已实现）

| 功能 | 说明 |
|---|---|
| 插卡 | 校验卡号、卡状态、有效期，建立 5 分钟会话 |
| 交易密码验证 | BCrypt 校验，连错 3 次自动锁卡 |
| 取款 | 单笔 100~5000 元、100 整数倍；事务内扣余额 + 扣钞箱 + 记流水 |
| 存款 | 单笔 100~10000 元、100 整数倍；加余额 + 加钞箱 + 记流水 |
| 转账 | 手续费 0.5%（最低 1 元、最高 50 元）；双边更新 + 记 3 条流水 |
| 余额查询 | 返回账面余额与可用余额 |
| 交易明细 | 最近 N 笔（默认 10、上限 50），倒序，含中文交易类型 |
| 退卡 | 立即结束会话，令牌失效（前端有二次确认） |

### 管理端（预留接口）

- 管理员登录（BCrypt + JWT）
- 客户查询：列表 / 按 id / 按姓名模糊 / 总数

> 管理端界面、统计报表、日终轧账、电子凭条不在本期范围内，数据库表结构已预留。

---

## 技术栈

### 后端

| 组件 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.16 |
| MyBatis（starter） | 3.0.5 |
| MySQL | 8.4 |
| JWT（jjwt） | 0.12.5 |
| 密码加密 | BCrypt（spring-security-crypto） |
| 构建 | Maven |

### 前端

| 组件 | 版本 |
|---|---|
| Flutter | 3.44.8（stable） |
| Dart SDK | ^3.12.2 |
| HTTP | http ^1.6.0 |
| 目标平台 | Windows 桌面 |

---

## 项目结构

```
atm/
├── atm-server/                     后端（Spring Boot）
│   ├── src/main/java/com/atm/
│   │   ├── common/                 统一响应、错误码、业务异常、全局异常处理
│   │   ├── config/                 SecurityConfig、WebMvcConfig
│   │   ├── controller/             Hello / Admin / Customer / Atm
│   │   ├── dto/                    请求体与响应体（16 个）
│   │   ├── entity/                 数据库表实体（7 个）
│   │   ├── interceptor/            登录拦截器（JWT）
│   │   ├── mapper/                 数据访问接口（7 个）
│   │   ├── service/impl/           业务实现（AtmServiceImpl 为核心）
│   │   └── util/                   JwtUtil
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── mapper/                 MyBatis XML（7 个，共 25 条 SQL）
│   └── pom.xml
│
├── atm-app/                        前端（Flutter）
│   ├── lib/
│   │   ├── main.dart               单页状态机 + 6 个界面
│   │   └── api.dart                统一 POST 封装
│   ├── test/widget_test.dart       冒烟测试
│   └── pubspec.yaml
│
├── docs/
│   ├── sql/01_schema.sql           建表脚本（13 张表）
│   ├── sql/02_init_data.sql        初始化数据（9 条 insert，覆盖 9 张表）
│   ├── 接口文档.md                  14 个接口的完整规格
│   └── 后端设计说明.md               架构、链路、关键设计决策
└── README.md
```

**规模**：后端 50 个 Java 文件 / 约 2400 行 + 7 个 Mapper XML；前端 2 个 Dart 文件 / 约 740 行。

---

## 快速开始

### 环境要求

```
JDK 21
MySQL 8.x
Flutter 3.44+（仅前端需要）
```

### 1. 初始化数据库

在 Navicat / DBeaver 里新建查询窗口逐段执行，或在 `mysql` 命令行里用 `source`：

```sql
-- ① 先手动建库（脚本里没有 create database）
create database if not exists atm_db
  default charset utf8mb4 collate utf8mb4_general_ci;

-- ② 再按顺序执行两个脚本（顺序不能反）
source docs/sql/01_schema.sql;    -- 建表（13 张，可重复执行）
source docs/sql/02_init_data.sql; -- 演示数据（9 条 insert）
```

### 2. 配置数据库连接

修改 `atm-server/src/main/resources/application.properties`：

```properties
spring.datasource.username=root
spring.datasource.password=你的密码
```

### 3. 启动后端

在 IDEA 中运行 `AtmServerApplication`，或用命令行：

```bash
cd atm-server
mvn spring-boot:run
```

验证：

```bash
curl http://localhost:8080/api/hello
```

返回 `{"code":200,...}` 即启动成功。**服务默认监听 8080 端口。**

### 4. 启动前端

```bash
cd atm-app
flutter pub get
flutter run -d windows
```

> 也可以 `flutter build windows --release`，然后直接运行 `build/windows/x64/runner/Release/atm_app.exe`。

---

## 测试账号

### 银行卡（ATM 机具端）

| 卡号 | 持卡人 | 初始余额 | 交易密码 |
|---|---|---|---|
| 6222020000000001 | 张三 | 10000.00 | 123456 |
| 6222020000000002 | 李四 | 5000.00 | 123456 |
| 6222020000000003 | 王五 | 1000.00 | 123456 |

**ATM 机具编号**：`ATM001`（钞箱：100 元券 500 张、50 元券 200 张）

### 管理员（管理端接口）

| 账号 | 密码 | 角色 |
|---|---|---|
| admin | admin123 | admin |
| operator | admin123 | operator |
| auditor | admin123 | auditor |

---

## 接口

完整规格见 [docs/接口文档.md](docs/接口文档.md)，共 14 个接口：

| 分组 | 接口 |
|---|---|
| 通用 | `GET /api/hello` |
| 管理端 | `POST /api/admin/login` |
| 客户 | `GET /api/customer/list`、`/get/{id}`、`/search`、`/count` |
| ATM | `POST /api/atm/insert-card`、`/verify-password`、`/eject-card`、`/withdraw`、`/deposit`、`/balance`、`/transactions`、`/transfer` |

**统一响应格式：**

```json
{ "code": 200, "message": "操作成功", "data": {}, "timestamp": 1792893055095 }
```

**两套认证机制：**

- 管理端：无状态 JWT，放在请求头 `Authorization: Bearer <token>`
- ATM 端：数据库会话令牌，放在请求体 `sessionToken`（5 分钟无操作超时，可立即失效）

---

## 技术要点

- **统一响应 + 全局异常处理**：所有接口返回同一结构，Controller 层零 try-catch
- **DTO 与 Entity 分离**：阻止密码哈希、内部 id 等字段泄露到接口
- **事务边界精确控制**：取款/存款/转账加 `@Transactional`；密码验证刻意不加（否则错误次数会被回滚，导致可无限试密码）
- **并发安全**：扣款用条件更新 `where available_balance >= #{amount}`，靠数据库行锁避免超扣
- **防超时攻击**：密码错误次数用原子自增 `set pwd_error_count = pwd_error_count + 1`
- **会话双阶段校验**：`status = 1` 且 `verified = 1` 才允许资金操作，防止跳过密码直接取款
- **转账防死锁**：按 `account_id` 升序更新两方账户
- **手续费三档计算**：`amount × 0.005`，结果夹在 [1, 50]，用 `BigDecimal` + `RoundingMode` 避免浮点误差
- **多条流水**：转账记 3 条（转出 / 手续费 / 转入），`trans_no` 加 `F`/`I` 后缀保证唯一索引不冲突
- **幂等**：`trans_record.trans_no` 唯一索引兜底重复提交
- **前端单页状态机**：`enum Step` + `switch` 组织 6 个界面，避免多页面路由传参

详细设计说明见 [docs/后端设计说明.md](docs/后端设计说明.md)。

---

## 已知限制

| 项目 | 说明 |
|---|---|
| 日累计限额 | 仅实现单笔限额，未实现「单日累计 20000 元 / 10 次」 |
| 失败交易流水 | 交易失败时异常回滚，流水表不记录（仅写应用日志） |
| 接口 404 | 路径不存在时被兜底异常处理器包装为 `code=500`，语义应为 404 |
| 管理端界面 | 仅提供接口，无 UI |
| 统计报表 / 日终轧账 / 电子凭条 | 表结构已设计，功能未实现 |

---

## 开发进度

- [x] 数据库设计（13 张表 + 种子数据）
- [x] 统一响应与全局异常处理
- [x] 客户模块、管理员登录 + JWT
- [x] ATM 会话与交易密码（含连错 3 次锁卡）
- [x] 取款 / 存款 / 转账 / 余额查询 / 交易明细
- [x] 会话 verified 双阶段安全校验
- [x] Flutter 前端六项功能
- [x] 接口文档与后端设计说明
- [ ] 日累计限额
- [ ] 单元测试
- [ ] Flutter Web 版
