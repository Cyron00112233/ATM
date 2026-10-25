import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

///后端服务地址（以后部署到别的机器，只改这一行）
const String baseUrl = 'http://localhost:8080';

///后端统一响应外壳：{code,message,data,timestamp}
class ApiResult {
  final int code;
  final String message;
  final dynamic data;

  ApiResult(this.code,this.message,this.data);

  ///是否成功（后端约定 code == 200）
  bool get ok => code == 200;
}

///统一的POST请求封装
Future<ApiResult> apiPost(String path,Map<String,dynamic> body) async{
  try{
    final res = await http
        .post(
      Uri.parse('$baseUrl$path'),
      headers: {'Content-Type':'application/json;charset=utf-8'},
      body: jsonEncode(body),
    )
        .timeout(const Duration(seconds: 10));

    //必须用bodyBytes+utf8.decode,直接用res.body中文会乱码
    final map = jsonDecode(utf8.decode(res.bodyBytes)) as Map<String,dynamic>;

    return ApiResult(
        (map['code'] as num).toInt(),
        (map['message']??'').toString(),
         map['data'],
    );
  }catch(e){
    return ApiResult(-1, '网络异常：$e\n\n请确认后端已启动（$baseUrl）', null);
  }
}

/// 金额安全转换：后端返回的可能是 int，也可能是 double
double toDouble(dynamic v) => (v as num).toDouble();