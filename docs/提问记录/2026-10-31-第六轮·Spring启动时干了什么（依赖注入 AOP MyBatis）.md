# 第六轮 · Spring 启动时干了什么（依赖注入 / AOP / MyBatis XML）（2026-10-31）

**背景**：第 2 阶段（读懂代码）继续。主题是**"为什么加几个注解，那些对象就自动出现了"**。

**检验结果：10 道题，8 ✅ / 1 ⚠️ / 1 ❌（答对率 80%）**

**AOP 那 3 题全对 —— 是目前所有检验里表现最好的一块。**

---

# 第一部分：依赖注入（Bean 是怎么被创建和装配的）

## 讲解要点

### 问题入口

`AtmServiceImpl` 的 7 个字段全是 `final` 且没有赋值：

```java
private final BankCardMapper bankCardMapper;
private final AtmSessionMapper atmSessionMapper;
private final CustomerMapper customerMapper;
private final PasswordEncoder passwordEncoder;
private final AccountMapper accountMapper;
private final AtmCashBoxMapper cashBoxMapper;
private final TransRecordMapper transRecordMapper;
```

**但它们能正常调用 —— 对象是谁创建的？**

### 三个注解各干什么

**① `@Service`** —— 把这个类交给 Spring 管

> "Spring，启动时创建一个这个类的实例，存进容器（Bean），别人需要就找你拿。"

**容器本质是一个大 Map：**

```
"AtmService"       → AtmServiceImpl 实例
"BankCardMapper"   → MyBatis 生成的代理实例
"PasswordEncoder"  → BCryptPasswordEncoder 实例
"JwtUtil"          → JwtUtil 实例
```

**② `@Mapper`** —— 让 MyBatis 生成代理并交给 Spring

```
只有接口，没有实现类
    ↓
你根本 new 不出来（接口不能实例化，也没有 Impl 类）
    ↓
MyBatis 启动时用 JDK 动态代理生成实现对象
    ↓
注册进 Spring 容器
    ↓
Service 才能拿到它
```

**③ `@RequiredArgsConstructor`** —— Lombok 生成构造函数（只处理 `final` 字段）

```java
// 自动生成
public AtmServiceImpl(BankCardMapper bankCardMapper, AtmSessionMapper atmSessionMapper, ...) {
    this.bankCardMapper = bankCardMapper;
    ...
}
```

### Spring 启动的装配过程

```
① 扫描 com.atm 包，找 @Component / @Service / @Controller / @Configuration
② 对每个类看构造函数需要什么参数（AtmServiceImpl 需要 7 个）
③ 去容器里找这 7 个类型的 Bean
④ 全部找齐 → 调构造函数 → 创建实例
⑤ 把实例放进容器
⑥ Controller 需要 AtmService → 从容器拿到那个实例
```

### 三种注入方式

| 方式 | 写法 | 评价 |
|---|---|---|
| 字段注入 | `@Autowired private Xxx x;` | 字段不能 final，测试难替换 |
| setter 注入 | `@Autowired public void setXxx(...)` | 很少用 |
| **构造器注入** | `@RequiredArgsConstructor` + `private final` | ✅ 本项目用 |

**为什么推荐构造器注入：**

```
① 字段可以 final → 注入后不可改，线程安全
② 依赖一目了然（看构造函数就知道需要什么）
③ 循环依赖会立刻报错（字段注入延迟到运行时才炸）
④ 测试时可以手动 new：new AtmServiceImpl(假mapper1, 假mapper2, ...)
```

### 关键认知

> **依赖注入 = 你不 new 对象，Spring 帮你 new 好塞进去。**
> **在本项目里它不是"优雅写法"，而是"唯一可行的写法"** —— MyBatis 的 Mapper 你 new 不出来。

---

## 检验 1-3

### Q1：`PasswordEncoder` 是谁注册的？

**你的答案**：由 SecurityConfig 的 @Bean 方法注册

**评分**：✅ **完全正确**

**补充：注册 Bean 的两种方式**

```
① 加注解让 Spring 自己扫（@Service / @Component / @RestController）
② 在 @Configuration 类里用 @Bean 方法手动造

BCryptPasswordEncoder 是 Spring Security 的类，你不能改它源码加 @Service
→ 只能用 ② 的 @Bean 方式
```

### Q2：构造函数需要 7 个参数，容器里只找到 6 个，会怎样？ ❌

**你的答案**：编译时会报错，编译不通过

**评分**：❌ **错误 —— 这是运行期（启动时）错误**

**正确答案：编译能过，但应用启动失败。**

```
编译阶段：javac 只看语法和类型
    构造函数需要 7 个参数（Lombok 生成）
    final 字段都被构造函数赋值
    → 编译通过 ✅

运行阶段：Spring 启动时才去容器找 Bean
    缺 1 个 → 抛异常 → 应用启动不起来
```

**真实报错：**

```
***************************
APPLICATION FAILED TO START
***************************

Description:
Parameter 0 of constructor in com.atm.service.impl.AtmServiceImpl
required a bean of type 'com.atm.mapper.BankCardMapper' that could not be found.
```

**编译器不懂 Spring 容器，容器也不知道编译器 —— 两个世界。**

**用自己踩过的坑对照：**

| 阶段 | 报错长什么样 | 项目里的例子 |
|---|---|---|
| 编译期 | `找不到符号`、`方法未实现` | `AtmServiceImp` 拼错、`withdraw` 加到错误接口 |
| 启动期 | `APPLICATION FAILED TO START` | 缺 Bean、数据库连不上、端口占用 |
| 运行期 | 接口报错 / 后台异常 | `BindingException: Parameter 'deviceNo' not found` |

**判断口诀：**
```
IDEA 有红线        → 编译期
无红线但启动报错    → 容器/配置
启动成功但接口报错  → 运行时
```

### Q3：Mapper 为什么自己 new 不出来，但能调用？

**你的答案**：Mapper 没有实现接口，但 jdk 能自动代理，能实现接口

**评分**：✅ 正确（措辞：不是"没有实现接口"，是"没有实现**类**"）

---

# 第二部分：AOP 与事务代理

## 讲解要点

### 问题入口

```java
@Transactional(rollbackFor = Exception.class)      // 就一行注解
public WithdrawResponse withdraw(WithdrawRequest request) {
    // 方法体里【没有任何】"begin transaction" 的代码
}
```

**事务在哪开的？谁提交、谁回滚？**

### 答案：Spring 给你的不是"你自己"，是**代理对象**

```
你写的：          AtmServiceImpl（原生对象）
Spring 放进容器的： AtmServiceImpl 的【代理对象】
```

**调用链：**

```
Controller: atmService.withdraw(request)
        ↓
实际调到【代理对象】的 withdraw()
        ↓
代理：① 开启事务
        ↓
代理：② 调用原生对象的 withdraw()   ← 你写的业务代码
        ↓
     ┌──┴──┐
  正常返回  抛异常
     ↓       ↓
  ③ COMMIT  ③ ROLLBACK
            ④ 把异常继续往上抛（给 GlobalExceptionHandler）
```

### 代理的伪代码

```java
public class AtmServiceImpl$$Proxy extends AtmServiceImpl {

    @Override
    public WithdrawResponse withdraw(WithdrawRequest request) {
        TransactionStatus tx = transactionManager.begin();       // ① 开事务
        try {
            WithdrawResponse result = super.withdraw(request);   // ② 调真实方法
            transactionManager.commit(tx);                        // ③ 提交
            return result;
        } catch (RuntimeException e) {
            transactionManager.rollback(tx);                      // ④ 回滚
            throw e;                                              // ⑤ 继续抛
        }
    }
}
```

**`@Transactional` 的本质：Spring 创建 Bean 时发现有这个注解，就生成一个【子类】（CGLIB 继承代理），在方法前后包上事务代码。**

### 这解释了四件"奇怪"的事

**① 为什么加在 `private` 方法上无效**

```
CGLIB 靠"继承 + 覆盖方法"
private 方法不能被覆盖 → 代理插不进去 → 注解白加
（final 方法同理）
```

**② ⭐ 为什么同一个类里 A 调 B，B 的事务不生效（经典坑）**

```java
public void a() {
    this.b();          // ← this 是原生对象，不走代理
}

@Transactional
public void b() { }
```

```
外部调 a() → 代理.a()
    · 代理看 a() 没有 @Transactional → 不开事务
    · 执行 super.a()（原生对象）
        · this.b()  ← 原生对象自己调自己，【不经过代理】
        · b() 上的 @Transactional 【完全被无视】
```

**三种解法：**
```
① 把 b() 拆到另一个 Bean（跨 Bean 调用就会走代理）
② 注入自己：@Autowired private AtmService self; → self.b()
③ 用 TransactionTemplate 手动控制
```

**③ 为什么 `rollbackFor = Exception.class` 要显式写**

```
Spring 默认规则：
    抛 RuntimeException / Error  → 回滚 ✅
    抛【受检异常】              → 不回滚（默认提交！）❌

显式写 rollbackFor = Exception.class = "任何异常都回滚"
```

**④ 为什么 `verifyPassword` 绝不能加**

```
加了事务 → 抛 BusinessException(RuntimeException) → 代理执行 ROLLBACK
        → "错误次数 +1" 和 "锁卡" 都被撤销
        → 计数永远在 0~1 → 永远锁不了卡 → 可无限试密码
```

### AOP 一句话

> **把"横切关注点"（事务、日志、权限）从业务代码里抽出来，用代理统一插进去。**

**没有 AOP 时每个方法要写 8 行（begin / try / commit / catch / rollback / throw）；有 AOP 只要 1 行注解。**

### 项目里的三种"代理/拦截"

| 机制 | 实现方式 | 位置 |
|---|---|---|
| 事务 | CGLIB 继承代理 | `@Transactional` |
| MyBatis Mapper | JDK 动态代理（基于接口） | 7 个 Mapper 接口 |
| 登录校验 | Spring MVC 拦截器（**不是代理**） | `LoginInterceptor` |

---

## 检验 4-6（全对 ✅✅✅）

### Q4：为什么 `@Transactional` 加在 private 方法上不生效

**你的答案**：因为 CGLIB 靠"继承+覆盖方法"来实现代理，而 private 方法不能被覆盖，代理插不进去，注解白加

**评分**：✅ **完全正确**

### Q5：同一个类里 A 调 B（B 有 @Transactional），事务生效吗？

**你的答案**：不会，A 里面没有事务，A 调 super.a，而 b 调 this.b()，不会经过代理，所以事务失效

**评分**：✅ **正确**

### Q6：如果 Spring 改用 JDK 动态代理实现事务，Service 必须有接口吗？

**你的答案**：要，JDK 的动态代理要基于接口才能实现

**评分**：✅ **完全正确**

**有意思的对比 —— 项目里正好两种代理都用到：**

| | 有接口吗 | 只能用哪种 |
|---|---|---|
| Service | `AtmService` + `AtmServiceImpl` | 两种都行 |
| Mapper | 只有接口，没有实现类 | **只能用 JDK 动态代理** |

**所以"Mapper 为什么必须是接口"有了更深的答案：**
```
Mapper 没有实现类 → 只能靠 JDK 动态代理
JDK 动态代理【只能代理接口】
→ 所以 Mapper 必须是接口，不能是抽象类
```

---

# 第三部分：MyBatis XML

## 讲解要点

### ① 骨架

```xml
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.atm.mapper.AtmSessionMapper">
```

| 部分 | 作用 |
|---|---|
| `DOCTYPE` | XML 语法声明，IDEA 靠它提供自动补全 |
| `namespace` | 绑定到哪个接口（写接口全名） |

### ② `#{}` 的底层

```xml
where token = #{token}
    ↓ MyBatis 编译成
where token = ?          ← PreparedStatement 占位符
    ↓ 再绑定参数值
```

**为什么防注入：**

```
数据库看到的是：
    SQL 结构（固定死）：where token = ?
    参数值           ："' or '1'='1"

参数被当作【普通字符串值】比较，永远不会被解释成 SQL 代码 ✅

对比 ${}：
    拼出来的 SQL 结构被改变了：
    where token = '' or '1'='1'
    → 多出一个永远成立的条件 → 查出所有数据 💥
```

### ③ `resultType` + 驼峰映射

```
数据库列 session_no  →  Java 字段 sessionNo
                ↓
mybatis.configuration.map-underscore-to-camel-case=true
```

**三个层次的报错（重要）：**

| 现象 | 原因 |
|---|---|
| `Invalid bound statement (not found)` | XML 没绑上（namespace 错 / XML 没加载） |
| `Unknown column 'xxx' in 'field list'` | SQL 里的列名在表里不存在 |
| **字段静默为 null（不报错）** | 列名对，但和实体字段映射不上 |

**第三种最阴** —— 这就是为什么用 `select <列清单> from 表 limit 0` 让数据库检查列名的原因。

### ④ `useGeneratedKeys` + `keyProperty`

```xml
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
```

| 属性 | 意思 |
|---|---|
| `useGeneratedKeys="true"` | 用数据库生成的主键，插入后**取回来** |
| `keyProperty="id"` | 取回来塞进参数对象的 `id` 属性 |

**不加的后果：**

```
insert 成功，数据库有 id=11
但 Java 的 session.getId() 还是 null
    ↓
下一行：updateLastActive(session.getId())   // 传 null
    → update ... where id = null
    → 影响 0 行（静默失败，不报错！）
```

**⚠️ 注意：它管"回填",不管"自增"** —— 自增是表定义里 `auto_increment` 决定的，主键永远唯一。

### ⑤ `case when`（`updateStatus` 的巧妙）

```xml
<update id="updateStatus">
    update atm_session
    set status = #{status},
        logout_time = case when #{status} = 2 then now() else logout_time end
    where id = #{id}
</update>
```

**翻译成 Java：**

```java
if (status == 2) {
    logoutTime = now();           // 退卡 → 记录时间
} else {
    logoutTime = logoutTime;      // 其它 → 保持原值
}
```

**效果：**
```
退卡 updateStatus(id, 2) → status=2, logout_time=now()     ✅
超时 updateStatus(id, 3) → status=3, logout_time 不变       ✅
```

**一个 Mapper 方法服务两个场景。**

### ⑥ `<sql>` + `<include>`（AccountMapper）

```xml
<sql id="baseColumns">
    id, account_no, card_id, customer_id, ...
</sql>

<select id="selectByCardId" resultType="com.atm.entity.Account">
    select <include refid="baseColumns"/>
    from account where card_id = #{cardId}
</select>
```

```
表加字段 → 只改 <sql> 一处 → 所有查询自动生效
```

**⚠️ 踩过的坑：`select<include .../>` 中间没空格 → 拼成 `selectid,account_no,...` → SQL 语法错误。**

### ⑦ 动态 SQL（项目没用，但要知道）

```xml
<where>
    <if test="name != null and name != ''">
        and real_name like concat('%', #{name}, '%')
    </if>
</where>
```

**`<where>` 干三件智能的事：**
```
① 有条件时自动加 where
② 第一个条件是 and 时自动去掉 and
③ 所有条件都不成立时不加 where
```

### ⑧ 完整链路

**启动时：**
```
① 扫描 @Mapper 接口
② JDK 动态代理生成实现
③ 解析 XML：namespace → 接口，<select id> → 方法
④ 代理对象注册进 Spring 容器
```

**运行时：**
```
⑤ atmSessionMapper.selectByToken(token)
⑥ 代理拦截 → 找 namespace + id 对应的 SQL
⑦ #{} 编译成 ?，绑定参数
⑧ 执行 → ResultSet
⑨ resultType + 驼峰映射 → 每行变成一个对象
⑩ 返回 List
```

---

## 检验 7-10

### Q7：`#{}` 和 `${}` 的区别

**你的答案**：`#{}` 用来传值，`${}` 用来拼列名或表名；`#{}` 开始的时候会预编译

**评分**：✅ 正确（补充了"为什么预编译能防注入"的机制）

### Q8：`useGeneratedKeys` 和 `keyProperty` 是什么意思？ ⚠️

**你的答案**：第一个代表自增，第二个代表什么要自增，这里是 id，会导致 id 一直是一样的，一个 id 对应多个用户

**评分**：⚠️ **理解偏了**

**正确含义：**
```
useGeneratedKeys="true" → 用数据库生成的主键，插入后【取回来】
keyProperty="id"        → 取回来塞进参数对象的【哪个属性】
```

**它管"回填",不管"自增"。** 自增是表定义里 `auto_increment` 决定的，**主键永远唯一，不会重复**。

**不加的后果：`session.getId()` 为 null → 后续按 id 更新时影响 0 行（静默失败）。**

### Q9：`case when` 是干什么的？

**你的答案**：用来做判断的，相当于 java 里面的 if else，不是这种就是另外的

**评分**：✅ 正确（"不是这种就是另外的" = else 分支，理解到位）

**具体到这一行：`else logout_time` = 保持原值，所以超时不会改退卡时间。**

### Q10：namespace 拼错，什么时候出错？报什么错？

**你的答案**：运行时报错，会报找不到这个字段的异常

**评分**：✅ 时机对（运行期）；⚠️ 报错内容要修正

**正确报错：**

```
Invalid bound statement (not found): com.atm.mapper.AtmSessionMapper.selectByToken
```

**中文意思**："我找不到这个命名空间下 `selectByToken` 这个语句。"

**两种原因：① namespace 写错 ② `mybatis.mapper-locations` 配错，XML 没加载。**

---

# 统计

| 部分 | 题数 | 结果 |
|---|---|---|
| 依赖注入 | 3 | ✅❌✅ |
| AOP / 事务代理 | 3 | ✅✅✅ |
| MyBatis XML | 4 | ✅⚠️✅✅ |

```
✅ 8 个        ⚠️ 1 个        ❌ 1 个        答对率 80%
```

**AOP 那 3 题全对 —— 目前所有检验里表现最好的一块。**

---

# 今天的三条核心认知

```
① 编译器 和 Spring 容器 是两个世界
    编译期只管语法和类型
    缺 Bean 是运行期（启动时）的事

② 代理是这个项目里三种机制的基础
    事务    → CGLIB 继承代理（所以 private/final 方法无效）
    Mapper  → JDK 动态代理（所以必须是接口）
    拦截器  → 不是代理，是框架在固定时机回调

③ "接口"在这个项目里有两层意义
    设计层：解耦、可测试、契约
    技术层：JDK 动态代理只能代理接口 → Mapper 必须是接口
```

---

# 待巩固

```
★★  useGeneratedKeys/keyProperty = 把自增主键【回填】到对象，不控制自增
★★  编译期 vs 启动期 vs 运行期 三种错误的区分
★   Invalid bound statement ≠ 找不到字段（两种不同报错）
★   case when 的 else 分支 = 保持原值
```

---

# 第 2 阶段进度

```
✅ Stream / Lambda / 方法引用      第五轮
✅ JWT                            第五轮
✅ 拦截器                          第五轮
✅ 依赖注入 / Bean 装配            第六轮
✅ AOP / 事务代理                  第六轮
✅ MyBatis XML                    第六轮
──────────────────────────────────────────
⬜ Dart 基础（空安全 / 异步 / 命名参数）  ← 前端唯一短板
⬜ 怎么系统性地读一个陌生的类
⬜ Spring Boot 自动配置
```
