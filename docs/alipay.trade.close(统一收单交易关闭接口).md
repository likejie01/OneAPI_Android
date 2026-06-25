用于交易创建后，用户在一定时间内未进行支付，可调用该接口直接将未付款的交易进行关闭。

## 公共请求参数

## 业务请求参数

以下参数 二选一 传入

**trade_no**string(64)

【描述】该交易在支付宝系统中的交易流水号。最短 16 位，最长 64 位。和out_trade_no不能同时为空，如果同时传了 out_trade_no和 trade_no，则以 trade_no为准。

【示例值】2013112611001004680073956707

**out_trade_no**string(64)

【描述】订单支付时传入的商户订单号,和支付宝交易号不能同时为空。 trade_no,out_trade_no如果同时存在优先取trade_no

【示例值】HZ0120131127001

**operator_id**可选string(28)

【描述】商家操作员编号 id，由商家自定义。

【示例值】YX01

常见请求示例

默认示例

cURLJavaC#PHPNode.js

```bash
curl 'https://openapi.alipay.com/gateway.do?charset=UTF-8&method=alipay.trade.close&format=json&sign=${sign}&app_id=${appid}&version=1.0&sign_type=RSA2&timestamp=${now}' \
 -F 'app_auth_token=${app_auth_token}' \
 -F 'biz_content={
	"trade_no":"2013112611001004680073956707",
	"out_trade_no":"HZ0120131127001",
	"operator_id":"YX01"
}' 
```

说明：本示例仅供参考。

## 公共响应参数

## 业务响应参数

**trade_no**特殊可选string(64)

【描述】支付宝交易号

【示例值】2013112111001004500000675971

**out_trade_no**特殊可选string(64)

【描述】创建交易传入的商户订单号

【示例值】YX_001

响应示例

正常示例

异常示例

```json
{
    "alipay_trade_close_response": {
        "code": "10000",
        "msg": "Success",
        "trade_no": "2013112111001004500000675971",
        "out_trade_no": "YX_001"
    },
    "sign": "ERITJKEIJKJHKKKKKKKHJEREEEEEEEEEEE"
}
```

说明：本示例仅供参考。

## 公共错误码

[前往查看](https://opendoc.alipay.com/common/02km9f)

## 业务错误码

|错误码|错误描述|解决方案|
|---|---|---|
|ACQ.INVALID_PARAMETER|参数无效|检查请求参数，修改后重新发起请求|
|ACQ.REASON_ILLEGAL_STATUS|交易状态异常|确认交易状态，非待支付状态下不支持关单操作|
|ACQ.REASON_TRADE_STATUS_INVALID|交易状态异常|确认交易状态，非待支付状态下不支持关单操作|
|ACQ.SYSTEM_ERROR|系统异常|重新发起请求|
|ACQ.TRADE_NOT_EXIST|交易不存在|检查传入的交易号和外部订单号是否正确，修改后再重新发起|
|ACQ.TRADE_STATUS_ERROR|交易状态不合法|检查当前交易的状态是不是等待买家付款，只有等待买家付款状态下才能发起交易关闭。|

## 触发通知类型

|通知类型|描述|默认开启|
|---|---|---|
|tradeStatus.TRADE_CLOSED|交易关闭|1|
|tradeStatus.TRADE_SUCCESS|交易成功|0|

## 触发通知示例

```http
https://www.merchant.com/receive_notify.htm?notify_type=trade_status_sync&notify_id=91722adff935e8cfa58b3aabf4dead6ibe&notify_time=2017-02-16 21:46:15&sign_type=RSA2&sign=WcO+t3
```