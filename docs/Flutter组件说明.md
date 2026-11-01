# Flutter 组件速查（本项目用到的全部）

> 目标：看到 `main.dart` 里任何一个 Widget，都知道它是什么、干什么、有哪些常用参数。
> 所有例子都是本项目 `main.dart` 里的真实代码。

---

# 0. 先建立三个基础认知

## ① 一切皆 Widget

Flutter 里没有 HTML 那种"标签 + CSS"，**界面上的每一样东西都是一个 Widget**：

```
文字        → Text
图片        → Image
按钮        → ElevatedButton / OutlinedButton / TextButton
一排东西    → Row
一列东西    → Column
间距        → SizedBox
内边距      → Padding
```

**连"间距"和"内边距"都是 Widget** —— 这是 Flutter 和其他前端框架最大的不同。

## ② Widget 组成一棵树

```dart
Scaffold(                        ← 页面外壳
  appBar: AppBar(...),           ← 顶部栏
  body: Padding(                 ← 内容区加内边距
    child: Center(               ← 内容居中
      child: Column(             ← 里面竖着排
        children: [
          Text('请插入银行卡'),    ← 第一行文字
          SizedBox(height: 24),   ← 间距
          TextField(...),         ← 输入框
        ],
      ),
    ),
  ),
)
```

**读的顺序：从外往里，一层层剥。**

## ③ 两种参数：`child` 和 `children` ⭐（这是最容易混的）

| 参数 | 意思 | 谁用它 |
|---|---|---|
| `child:` | **只能放【一个】** Widget | Padding、Center、Expanded、Container、SizedBox(偶尔) |
| `children:` | **放【一个列表】**（多个 Widget） | Column、Row、ListView、Scaffold(body 里的 Stack) |

```dart
// ✅ child：只能一个
Padding(
  child: Text('只有一个'),        ← 对
)

// ❌ child 放多个 → 编译错误
Padding(
  child: [Text('a'), Text('b')],  ← 错！child 只收一个
)

// ✅ children：放列表
Column(
  children: [
    Text('第一行'),
    Text('第二行'),
  ],
)
```

**记忆法：**

```
child    → 单数 → 一个
children → 复数 → 一堆（一个 List）
```

---

# 一、布局类：决定"东西放哪儿"

## Column —— 竖着排 ⭐（项目里 10 次）

```dart
Column(
  mainAxisSize: MainAxisSize.min,
  crossAxisAlignment: CrossAxisAlignment.stretch,
  children: [
    Text('第一行'),
    Text('第二行'),
  ],
)
```

**两个最重要的参数：**

| 参数 | 取值 | 效果 |
|---|---|---|
| `mainAxisSize` | `MainAxisSize.min` | 高度**只包住内容**（不占满屏幕） |
|  | `MainAxisSize.max`（默认） | 高度占满父容器 |
| `crossAxisAlignment` | `CrossAxisAlignment.stretch` | **子元素横向拉满宽度**（按钮会变全宽） |
|  | `start`（默认） | 子元素靠左，宽度按自身内容 |
|  | `center` | 子元素横向居中 |
| `mainAxisAlignment` | `center` / `start` / `end` / `spaceBetween` | 子元素**纵向**怎么分布 |

**你项目里的典型用法：**

```dart
Column(
  mainAxisSize: MainAxisSize.min,                    // 高度包住内容
  crossAxisAlignment: CrossAxisAlignment.stretch,    // 按钮拉满宽度
  children: [
    const Icon(Icons.credit_card, size: 72),
    const SizedBox(height: 24),
    const Text('请插入银行卡', textAlign: TextAlign.center),
    ...
    _bigButton('插 卡', insertCard),                  // ← 因为 stretch，自动全宽
  ],
)
```

**⚠️ 方向别搞混：**

```
Column 的主轴是【竖直方向】
    → mainAxisAlignment  管纵向（上下）
    → crossAxisAlignment 管横向（左右）

Row 正好相反：
    → mainAxisAlignment  管横向（左右）
    → crossAxisAlignment 管纵向（上下）
```

## Row —— 横着排（项目里 1 次，在 `_quickAmounts`）

```dart
Row(
  children: [
    for (final v in values)
      Expanded(                              ← 每个按钮平分宽度
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4),
          child: OutlinedButton(child: Text('$v')),
        ),
      ),
  ],
)
```

**效果：一排按钮，每个宽度一样。**

**Row 和 Column 唯一的区别就是方向，参数完全一样（只是纵横互换）。**

## Center —— 居中（项目里 2 次）

```dart
Center(
  child: _body(),
)
```

**把子 Widget 在父容器里水平和垂直都居中。**

**如果子 Widget 自己有大小，Center 就把它摆在正中间。**

## Padding —— 内边距（项目里 3 次）

```dart
Padding(
  padding: const EdgeInsets.all(24),      // 四周各 24 像素内边距
  child: Center(...),
)
```

**`EdgeInsets` 的四种写法：**

```dart
EdgeInsets.all(24)                                  // 四周都是 24
EdgeInsets.symmetric(horizontal: 4, vertical: 12)   // 左右 4，上下 12
EdgeInsets.only(left: 8, top: 16)                   // 只指定某几边
EdgeInsets.fromLTRB(8, 16, 8, 16)                   // 左上右下
```

## SizedBox —— 占位 / 间距 ⭐（项目里 34 次，最多）

```dart
const SizedBox(height: 24)     // 竖直方向空出 24 像素
const SizedBox(width: 8)       // 水平方向空出 8 像素
```

**它的本质是"一个指定大小的空盒子"。最常用的场景就是当间距用。**

**为什么不用 `Padding` 做间距？**

```
Padding  → 给"别人"加内边距（改变别人占的空间）
SizedBox → 自己就是一个"占位块"（不改变别人，只插一块空白）

在 Column 的 children 里插间距，SizedBox 更直观：
    children: [
      Text('第一行'),
      SizedBox(height: 24),      ← 插一块 24 像素的空白
      Text('第二行'),
    ]
```

## Expanded —— 平分剩余空间（项目里 2 次）

```dart
Row(
  children: [
    Expanded(child: OutlinedButton(child: Text('100'))),
    Expanded(child: OutlinedButton(child: Text('500'))),
  ],
)
```

**作用：把父容器（Row）剩下的宽度**平均分**给每个 Expanded。**

```
Row 总宽 400
    第 1 个 Expanded → 200
    第 2 个 Expanded → 200
```

**⚠️ 一个必须知道的限制：**

**`Expanded` 的祖先必须有确定的高度/宽度**（比如在 Row 里分宽度、在 Column 里分高度）。

**这就是明细页为什么要从 `Center` 里拿出来的原因：**

```dart
// ❌ 报错：RenderBox was not laid out / unbounded height
Center(child: Column(children: [Expanded(child: ListView(...))]))

// ✅ 正确：让 Column 直接占满 Padding 给的空间（有确定高度）
Padding(child: Column(children: [Expanded(child: ListView(...))]))
```

---

# 二、页面骨架

## MaterialApp —— 整个 App 的根（项目里 1 次）

```dart
MaterialApp(
  title: 'ATM自助服务',
  debugShowCheckedModeBanner: false,          // 去掉右上角 DEBUG 标签
  theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.blue),
  home: const AtmHome(),                      // 启动时显示哪个页面
)
```

**四个参数：**

| 参数 | 作用 |
|---|---|
| `title` | App 名称（任务栏显示） |
| `debugShowCheckedModeBanner` | 是否显示右上角红色 DEBUG 角标 |
| `theme` | 全局主题（颜色、字体） |
| `home` | 首屏显示哪个 Widget |

## Scaffold —— 页面外壳（项目里 1 次）

```dart
Scaffold(
  appBar: AppBar(title: Text(_title()), centerTitle: true),
  body: Padding(...),                          // 内容区
)
```

**Scaffold 提供了一整套"页面要素"：**

```
appBar    → 顶部栏
body      → 内容区         ← 项目里只用了这两个
floatingActionButton → 右下角悬浮按钮（项目未用）
bottomNavigationBar  → 底部导航（项目未用）
drawer    → 侧边抽屉（项目未用）
```

## AppBar —— 顶部栏（项目里 1 次）

```dart
AppBar(
  title: Text(_title()),      // 标题（动态的，根据 step 变）
  centerTitle: true,          // 标题居中
)
```

---

# 三、内容展示

## Text —— 显示文字 ⭐（项目里 32 次，第二多）

```dart
const Text('请插入银行卡')
Text('可用余额 $menuBalance 元', textAlign: TextAlign.center)
```

**常用参数：**

| 参数 | 作用 |
|---|---|
| `textAlign` | 文字对齐（`left` / `center` / `right`） |
| `style` | 样式（字体大小、颜色、粗细） |
| `maxLines` | 最多几行 |
| `overflow` | 超出怎么办（`ellipsis` 显示省略号） |

**⚠️ `const Text(...)` 什么时候能用：**

```dart
const Text('请插入银行卡')            // ✅ 文字固定，可以 const
const Text('余额：$balance')          // ❌ 编译错误！balance 是变量
```

## Icon —— 图标（项目里 13 次）

```dart
const Icon(Icons.credit_card, size: 72, color: Colors.blue)
```

| 参数 | 作用 |
|---|---|
| 第一个参数（位置参数） | 用哪个图标（`Icons.xxx`） |
| `size` | 大小（像素） |
| `color` | 颜色 |

**项目里用到的 10 个图标：**

| 图标 | 用在哪 |
|---|---|
| `Icons.credit_card` | 插卡页、输入框前缀、转账对方卡号 |
| `Icons.lock_outline` | 密码页 |
| `Icons.password` | 密码输入框前缀 |
| `Icons.payments_outlined` | 取款页 |
| `Icons.savings_outlined` | 存款页 |
| `Icons.swap_horiz` | 转账页 |
| `Icons.account_balance_wallet` | 余额页 |
| `Icons.currency_yuan` | 金额输入框前缀 |
| `Icons.arrow_upward` | 明细：支出（红色向上） |
| `Icons.arrow_downward` | 明细：收入（绿色向下） |

> 💡 **图标名去哪查**：https://fonts.google.com/icons 搜关键词，或 IDEA 里敲 `Icons.` 看补全

## TextStyle —— 文字样式（项目里 18 次）

```dart
const TextStyle(fontSize: 20, color: Colors.grey, fontWeight: FontWeight.bold)
```

| 参数 | 作用 | 常用值 |
|---|---|---|
| `fontSize` | 字体大小 | 12 / 13 / 14 / 16 / 18 / 20 / 22 |
| `color` | 颜色 | `Colors.blue` / `Colors.grey` / `Colors.red` |
| `fontWeight` | 粗细 | `FontWeight.bold` / `normal` |
| `height` | 行高（倍数） | `1.6`（明细页多行文字用的） |

## Divider —— 分割线（项目里 1 次）

```dart
separatorBuilder: (context, index) => const Divider(height: 1),
```

**明细列表每两项之间的横线。**

---

# 四、输入类

## TextField —— 输入框（项目里 6 次）

```dart
TextField(
  controller: cardCtrl,                          // 谁管这个框里的文字
  keyboardType: TextInputType.number,            // 弹数字键盘
  decoration: const InputDecoration(             // 外观配置
    labelText: '银行卡号',
    hintText: '6222020000000001',
    border: OutlineInputBorder(),
    prefixIcon: Icon(Icons.credit_card),
  ),
  onSubmitted: (_) => insertCard(),              // 按回车时干什么
)
```

**参数逐个说：**

| 参数 | 作用 |
|---|---|
| `controller` | **必须配**。通过它读/写输入框内容 |
| `keyboardType` | 键盘类型（`number` 弹数字键盘） |
| `obscureText` | `true` = 密码遮挡（显示成圆点） |
| `maxLength` | 最多几个字符（密码框用了 6） |
| `decoration` | 外观（标签、提示、边框、图标） |
| `onSubmitted` | 按回车（或手机上的"完成"）时执行 |
| `onChanged` | 每输入一个字符就执行 |

## TextEditingController —— 输入框的"遥控器"（项目里 4 个）

```dart
final cardCtrl = TextEditingController();
```

**三个用法：**

```dart
cardCtrl.text                     // 读出当前内容
cardCtrl.text = '622202...'       // 写入内容（快捷按钮就靠这个）
cardCtrl.clear()                  // 清空
```

**⚠️ 两个要点：**

```
① 必须在 dispose() 里调用 cardCtrl.dispose()，否则内存泄漏
② 赋值时会自动通知界面刷新 → 所以改它【不需要】setState
```

## InputDecoration —— 输入框的外观（项目里 6 次）

```dart
const InputDecoration(
  labelText: '银行卡号',            // 标签（显示在框内或上方）
  hintText: '6222020000000001',     // 占位提示（框空时显示的灰字）
  border: OutlineInputBorder(),     // 边框样式（矩形框）
  prefixIcon: Icon(Icons.credit_card),  // 框内左侧的图标
)
```

| 参数 | 效果 |
|---|---|
| `labelText` | 标签文字 |
| `hintText` | 空框时的灰色提示 |
| `border: OutlineInputBorder()` | 画一个矩形边框（不写就是下划线样式） |
| `prefixIcon` | 左侧图标 |
| `suffixIcon` | 右侧图标 |

---

# 五、按钮类（四种对比 ⭐）

| Widget | 长什么样 | 用在哪 |
|---|---|---|
| **`ElevatedButton`** | 浅色底 + 阴影（"浮起来"） | `_bigButton`（主要按钮） |
| **`OutlinedButton`** | 只有描边，无填充 | 快捷金额按钮（次要） |
| **`FilledButton`** | 蓝色实心（Material 3 的主按钮） | 弹窗里的"确定退卡" |
| **`TextButton`** | 纯文字，无边框 | "退卡"（密码页）、"返回" |

## 共同点：都需要 `onPressed`

```dart
ElevatedButton(
  onPressed: () { ... },        // 点下去执行什么；传 null 就是禁用状态
  child: Text('插 卡'),          // 按钮上显示什么（只能一个）
)
```

**⚠️ `onPressed: null` 会让按钮变灰、点不动** —— 有时可以故意这么用来做"禁用".

## 项目里的 `_bigButton`（统一封装的按钮）

```dart
Widget _bigButton(String text, VoidCallback onTap) {
  return ElevatedButton(
    onPressed: onTap,
    child: Padding(
      padding: const EdgeInsets.symmetric(vertical: 14),   // 上下加厚，按钮变大
      child: Text(text, style: const TextStyle(fontSize: 18)),
    ),
  );
}
```

**为什么套一层 `Padding`：**

```
ElevatedButton 默认比较矮
在 child 外面加 vertical: 14 的内边距 → 按钮变高变醒目
比设置固定 height 更灵活（文字变大时按钮也跟着变）
```

**`VoidCallback` 是什么：**

```dart
VoidCallback  ==  void Function()      // 「无参数、无返回值」的函数类型
```

所以 `_bigButton('插 卡', insertCard)` 传的是 `insertCard` 这个函数本身。

---

# 六、列表类

## ListView.separated —— 可滚动列表（项目里 1 次，明细页）

```dart
ListView.separated(
  itemCount: detailList.length,                            // 总共几项
  separatorBuilder: (context, index) => const Divider(height: 1),   // 每项之间的分隔
  itemBuilder: (context, i) {                              // 第 i 项长什么样
    final t = detailList[i];
    return ListTile(...);
  },
)
```

**三个参数的关系：**

```
itemCount       → 一共 5 条
itemBuilder     → 第 0 条、第 1 条 … 第 4 条 分别长什么样
separatorBuilder → 第 0 和 1 之间、1 和 2 之间… 放什么（这里放分割线）
```

**⚠️ 为什么必须给高度**（明细页从 `Center` 里拿出来的原因）：

```
ListView 是"可滚动的" → 它需要知道"我有多高"才能决定滚动范围
如果外面是 Center（高度不确定）→ 它不知道多高 → 报错：
    Vertical viewport was given unbounded height
```

## ListTile —— 列表里的一行（项目里 1 次）

```dart
ListTile(
  leading: Icon(Icons.arrow_upward, color: Colors.red),    // 左侧（图标）
  title: Text('取款'),                                     // 主标题
  subtitle: Text('2026-10-29 16:50:17'),                   // 副标题
  trailing: Text('-500.00'),                               // 右侧（金额）
)
```

```
leading      左侧
title        主标题（较大）
subtitle     副标题（较小、灰色）
trailing     右侧
onTap        点了这一行干什么
```

**`ListTile` 是 Material 设计里"列表行"的标准结构，自动帮你排好左右布局。**

---

# 七、弹窗与提示

## showDialog + AlertDialog —— 对话框（退卡确认）

```dart
final ok = await showDialog<bool>(
  context: context,
  builder: (context) => AlertDialog(
    title: const Text('确认退卡'),
    content: const Text('退卡后本次会话结束，确定要退卡吗？'),
    actions: [
      TextButton(
        onPressed: () => Navigator.pop(context, false),    // 取消 → 返回 false
        child: const Text('取消'),
      ),
      FilledButton(
        onPressed: () => Navigator.pop(context, true),     // 确定 → 返回 true
        child: const Text('确定退卡'),
      ),
    ],
  ),
);

if (ok == true) {
  await ejectCard();
}
```

**三个要点：**

```
① showDialog 返回 Future<T> → 用 await 等用户点按钮
② Navigator.pop(context, 值) → 关闭对话框并"带回"一个值
③ 返回值类型由 <bool> 指定
```

## SnackBar + ScaffoldMessenger —— 底部临时提示（密码错误时用）

```dart
ScaffoldMessenger.of(context).showSnackBar(
  SnackBar(content: Text(r.message)),
);
```

**效果：屏幕底部弹出一条黑色提示，几秒后自动消失。**（Android 原生的 Toast 在 Flutter 里就用这个）

**为什么用 `ScaffoldMessenger.of(context)`：**

```
SnackBar 需要一个"能承载它的 Scaffold"
ScaffoldMessenger.of(context) → 找到当前页面最近的那个 Scaffold
```

---

# 八、加载指示

## CircularProgressIndicator —— 转圈

```dart
const Column(
  mainAxisSize: MainAxisSize.min,
  children: [
    CircularProgressIndicator(),      // 一个转圈
    SizedBox(height: 16),
    Text('处理中...'),
  ],
)
```

**在 `_body()` 里，`loading == true` 时就显示这个。**

---

# 九、属性归属速查（最重要的表）

**看到某个参数，想知道它属于谁：**

| 属性 | 属于 | 说明 |
|---|---|---|
| `child` | Padding / Center / Expanded / Container / 所有按钮 | 只放**一个** |
| `children` | Column / Row / ListView / Scaffold 的部分属性 | 放**一个列表** |
| `body` | Scaffold | 内容区 |
| `appBar` | Scaffold | 顶部栏 |
| `title` | AppBar / ListTile / AlertDialog | 标题 |
| `style` | Text / TextField 的 textStyle | 样式对象 |
| `padding` | Padding / InputDecoration / 按钮样式 | 内边距 |
| `controller` | TextField | 文字的控制器 |
| `decoration` | TextField | 外观配置 |
| `onPressed` | 所有按钮 | 点击回调 |
| `onSubmitted` | TextField | 按回车回调 |
| `leading` / `trailing` | ListTile | 左右两端的元素 |
| `actions` | AlertDialog / AppBar | 右侧操作按钮列表 |
| `itemCount` / `itemBuilder` | ListView | 列表项数量 / 每项长什么样 |

---

# 十、读嵌套 Widget 的方法（从外往里）

**Flutter 代码嵌套很深，一行行死读会晕。方法是：先看最外层，抓住"整体布局"，再往里剥。**

## 例子：`_quickAmounts`

```dart
Row(                                  ← ① 横着排
  children: [                         ← ② 里面装一排
    for (final v in values)           ← ③ 对每个数字
      Expanded(                       ← ④ 平均分宽度
        child: Padding(               ← ⑤ 加点空隙
          padding: const EdgeInsets.symmetric(horizontal: 4),
          child: OutlinedButton(      ← ⑥ 描边按钮
            onPressed: () => amountCtrl.text = '$v',   ← 点下去填金额
            child: Text('$v'),        ← 按钮上显示数字
          ),
        ),
      ),
  ],
)
```

**心里默念：**

> "横着排一排按钮，每个平均分宽，每个有点左右空隙，点一下会把那个数字填进金额框。"

## 三步读法

```
第 1 步：看最外层的容器是什么（Row？Column？Scaffold？）
          → 决定整体布局方向

第 2 步：看 children / child 里面有几个东西
          → 决定数量

第 3 步：逐个看每个子 Widget 的参数
          → 决定细节（样式、点击行为）
```

---

# 十一、一页纸速查

```
布局      Column(竖) Row(横) Center(居中) Padding(内边距) SizedBox(间距) Expanded(平分)
骨架      MaterialApp(根) Scaffold(壳) AppBar(顶栏)
展示      Text(文字) Icon(图标) TextStyle(样式) Divider(分割线)
输入      TextField(输入框) TextEditingController(遥控器) InputDecoration(外观)
按钮      ElevatedButton(浮起) OutlinedButton(描边) FilledButton(实心) TextButton(纯文字)
列表      ListView.separated(滚动列表) ListTile(一行)
弹窗      showDialog + AlertDialog / SnackBar + ScaffoldMessenger
加载      CircularProgressIndicator(转圈)

关键参数
    child     → 放一个
    children  → 放一堆
    onPressed → 点击回调
    controller → 输入框遥控器

三个必记
    ① 间距用 SizedBox，不用 margin
    ② 按钮要全宽 → 父容器 crossAxisAlignment: stretch
    ③ ListView 必须有确定高度（不能放在 Center 里）
```
