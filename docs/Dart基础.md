# Dart 基础 8 个概念（配本项目真实代码）

> 目的：让你能看懂 `main.dart` 和 `api.dart`，不再一片陌生词。
> 所有例子都取自本项目的真实代码，标注了文件位置。

---

## ① var / final / const —— 三种声明方式

```dart
var  a = 5;      // 类型自动推断为 int，之后【可以】重新赋值
final b = 5;     // 只能赋值【一次】，之后不能改（运行期确定）
const c = 5;     // 编译期常量，最严格（编译时就必须是固定值）
```

### 项目里的例子

```dart
// main.dart
const String baseUrl = 'http://localhost:8080';   // 编译期常量
final cardCtrl = TextEditingController();          // 只创建一次，之后不能换
Step step = Step.insertCard;                        // 普通变量，会不断变
```

### 怎么选

| 用哪个 | 什么时候 |
|---|---|
| `const` | 值永远不变，且编译时就知道（字符串、数字、固定配置） |
| `final` | 只赋值一次，但值运行时才确定（`DateTime.now()`、控制器对象） |
| 不用修饰符 | 值会变（`step`、`loading`、`balance`） |

### final 和 const 的区别（一个例子看懂）

```dart
final now = DateTime.now();    // ✅ 合法：运行时才知道当前时间
const now2 = DateTime.now();   // ❌ 编译错误：DateTime.now() 不是编译期常量
```

---

## ② 基本类型

```dart
int     整数        500
double  小数        9800.00
String  字符串      '张三'
bool    布尔        true / false
List    列表        [100, 500, 1000]
Map     键值对      {'cardNo': '6222...', 'amount': 500}
dynamic 任意类型    （放弃类型检查，慎用）
```

### 项目里的例子

```dart
// main.dart —— 各种字段
String sessionToken = '';
String customerName = '';
bool loading = false;
int count = 0;
List<dynamic> detailList = [];          // 交易明细列表

// api.dart —— Map 就是 JSON 对象
final map = jsonDecode(...) as Map<String, dynamic>;
map['code']      // 取 key 为 code 的值
map['data']      // 取 key 为 data 的值
```

### ⚠️ 一个坑：int 和 double 是两种类型

```dart
int a = 500;
double b = 500.0;
// a == b 是 true，但类型不同
```

**后端返回的金额，可能是 int 也可能是 double**，所以项目里写了：

```dart
// api.dart
double toDouble(dynamic v) => (v as num).toDouble();
//                                   ↑ num 是 int 和 double 的共同父类
```

**用法：`toDouble(d['amount'])` —— 不管后端给 500 还是 500.00，都能安全转换。**

---

## ③ 空安全（⭐ 最容易懵、也最重要）

### 核心规则

```dart
String  name = '张三';   // 加问号 = 不能为 null
String? name = null;     // 加 ? = 允许为 null
```

**Dart 在编译时就会检查：一个"不能为 null"的变量，你绝不能给它赋 null。**
这就是"空安全"——**把空指针异常从运行期提前到编译期**。

### 项目里的例子

```dart
// main.dart —— _call 的返回类型是 ApiResult?（可能返回 null）
Future<ApiResult?> _call(String path, Map<String, dynamic> body) async {
    ...
    if (!mounted) return null;      // ← 所以这里能返回 null
    return r;
}

// 调用方必须处理 null
final r = await _call(...);
if (r == null) return;              // ← 必须先判空，否则编译器报错
if (!r.ok) { ... }
```

### 三种处理 null 的写法

```dart
// ① ?? —— 如果左边是 null，用右边的默认值
String token = (r.data['sessionToken'] ?? '').toString();
//                                   ↑ 如果后端没返回这个字段，用空字符串

// ② ?. —— 如果左边是 null，整个表达式返回 null（不会崩）
int? len = name?.length;            // name 为 null 时 len 也是 null

// ③ ! —— 我保证它不为 null（危险！猜错就崩）
String s = name!;                   // 如果 name 真是 null，运行时报错
```

### 一句话记忆

```
String   —— 保证有值
String?  —— 可能是 null，用之前必须处理
??       —— 给个兜底值
?.       —— 安全地访问
!        —— 我拿命保证（尽量别用）
```

---

## ④ 函数

### 三种写法

```dart
// ① 普通函数
int add(int a, int b) {
  return a + b;
}

// ② 箭头函数（只有一行时用）
int add(int a, int b) => a + b;

// ③ 匿名函数（没有名字，当场用）
() { ... }        // 完整写法
() => ...         // 箭头写法
```

### 项目里的例子

```dart
// main.dart —— 箭头函数
void _goResult(String text) {
  setState(() {
    resultText = text;
    step = Step.result;
  });
}

// main.dart —— 箭头写法
String _title() => switch (step) {
      Step.insertCard => 'ATM自助服务',
      ...
    };

// main.dart —— 把函数当作参数传
_bigButton('插 卡', insertCard)     // 传的是 insertCard 这个函数本身
_bigButton('退 卡', () => setState(() => step = Step.menu))   // 传匿名函数
```

### 关键认知：**Dart 里函数是一等公民**

```dart
// _bigButton 的签名：第二个参数是一个"无参无返回值的函数"
Widget _bigButton(String text, VoidCallback onTap) {
//                                       ↑ VoidCallback = void Function()
  return ElevatedButton(
    onPressed: onTap,      // ← 把函数交给按钮，点的时候由 Flutter 调用
    ...
  );
}
```

**`_bigButton('插 卡', insertCard)` 这一行没有执行 `insertCard`**，只是把它的名字（引用）传进去。**真正的执行发生在你手指按下的那一刻。**

---

## ⑤ 类

### 基本结构

```dart
class Person {
  // 字段（属性）
  String name;
  int age;

  // 构造函数
  Person(this.name, this.age);       // ← 这个写法是 Dart 特有的简写：
                                      //    等价于 { this.name = name; this.age = age; }

  // 方法
  String greet() => '我是 $name，今年 $age 岁';
}
```

### 项目里的例子

```dart
// api.dart —— 一个数据类
class ApiResult {
  final int code;
  final String message;
  final dynamic data;

  ApiResult(this.code, this.message, this.data);   // ← 简写构造

  bool get ok => code == 200;                      // ← getter 方法
}

// 使用
return ApiResult(200, '操作成功', map['data']);
```

```dart
// main.dart —— 页面类（Flutter 的固定套路）
class AtmHome extends StatefulWidget {           // ① 继承 StatefulWidget
  const AtmHome({super.key});

  @override
  State<AtmHome> createState() => _AtmHomeState();   // ② 指定自己的 State 类
}

class _AtmHomeState extends State<AtmHome> {     // ③ 真正的逻辑和界面都在这
  Step step = Step.insertCard;                   // 字段

  @override
  void dispose() { ... }                         // 方法

  @override
  Widget build(BuildContext context) { ... }     // 方法
}
```

### 三个关键字

| 关键字 | 含义 |
|---|---|
| `extends` | 继承 |
| `@override` | 我覆盖了父类的方法（加不加都能跑，但加了编译器会帮你检查拼写） |
| `super.key` | 调用父类构造函数，把 key 传上去（Flutter 的固定写法） |

> 💡 **`_AtmHomeState` 前面的下划线 `_` = 私有**，只有本文件能访问。

---

## ⑥ 命名参数（⭐ Flutter 里到处都是）

### 两种参数

```dart
// 位置参数：靠顺序区分
Person('张三', 20)          // 第一个是 name，第二个是 age

// 命名参数：靠名字区分（用大括号包起来）
Person(name: '张三', age: 20)   // 顺序随便，写名字就行
```

### 项目里的例子

```dart
// main.dart —— 这是【位置参数】
Widget _bigButton(String text, VoidCallback onTap) { ... }
// 调用：_bigButton('插 卡', insertCard)      ← 靠顺序

// Flutter 的组件几乎全是【命名参数】（大括号）
return Scaffold(
  appBar: AppBar(title: Text('ATM自助服务')),   // appBar: 是命名的
  body: Padding(
    padding: const EdgeInsets.all(24),          // padding: 是命名的
    child: Center(...),                         // child: 是命名的
  ),
);
```

### required —— 必填的命名参数

```dart
class AtmHome extends StatefulWidget {
  const AtmHome({super.key});
  //             ↑ 大括号 = 命名参数
  //               super.key = 把 key 传给父类
}
```

```dart
// 如果有自己的必填参数
class MyCard extends StatelessWidget {
  final String title;

  const MyCard({super.key, required this.title});
  //                       ↑ 调用时必须写 title:
}

// 调用
MyCard(title: '取款')      // ✅
MyCard()                   // ❌ 编译错误：title 是 required
```

### 一句话记忆

```
小括号 ()      → 位置参数，靠顺序
大括号 {}      → 命名参数，靠名字
required      → 命名参数里必须传的
this.xxx      → 参数值直接赋给同名字段
```

---

## ⑦ 异步：Future / async / await（⭐ 前端最核心）

### 一句话理解

```
Future<T>   = "将来会给你一个 T"
async       = "这个函数里有异步操作"
await       = "就在这等着，等它回来再往下"
```

### 项目里的例子（完整流程）

```dart
// api.dart —— 返回 Future<ApiResult>，意思是"将来给你一个 ApiResult"
Future<ApiResult> apiPost(String path, Map<String, dynamic> body) async {
//                                                  ↑ async 是必须的
  try {
    final res = await http.post(...).timeout(...);
    //          ↑ await：挂起这里，等 HTTP 响应回来

    final map = jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
    return ApiResult(...);
  } catch (e) {
    return ApiResult(-1, '网络异常：$e', null);
  }
}
```

```dart
// main.dart —— 调用方也必须 await
Future<void> withdraw() async {          // ← 有 await，所以方法也要 async
  final r = await _call('/api/atm/withdraw', {...});
  //          ↑ 等请求完成，拿到结果后才继续
  if (r == null) return;
  ...
}
```

### 为什么方法要写 `Future<void>`

```
Future<void>   → 异步方法，结束时没有返回值（void）
Future<ApiResult> → 异步方法，结束时返回一个 ApiResult
void           → 同步方法，立刻执行完
```

**只要方法体里有 `await`，返回类型就必须是 `Future<...>`，而且必须加 `async`。**

### 不用 await 会怎样

```dart
// ❌ 忘了 await
void wrong() {
  _call('/api/atm/withdraw', {...});   // 请求发出去了，但没人等结果
  // 下面立刻执行，此时 r 还不存在
}

// ✅ 正确
Future<void> right() async {
  final r = await _call(...);          // 等到结果再继续
}
```

**叫"发射后不管"（fire and forget）** —— 有时是故意的（比如后台刷新余额），但大多数时候是 bug。

### 项目里故意不用 await 的地方

```dart
// main.dart —— 进菜单时静默刷新余额
setState(() => step = Step.menu);
_refreshMenuBalance();     // ← 故意不 await：先切页面，余额查到再更新
```

**这样用户不用等余额查完才看到菜单。**

---

## ⑧ 集合推导（列表里的 for 和 if）

### 项目里的例子

```dart
// main.dart —— 快捷金额按钮
Widget _quickAmounts(List<int> values) {
  return Row(
    children: [
      for (final v in values)          // ← for 直接写在列表里
        Expanded(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: OutlinedButton(
              onPressed: () => amountCtrl.text = '$v',
              child: Text('$v'),
            ),
          ),
        ),
    ],
  );
}
```

**等价于 Java：**

```java
List<Widget> children = new ArrayList<>();
for (int v : values) {
    children.add(new Expanded(...));
}
```

### 还有 if 也能写在列表里

```dart
// main.dart —— 结果页的按钮：登录中显示"返回菜单"，已退卡显示"重新插卡"
children: [
  if (loggedIn)
    _bigButton('返回菜单', () => setState(() => step = Step.menu))
  else
    _bigButton('重新插卡', () { ... }),
]
```

**等价于 Java：**

```java
if (loggedIn) children.add(返回菜单按钮); else children.add(重新插卡按钮);
```

### 常用的列表操作

```dart
List<int> a = [1, 2, 3, 4, 5];

a.where((x) => x > 2).toList();      // 过滤 → [3, 4, 5]
a.map((x) => x * 2).toList();        // 转换 → [2, 4, 6, 8, 10]
a.contains(3);                        // 包含 → true
a.isEmpty;                            // 是否为空
```

### 项目里的用法

```dart
// main.dart —— 判断交易类型是不是"支出"
final isOut = ['WITHDRAW', 'TRANSFER_OUT', 'FEE'].contains(t['transTypeCode']);
//                                                          ↑ 列表的 contains
```

---

## 🎁 附：两个你项目里用到、但不在这 8 个里的语法

### ① switch 表达式（Dart 3 新语法）

```dart
// main.dart —— 根据 step 返回标题
String _title() => switch (step) {
      Step.insertCard => 'ATM自助服务',
      Step.password => '请输入交易密码',
      Step.menu => '请选择业务',
      Step.withdraw => '取款',
      Step.deposit => '存款',
      Step.transfer => '转账',
      Step.detail => '交易明细',
      Step.balance => '余额查询',
      Step.result => '操作结果',
    };
```

**特点：**
- `=>` 后面直接是结果，不用 `return`
- **编译器会检查你有没有漏掉某个枚举值**（漏了直接编译报错）
- 比 `if-else if-else` 链清晰得多

### ② 字符串插值

```dart
String name = '张三';
int amount = 500;

'欢迎，$name'                      // 简单变量直接用 $变量名
'金额：${amount * 2} 元'           // 表达式要用 ${...}
'余额：${toDouble(x).toStringAsFixed(2)} 元'
```

**注意：**
- `$name` 只对"单个变量名"有效
- 只要里面有运算、方法调用、或属性访问，就必须用 `${...}`
- **单引号双引号都行**，项目里统一用单引号（Flutter 惯例）

---

## ✅ 自查：看你能不能回答这 5 个

```
1. final 和 const 有什么区别？举一个只能用 final 的例子。
2. String 和 String? 有什么区别？r.data['x'] ?? '' 是什么意思？
3. Future<ApiResult?> 里的 ? 和 Future 分别代表什么？
4. Scaffold(body: ..., appBar: ...) 用的是位置参数还是命名参数？
5. [for (final v in values) ...] 用 Java 怎么写？
```

**能答出 4 个，看 `main.dart` 就不会懵了。**

---

## 📌 建议的阅读方式

**不要从头读到尾。** 推荐：

```
第一遍（15 分钟）  看 ③空安全 和 ⑦异步 —— 这两个占了你项目代码的 60% 陌生感
第二遍（10 分钟）  打开 main.dart，遇到不认识的语法回来查
第三遍（随时）     卡住了再翻
```

**最有效的方法：拿 `main.dart` 对着这份文档读一遍，每读一行问自己"这行在干什么"，能答上就过。**
