import 'package:flutter/material.dart';

import 'api.dart';

void main() => runApp(const AtmApp());

class AtmApp extends StatelessWidget {
  const AtmApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ATM自助服务',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.blue),
      home: const AtmHome(),
    );
  }
}

/// 界面步骤 —— 单页状态机，用 step 切换显示的界面
enum Step { insertCard, password, menu, withdraw,deposit,transfer, detail,balance, result }

class AtmHome extends StatefulWidget {
  const AtmHome({super.key});

  @override
  State<AtmHome> createState() => _AtmHomeState();
}

class _AtmHomeState extends State<AtmHome> {
  //--------------状态-------------
  Step step = Step.insertCard;
  bool loading = false;

  String sessionToken = '';
  String cardNoMasked = '';
  String customerName = '';

  String resultText = '';
  String balanceText = '';
  String menuBalance = '--';
  List<dynamic> detailList = [];

  //-------------------输入框--------------------
  final cardCtrl = TextEditingController();
  final pwdCtrl = TextEditingController();
  final amountCtrl = TextEditingController();
  final targetCtrl = TextEditingController();

  @override
  void dispose() {
    cardCtrl.dispose();
    pwdCtrl.dispose();
    amountCtrl.dispose();
    targetCtrl.dispose();
    super.dispose();
  }

  //--------------通用工具------------

  /// 发请求 + 统一控制loading
  Future<ApiResult?> _call(String path, Map<String, dynamic> body) async {
    setState(() => loading = true);
    final r = await apiPost(path, body);
    if (!mounted) return null;
    setState(() => loading = false);
    return r;
  }

  /// 跳到结果页
  void _goResult(String text) {
    setState(() {
      resultText = text;
      step = Step.result;
    });
  }

  ///统一处理失败：401 踢回插卡页，其它显示后端的中文提示
  void _fail(ApiResult r) {
    if (r.code == 401) {
      setState(() {
        sessionToken = '';
        cardNoMasked = '';
        customerName = '';
        resultText = '${r.message}\n\n请重新插卡';
        step = Step.result;
      });
    } else {
      _goResult(r.message);
    }
  }

  //--------------业务动作------------

  /// ① 插卡
  Future<void> insertCard() async {
    if (cardCtrl.text.trim().isEmpty) {
      _goResult('请输入银行卡号');
      return;
    }
    final r = await _call('/api/atm/insert-card', {
      'cardNo': cardCtrl.text.trim(),
      'deviceNo': 'ATM001',
    });
    if (r == null) return;
    if (!r.ok) {
      _goResult(r.message);
      return;
    }
    setState(() {
      sessionToken = (r.data['sessionToken'] ?? '').toString();
      cardNoMasked = (r.data['cardNo'] ?? '').toString();
      customerName = (r.data['customerName'] ?? '').toString();
      pwdCtrl.clear();
      step = Step.password;
    });
  }

  /// ② 验证交易密码
  Future<void> verifyPassword() async {
    if (pwdCtrl.text.trim().isEmpty) {
      _goResult('请输入交易密码');
      return;
    }
    final r = await _call('/api/atm/verify-password', {
      'sessionToken': sessionToken,
      'password': pwdCtrl.text.trim(),
    });
    if (r == null) return;

    if (!r.ok) {
      if (r.code == 401) {
        _fail(r);
        return;
      }
      // 密码错误：留在本页重试，用 SnackBar 提示还剩几次
      pwdCtrl.clear();
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(r.message)));
      return;
    }

    setState(() => step = Step.menu);
    _refreshMenuBalance();
  }

  /// ③ 取款
  Future<void> withdraw() async {
    final amount = int.tryParse(amountCtrl.text.trim());
    if (amount == null || amount <= 0) {
      _goResult('请输入正确的金额');
      return;
    }
    final r = await _call('/api/atm/withdraw', {
      'sessionToken': sessionToken,
      'amount': amount,
    });
    if (r == null) return;
    if (!r.ok) {
      _fail(r);
      return;
    }
    final d = r.data;
    _goResult(
      '✅ 取款成功\n\n'
      '金额：${toDouble(d['amount']).toStringAsFixed(2)} 元\n'
      '出钞：${d['cashDetail']}\n'
      '余额：${toDouble(d['balanceAfter']).toStringAsFixed(2)} 元\n\n'
      '流水号\n${d['transNo']}',
    );
  }

  ///存款
  Future<void> deposit() async{
    final amount = int.tryParse(amountCtrl.text.trim());
    if(amount == null || amount <= 0){
      _goResult('请输入正确的金额');
      return;
    }
    final r = await _call('/api/atm/deposit',{
      'sessionToken': sessionToken,
      'amount':amount,
    });
    if(r == null) return;
    if(!r.ok){
      _fail(r);
      return;
    }
    final d = r.data;
    _goResult(
      '存款成功\n\n'
       '金额：${toDouble(d['amount']).toStringAsFixed(2)} 元\n'
       '进钞：${d['cashDetail']}\n'
       '余额：${toDouble(d['balanceAfter']).toStringAsFixed(2)} 元\n\n'
       '流水号\n${d['transNo']}',
    );
  }

  ///转账
  Future<void> transfer() async{
    if(targetCtrl.text.trim().isEmpty){
      _goResult('请输入对方卡号');
      return;
    }
    if (targetCtrl.text.trim() == cardCtrl.text.trim()){
      _goResult('不能给自己转账');
      return;
    }
    final amount = int.tryParse(amountCtrl.text.trim());
    if(amount == null || amount <= 0){
      _goResult('请输入正确的金额');
      return;
    }
    final r = await _call('/api/atm/transfer', {
      'sessionToken': sessionToken,
      'targetCardNo': targetCtrl.text.trim(),
      'amount': amount,
    });
    if(r == null) return;
    if(!r.ok){
      _fail(r);
      return;
    }
    final d = r.data;
    _goResult(
      '转账成功\n\n'
      '收款人：${d['targetName']}\n'
      '收款卡号：${d['targetCardNo']}\n'
      '转账金额：${toDouble(d['amount']).toStringAsFixed(2)}元\n'
      '手续费：${toDouble(d['fee']).toStringAsFixed(2)} 元\n'
      '扣款合计：${toDouble(d['totalAmount']).toStringAsFixed(2)} 元\n'
      '余额：${toDouble(d['balanceAfter']).toStringAsFixed(2)} 元\n\n'
      '流水号\n${d['transNo']}',
    );
  }

  /// ④ 余额查询
  Future<void> queryBalance() async {
    final r = await _call('/api/atm/balance', {'sessionToken': sessionToken});
    if (r == null) return;
    if (!r.ok) {
      _fail(r);
      return;
    }
    final d = r.data;
    setState(() {
      balanceText = '卡号：${d['cardNo']}\n'
          '持卡人：${d['customerName']}\n'
          '账号：${d['accountNo']}\n\n'
          '可用余额\n${toDouble(d['availableBalance']).toStringAsFixed(2)} ${d['currency']}\n\n'
          '账面余额\n${toDouble(d['balance']).toStringAsFixed(2)} ${d['currency']}';
      step = Step.balance;
    });
  }

  ///进菜单时后台刷新余额(不显示 loading,不跳页)
  Future<void> _refreshMenuBalance() async{
    final r = await apiPost('/api/atm/balance', {'sessionToken':sessionToken});
    if(!mounted) return;
    if(r.code == 200){
      setState(() {
        menuBalance = toDouble(r.data['availableBalance']).toStringAsFixed(2);
      });
    }
  }

  ///交易明细
  Future<void> queryTransactions() async{
    final r = await _call('/api/atm/transactions', {'sessionToken': sessionToken,'limit':10});
    if(r == null) return;
    if(!r.ok){
      _fail(r);
      return;
    }
    setState(() {
      detailList = (r.data as List).cast<dynamic>();
      step = Step.detail;
    });
  }
  ///带确认的退卡
  Future<void> ejectCardWithConfirm() async{
    final ok = await showDialog<bool>(context: context,
        builder: (context) => AlertDialog(
          title: const Text('确认退卡'),
          content: const Text('退卡后本次会话结束,确定要退卡吗？'),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context,false),
                child: const Text('取消')
            ),
            FilledButton(onPressed: () => Navigator.pop(context, true),
                child: const Text('确定退卡')),
          ],
        ));
  }

  /// ⑤ 退卡
  Future<void> ejectCard() async {
    await _call('/api/atm/eject-card', {'sessionToken': sessionToken});
    setState(() {
      sessionToken = '';
      cardNoMasked = '';
      customerName = '';
      menuBalance = '--';
      cardCtrl.clear();
      pwdCtrl.clear();
      amountCtrl.clear();
      resultText = '请取走您的银行卡\n\n感谢使用，再见！';
      step = Step.result;
    });
  }

  //--------------界面------------

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(_title()), centerTitle: true),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: step == Step.detail ? _body() : Center(child: _body()),
      ),
    );
  }

  String _title() => switch (step) {
        Step.insertCard => 'ATM自助服务',
        Step.password => '请输入交易密码',
        Step.menu => '请选择业务',
        Step.withdraw => '取款',
        Step.deposit => '存款',
        Step.transfer => '转账',
        Step.balance => '余额查询',
        Step.result => '操作结果',
        Step.detail => '交易明细',
      };

  Widget _body() {
    if (loading) {
      return const Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          CircularProgressIndicator(),
          SizedBox(height: 16),
          Text('处理中...'),
        ],
      );
    }
    return switch (step) {
      Step.insertCard => _insertCardView(),
      Step.password => _passwordView(),
      Step.menu => _menuView(),
      Step.withdraw => _withdrawView(),
      Step.deposit => _depositView(),
      Step.transfer => _transferView(),
      Step.balance => _balanceView(),
      Step.result => _resultView(),
      Step.detail => _detailView(),
    };
  }

  Widget _bigButton(String text, VoidCallback onTap) {
    return ElevatedButton(
      onPressed: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 14),
        child: Text(text, style: const TextStyle(fontSize: 18)),
      ),
    );
  }

  /// 金额快捷按钮组：点一下自动填入金额框
  Widget _quickAmounts(List<int> values) {
    return Row(
      children: [
        for (final v in values)
          Expanded(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4),
              child: OutlinedButton(
                onPressed: () => amountCtrl.text = '$v',
                style: OutlinedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                ),
                child: Text('$v'),
              ),
            ),
          ),
      ],
    );
  }

  ///后端返回“2026-10-25T11:11:00”,显示成“2026-10-25 11:11:00”
  String _fmtTime(dynamic v){
    final s = (v ?? '').toString().replaceFirst('T', '');
    return s.length >= 19 ? s.substring(0,19) : s;
  }

  Widget _insertCardView() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Icon(Icons.credit_card, size: 72, color: Colors.blue),
        const SizedBox(height: 24),
        const Text('请插入银行卡',
            textAlign: TextAlign.center, style: TextStyle(fontSize: 20)),
        const SizedBox(height: 6),
        const Text('（演示环境：直接输入卡号）',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 12, color: Colors.grey)),
        const SizedBox(height: 20),
        TextField(
          controller: cardCtrl,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(
            labelText: '银行卡号',
            hintText: '6222020000000001',
            border: OutlineInputBorder(),
            prefixIcon: Icon(Icons.credit_card),
          ),
          onSubmitted: (_) => insertCard(),
        ),
        const SizedBox(height: 24),
        _bigButton('插 卡', insertCard),
      ],
    );
  }

  Widget _passwordView() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Icon(Icons.lock_outline, size: 72, color: Colors.orange),
        const SizedBox(height: 20),
        Text('欢迎，$customerName',
            textAlign: TextAlign.center, style: const TextStyle(fontSize: 18)),
        Text(cardNoMasked,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 13, color: Colors.grey)),
        const SizedBox(height: 20),
        TextField(
          controller: pwdCtrl,
          obscureText: true,
          keyboardType: TextInputType.number,
          maxLength: 6,
          decoration: const InputDecoration(
            labelText: '交易密码（6 位）',
            border: OutlineInputBorder(),
            prefixIcon: Icon(Icons.password),
          ),
          onSubmitted: (_) => verifyPassword(),
        ),
        _bigButton('确 认', verifyPassword),
        TextButton(onPressed: ejectCardWithConfirm, child: const Text('退 卡')),
      ],
    );
  }

  Widget _menuView() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text('欢迎，$customerName',
            textAlign: TextAlign.center, style: const TextStyle(fontSize: 20)),
        Text(cardNoMasked,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 13, color: Colors.grey)),
        const SizedBox(height: 16),
        Text('可用余额 $menuBalance 元',
              textAlign: TextAlign.center,
               style: const TextStyle(
                 fontSize: 22,fontWeight: FontWeight.bold,color: Colors.blue)),
        const SizedBox(height: 28),
        _bigButton('取 款', () {
          amountCtrl.clear();
          setState(() => step = Step.withdraw);
        }),
        const SizedBox(height: 12),
        _bigButton('存款', (){
          amountCtrl.clear();
          setState(() =>step = Step.deposit);
        }),
        const SizedBox(height: 12),
        _bigButton('转账', (){
          amountCtrl.clear();
          targetCtrl.clear();
          setState(() => step = Step.transfer);
        }),
        const SizedBox(height: 12),
        _bigButton('交易明细', queryTransactions),
        const SizedBox(height: 12),
        _bigButton('余额查询', queryBalance),
        const SizedBox(height: 12),
        _bigButton('退 卡', ejectCardWithConfirm),
        ]
    );
  }

  Widget _withdrawView() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Icon(Icons.payments_outlined, size: 72, color: Colors.green),
        const SizedBox(height: 20),
        const Text('请输入取款金额',
            textAlign: TextAlign.center, style: TextStyle(fontSize: 20)),
        const SizedBox(height: 6),
        const Text('单笔 100 ~ 5000 元，必须是 100 的整数倍',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 12, color: Colors.grey)),
        const SizedBox(height: 20),
        TextField(
          controller: amountCtrl,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(
            labelText: '金额（元）',
            border: OutlineInputBorder(),
            prefixIcon: Icon(Icons.currency_yuan),
          ),
          onSubmitted: (_) => withdraw(),
        ),
        const SizedBox(height: 12),
        _quickAmounts(const[100,500,1000,2000]),
        const SizedBox(height: 24),
        _bigButton('确 认', withdraw),
        TextButton(
            onPressed: () => setState(() => step = Step.menu),
            child: const Text('返回')),
      ],
    );
  }

  Widget _depositView(){
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Icon(Icons.savings_outlined,size: 72,color: Colors.teal,),
        const SizedBox(height: 20),
        const Text('请存入现金',
          textAlign: TextAlign.center,style: TextStyle(fontSize: 20)),
        const SizedBox(height: 20),
        TextField(
          controller: amountCtrl,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(
            labelText: '金额（元）',
            border: OutlineInputBorder(),
            prefixIcon: Icon(Icons.currency_yuan),
          ),
          onSubmitted: (_) => deposit(),
        ),
        const SizedBox(height: 12),
        _quickAmounts(const[100,500,1000,2000]),
        const SizedBox(height: 24),
        _bigButton('确认', deposit),
        TextButton(
        onPressed: () => setState(() => step = Step.menu),
        child: const Text('返回')),
      ],
    );
  }

  Widget _transferView(){
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Icon(Icons.swap_horiz,size: 72,color: Colors.purple),
        const SizedBox(height: 20),
        const Text('转账',
          textAlign: TextAlign.center,style: TextStyle(fontSize: 20)),
        const SizedBox(height: 6),
        const Text('单笔 1 - 50000 元 ，手续费 0.5%(最低1元，最高50元)',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 12,color: Colors.grey)),
        const SizedBox(height: 20),
        TextField(
          controller: targetCtrl,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(
            labelText: '对方卡号',
              hintText: '6222020000000002',
              border: OutlineInputBorder(),
            prefixIcon: Icon(Icons.credit_card),
          ),
        ),
        const SizedBox(height: 16),
        TextField(
          controller: amountCtrl,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(
            labelText: '转账金额（元）',
            border: OutlineInputBorder(),
            prefixIcon: Icon(Icons.currency_yuan),
          ),
          onSubmitted: (_) => transfer(),
        ),
        const SizedBox(height: 12),
        _quickAmounts(const[100,500,1000,2000]),
        const SizedBox(height: 24),
        _bigButton('确认', transfer),
        TextButton(
            onPressed: () => setState(() => step = Step.menu),
            child: const Text('返回')),
      ],
    );
  }

  Widget _detailView() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text('$customerName    $cardNoMasked',
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 13, color: Colors.grey)),
        const SizedBox(height: 12),
        Expanded(
          child: detailList.isEmpty
              ? const Center(child: Text('暂无交易记录'))
              : ListView.separated(
            itemCount: detailList.length,
            separatorBuilder: (context, index) => const Divider(height: 1),
            itemBuilder: (context, i) {
              final t = detailList[i];
              final isOut = ['WITHDRAW', 'TRANSFER_OUT', 'FEE']
                  .contains(t['transTypeCode']);
              return ListTile(
                leading: Icon(
                  isOut ? Icons.arrow_upward : Icons.arrow_downward,
                  color: isOut ? Colors.red : Colors.green,
                ),
                title: Text((t['transTypeName'] ?? '').toString()),
                subtitle: Text(_fmtTime(t['transTime']),
                    style: const TextStyle(fontSize: 12)),
                trailing: Text(
                  '${isOut ? '-' : '+'}${toDouble(t['amount']).toStringAsFixed(2)}',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: isOut ? Colors.red : Colors.green,
                  ),
                ),
              );
            },
          ),
        ),
        const SizedBox(height: 12),
        _bigButton('返回菜单', () => setState(() => step = Step.menu)),
      ],
    );
  }

  Widget _balanceView() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Icon(Icons.account_balance_wallet, size: 72, color: Colors.blue),
        const SizedBox(height: 20),
        Text(balanceText,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 15, height: 1.6)),
        const SizedBox(height: 28),
        _bigButton('返回菜单', () => setState(() => step = Step.menu)),
      ],
    );
  }

  Widget _resultView() {
    final loggedIn = sessionToken.isNotEmpty;
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(resultText,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 16, height: 1.7)),
        const SizedBox(height: 28),
        if (loggedIn)
          _bigButton('返回菜单', () => setState(() => step = Step.menu))
        else
          _bigButton('重新插卡', () {
            setState(() {
              cardCtrl.clear();
              pwdCtrl.clear();
              amountCtrl.clear();
              step = Step.insertCard;
            });
          }),
      ],
    );
  }
}
