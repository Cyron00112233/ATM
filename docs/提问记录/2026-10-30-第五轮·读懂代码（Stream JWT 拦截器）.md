# 第五轮 · 读懂代码（Stream / JWT / 拦截器）（2026-10-30）

**背景**：你提出"目标不是能写代码，而是能看懂每一行、知道方法在哪、看得懂专业写法（Stream / JWT / 拦截器）"。

**这一轮不是问答，是"讲解 + 检验"：讲完一块，出几道题验证是否真懂。**

**检验结果：8 道检验题，6 ✅ / 1 ⚠️ / 1 ❌（答对率 75%）**

---

# 第一部分：Stream / Lambda / 方法引用

## 讲解要点

### Lambda 是什么

> 一个"没有名字的方法"，可以当参数传。

```java
(参数) -> { 方法体 }
r -> r.getAmount()                  // Lambda
FieldError::getDefaultMessage       // 方法引用（Lambda 的简写）
```

### Stream 是什么

> 把"对集合的批量处理"写成一条流水线：**数据源 → 中间操作 → 终止操作**。

**用"传送带"理解：**

```
records          →  一筐货
.stream()        →  倒上传送带
.filter(条件)    →  中间设关卡，不合格的拿下来
.map(转换)       →  加工车间，每个货变成另一种货
.count()         →  尽头数一数
.collect(toList()) → 尽头装进箱子
```

### 逐行对照 for 循环（核心理解）

| Stream 写法 | for 循环对应 |
|---|---|
| `.stream()` | `for (TransRecord r : records) {` |
| `.filter(r -> 条件)` | `if (条件) {` |
| `.map(r -> 新东西)` | 循环体里造新对象 + `result.add(...)` |
| `.count()` | `count++`，最后返回 |
| `.collect(Collectors.toList())` | 循环结束，`return result` |

**结论：Stream 不是新语法，就是把 for 循环的三段拆成三个方法名。**

### filter 和 map 的区别

```
.filter  → 【筛】数量会变少，元素本身不变
.map     → 【换】数量不变，每个元素变成另一种东西
```

### 读 Stream 的口诀：从右往左

```
① 先看【终点】：要变成什么？（toList / count / findFirst）
② 再看【起点】：数据从哪来？（哪个 List）
③ 最后看【中间】：每个元素怎么筛 / 怎么换？
```

### 项目里的三处 Stream

```java
// ① GlobalExceptionHandler L38-41 —— 筛出第一个校验错误
e.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)      // 每个 FieldError → 取提示文字
        .findFirst()                             // 拿第一个
        .orElse(ResultCode.PARAM_ERROR.getMessage());  // 没有就用默认值

// ② AtmServiceImpl L374-387 —— Entity 列表转 DTO 列表
records.stream().map(r -> {
    TransDetailItem item = new TransDetailItem();
    item.setTransNo(r.getTransNo());
    ... 搬 9 个字段 ...
    return item;
}).collect(Collectors.toList());
```

---

## 检验 1：Stream 读代码

**题目**：

```java
List<TransRecord> records = transRecordMapper.selectByCardNo(cardNo, 10);

long count = records.stream()
        .filter(r -> "WITHDRAW".equals(r.getTransTypeCode()))
        .count();
```

**问**：`count` 是什么值？

**你的第一次回答**：读不懂

**处理方式**：改用"具体数据走一遍"的方式 —— 假设 records 里 3 条流水（2 条取款 1 条存款），逐步写出每一步之后"传送带上剩什么"，最后得出 `count = 2`。

**换一个最简例子再试**：

```java
List<Integer> nums = List.of(1, 2, 3, 4, 5);
long n = nums.stream().filter(x -> x > 3).count();      // 你的答案：2 ✅
long m = nums.stream().filter(x -> x % 2 == 0).count(); // 你的答案：2 ✅
```

**评分**：✅✅ 两道都对了，模型建立成功

**关键教学点**：**"读不懂"往往不是逻辑问题，是缺一个心智模型。** 换成"传送带 + 具体数据走一遍"，立刻就通了。

---

# 第二部分：JWT

## 讲解要点

### 三段结构（现场用 PowerShell 造的 token 演示）

```
eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJhZG1pbiIsInJvbGUiOiJhZG1pbiIsImlhdCI6MTc5MzQ5MzA1NSwiZXhwIjoxNzkzNDk2NjU1fQ.a_Qp1QKJ0p4ioBsbURUh56lQVvYZMcciJmU6fYIiWgZI2RxW2ioJvqjh_E595KOu
└──── header ────┘ └────────────────── payload ──────────────────┘ └──── signature ────┘
```

| 段 | 能否解开 | 内容 |
|---|---|---|
| header | ✅ base64 解码 → `{"alg":"HS384"}` | 算法 |
| payload | ✅ base64 解码 → `{"sub":"1","username":"admin","role":"admin","iat":...,"exp":...}` | 数据 |
| signature | ❌ 解不开（二进制哈希） | 签名 |

### 关键结论

```
JWT 是【签名】不是【加密】：
    内容公开（谁都能解开看）
    内容改不了（改一个字签名就完全变）
```

### 签名怎么算

```
signature = HMAC-SHA384( header + "." + payload , secret )
                                                    ↑ 只有服务端知道
```

**验证时**：服务端用 secret 重算一遍，和 token 第三段比对，不一致就拒绝。

### JwtUtil 逐行要点

```java
@Value("${atm.jwt.secret}")        // 从 application.properties 注入
private String secret;

private SecretKey getKey(){
    return Keys.hmacShaKeyFor(secret.getBytes(UTF_8));
    // 根据字节长度自动选算法：54 字节 → HS384（≥48）
}

Jwts.builder()
    .subject(...)          // sub
    .claim("username", ...) // 自定义
    .issuedAt(now)         // iat
    .expiration(exp)       // exp
    .signWith(getKey())    // 签名
    .compact();            // ★ 只有这一步才真正生成字符串

Jwts.parser()
    .verifyWith(getKey())      // 验签
    .build()
    .parseSignedClaims(token)  // 拆三段 + 验签名 + 查过期（失败抛异常）
    .getPayload();             // 取出 payload
```

### `@Value` 是什么（顺便讲的专业用法）

```java
@Value("${atm.jwt.secret}")
private String secret;
```

从配置文件里读 key 的值注入给字段，启动时自动赋值，不用自己 new。冒号后可写默认值：`${atm.jwt.expire:3600000}`。

---

## 检验 2：JWT 三问

### Q1 —— payload 能被人看到什么

**你的答案**：能看到用户 id、用户名、用户角色、签发时间、过期时间

**评分**：✅ **完全正确（5 个字段全对）**

### Q2 —— 篡改 payload 会怎样

**你的答案**：token 校验会不通过，因为 token 被篡改了，与之前的 token 信息不一致，会抛异常

**评分**：✅ 正确，一处措辞可精确

**精确说法**：服务端不是在"比对之前的 token"，而是**用 secret 重新算一遍签名**，和 token 第三段比对。

### Q3 —— secret 泄露的后果 ⚠️

**你的答案**：别人能登录你的账号，拿到你的个人信息

**评分**：⚠️ 方向沾边，但**严重低估**

**正确答案**：secret 泄露后，攻击者可以**伪造任意身份的 token**，不需要密码。

```java
String h = base64url("{\"alg\":\"HS384\"}");
String p = base64url("{\"sub\":\"1\",\"username\":\"admin\",\"role\":\"admin\",\"exp\":...}");
String s = hmacSha384(h + "." + p, 泄露的secret);   // 用偷来的密钥签名
String token = h + "." + p + "." + s;                // 签名合法，服务端验签通过
```

**后果清单：**

```
① 不需要知道任何人的密码，直接伪造任意用户的 token
② 可以伪造管理员身份（role 是自己写的）
③ 所有已签发的 token 全部作废（唯一补救是换 secret）
④ 日志里看起来像正常操作，无法察觉、无法追溯
```

**连回现实**：本项目的 `atm.jwt.secret` 硬编码在 `application.properties` 里，且仓库是公开的 —— **任何看过 GitHub 的人都能伪造管理端 token**。

**影响有限**（管理端只有客户查询，没有资金操作），但部署前必须改成环境变量注入。

---

# 第三部分：拦截器

## 讲解要点

### 拦截器在请求生命周期的位置

```
请求 → ① Filter（Servlet，Tomcat 层）
      → ② DispatcherServlet
      → ③ Interceptor.preHandle()        ★ LoginInterceptor
      → ④ Controller
      → ⑤ Interceptor.postHandle()       项目没用
      → ⑥ 序列化成 JSON
      → ⑦ Interceptor.afterCompletion()   ★ 清理 ThreadLocal
      → 响应
```

### WebMvcConfig 逐行

```java
@Configuration                     // 配置类
@RequiredArgsConstructor           // Lombok：给 final 字段生成构造函数（构造器注入）
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")        // ① 拦哪些
                .excludePathPatterns(               // ② 从里面排除哪些（优先）
                        "/api/admin/login",
                        "/api/atm/**",
                        "/api/hello",
                        "/error"
                );
    }
}
```

**判断规则：先看 addPathPatterns，再看 excludePathPatterns，两者都匹配时【排除优先】。**

**四个白名单的原因：**

| 路径 | 原因 |
|---|---|
| `/api/admin/login` | 登录接口不能要求登录，否则死循环 |
| `/api/atm/**` | ATM 端用 sessionToken，不用 JWT |
| `/api/hello` | 健康检查（部署探活） |
| `/error` | Spring 出错时转发到这里，拦了会丢错误信息 |

### 三个关键实现点

**① 两个钩子**

```java
public boolean preHandle(...)      // Controller 之前；true 放行，false 拦截
public void afterCompletion(...)   // 请求完全结束后；等价于 finally
```

**② 为什么拦截器里要手动写 JSON**

拦截器在 Controller **之前**执行，此刻还没进 Spring 的处理链，`@RestControllerAdvice` 接不到（它只处理 Controller 抛出的异常）。所以只能自己往 `response.getWriter()` 写 JSON。

**③ 为什么 `afterCompletion` 必须 `UserContext.clear()`

Tomcat 用线程池，线程会复用。不清理的话：

```
请求A（admin）→ T1 里存 admin → A 结束
请求B 分配到 T1 → 读到 admin  💥 串号
```

**为什么不能写在 preHandle**：那时还没 set。
**为什么不能写在 Controller**：Controller 抛异常时会被跳过（像 try 里没 finally）。
**afterCompletion 等价于 finally**，成功失败都执行。

### 拦截器 vs 过滤器 vs AOP

| | Filter | Interceptor | AOP |
|---|---|---|---|
| 属于 | Servlet（Tomcat） | Spring MVC | Spring 容器 |
| 能注入 Spring Bean | ❌ | ✅ | ✅ |
| 典型用途 | 编码、跨域 | 登录、权限校验 | 事务、日志 |

**项目里三个都用了：**
```
Filter      → CharacterEncodingFilter（Spring Boot 自动配）
Interceptor → LoginInterceptor（JWT 校验）
AOP         → @Transactional（事务，隐式的）
```

---

## 检验 3：拦截器三问

### Q1 —— `/api/atm/withdraw` 会被拦吗 ❌

**你的答案**：会，拦截器里写的是 `/api/**`，可以跨层级

**评分**：❌ **错误**

**正确答案：不会拦。**

```
第 1 步：/api/atm/withdraw 匹配 /api/** 吗？→ 匹配（本该拦）
第 2 步：/api/atm/withdraw 匹配 /api/atm/** 吗？→ 匹配（白名单）
第 3 步：两者都匹配 → 排除优先 → 不拦
```

**如果真被拦了，后果**：ATM 请求头里没有 `Authorization: Bearer`，拦截器 L37 直接返回 401「未登录」→ **所有 ATM 功能全部不可用**。所以那行 `/api/atm/**` 是必须的。

**值得注意的退步**：这个你第一轮 Q2 答对过（"ATM 的请求不经过这个拦截器"）。差别在于：

```
第一轮：问"为什么不校验 JWT" → 靠记忆中的结论答对了
今天：  问"这个接口会被拦吗" → 需要当场读代码判断就错了
```

### Q2 —— 为什么 clear 写在 afterCompletion ✅

**你的答案**：写在 preHandle 里面还没 set，所以没用；写在 afterCompletion 里面，无论成功还是失败都会执行

**评分**：✅ **完全正确**

### Q3 —— 拦截失败时 JSON 谁生成的 ✅

**你的答案**：拦截器自己手动生成的

**评分**：✅ 正确

---

# 统计

| 检验 | 题数 | 结果 |
|---|---|---|
| Stream 读代码 | 2 | ✅✅ |
| JWT 三问 | 3 | ✅✅⚠️ |
| 拦截器三问 | 3 | ❌✅✅ |

```
✅ 6 个        ⚠️ 1 个        ❌ 1 个        答对率 75%
```

---

# 这一轮的两个错题有共同点

```
JWT Q3   → 答"能登录你的账号"，低估了"能伪造任意身份"
拦截器 Q1 → 只看了 addPathPatterns，没看 excludePathPatterns
```

**都不是"完全不会"，而是【看到一半就下结论】。**

**这和我（AI）前面几次误报（`Get-ChildItem` 漏 `-Recurse`、没展开 `$USER_HOME$`、没做 URL 解码）是同一个毛病。**

---

# 读代码的硬功夫（这一轮的结论）

```
① 一个方法 / 一个配置文件里【所有相关的行】都要看完，再看结论
② 遇到 Stream / Lambda：先找终点，再找起点，最后看中间
③ 遇到 JWT：记住"前两段能解开，第三段是签名"
④ 遇到拦截器：把 add 和 exclude 两组路径【都】列出来再判断
⑤ 遇到读不懂的代码：问"删掉它会怎样" —— 每一行都对应一个"不加就出事"的场景
```

---

# 待巩固

```
★★  白名单优先于拦截规则（excludePathPatterns > addPathPatterns）
★★  secret 泄露 = 能伪造任意身份（不是"能登录你的账号"）
★   Stream 的读法：先终点、再起点、后中间
★   拦截器自己写 JSON 的原因（还没进 Spring 处理链）
```
