// ATM 自助服务 —— 冒烟测试
//
// 只验证「App 能启动、插卡页能正确渲染」，
// 第一帧不发网络请求，所以跑测试不需要先启动后端。

import 'package:flutter_test/flutter_test.dart';

import 'package:atm_app/main.dart';

void main() {
  testWidgets('App 能启动并显示插卡页', (WidgetTester tester) async {
    // 构建整个 App
    await tester.pumpWidget(const AtmApp());

    // 第一帧不应该抛异常
    expect(tester.takeException(), isNull);

    // 插卡页的关键文字应该出现
    expect(find.textContaining('请插入银行卡'), findsWidgets);
  });
}
