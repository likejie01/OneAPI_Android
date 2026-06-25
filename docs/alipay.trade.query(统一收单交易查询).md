该接口提供所有支付宝支付订单的查询，商户可以通过该接口主动查询订单状态，完成下一步的业务逻辑。 需要调用查询接口的情况： 当商户后台、网络、服务器等出现异常，商户系统最终未接收到支付通知； 调用支付接口后，返回系统错误或未知交易状态情况； 调用alipay.trade.pay，返回INPROCESS的状态； 调用alipay.trade.cancel之前，需确认支付状态

## 公共请求参数

## 业务请求参数

以下参数 二选一 传入

**out_trade_no**string(64)

【描述】订单支付时传入的商户订单号,和支付宝交易号不能同时为空。  
trade_no,out_trade_no如果同时存在优先取trade_no

【示例值】20150320010101001

**trade_no**string(64)

【描述】支付宝交易号，和商户订单号不能同时为空

【示例值】2014112611001004680 073956707

**query_options**可选string[](128)

【描述】查询选项，商户通过上送该参数来定制同步需要额外返回的信息字段，数组格式。

【枚举值】

交易结算信息: trade_settle_info

交易支付使用的资金渠道: fund_bill_list

交易支付时使用的所有优惠券信息: voucher_detail_list

更多

【示例值】trade_settle_info

常见请求示例

默认示例

cURLJavaC#PHPNode.js

```bash
curl 'https://openapi.alipay.com/gateway.do?charset=UTF-8&method=alipay.trade.query&format=json&sign=${sign}&app_id=${appid}&version=1.0&sign_type=RSA2&timestamp=${now}' \
 -F 'app_auth_token=${app_auth_token}' \
 -F 'biz_content={
	"out_trade_no":"20150320010101001",
	"trade_no":"2014112611001004680 073956707",
	"query_options":[
		"trade_settle_info"
	]
}' 
```

说明：本示例仅供参考。

## 公共响应参数

## 业务响应参数

展开所有属性

**trade_no**必选string(64)

【描述】支付宝交易号

【注意事项】在未生成真实交易时，不返回，需要商户多次调用该接口或支付通知，获取最终的交易号

【示例值】2013112011001004330000121536

**out_trade_no**必选string(64)

【描述】商家订单号

【示例值】6823789339978248

**trade_status**必选string(32)

【描述】交易状态：WAIT_BUYER_PAY（交易创建，等待买家付款）、TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）、TRADE_SUCCESS（交易支付成功）、TRADE_FINISHED（交易结束，不可退款）

【示例值】TRADE_CLOSED

**total_amount**必选price(11)

【描述】交易的订单金额，单位为元，两位小数。该参数的值为支付时传入的total_amount

【示例值】88.88

**fund_bill_list**必选TradeFundBill[]

【描述】交易支付使用的资金渠道。  
只有在签约中指定需要返回资金明细，或者入参的query_options中指定时才返回该字段信息。

**buyer_user_id**必选string(16)

【描述】买家在支付宝的用户id

新商户建议使用buyer_open_id替代该字段。对于新商户，buyer_user_id字段未来计划逐步回收，存量商户可继续使用。如使用buyer_open_id，请确认 应用-开发配置-openid配置管理 已启用。无该配置项，可查看[openid配置申请](https://opendocs.alipay.com/mini/0ai9ok?pathHash=de631c06)。

【示例值】2088101117955611

**send_pay_date**特殊可选date(32)

【描述】本次交易打款给卖家的时间

【示例值】2014-11-27 15:45:57

**receipt_amount**特殊可选string(11)

【描述】实收金额，单位为元，两位小数。该金额为本笔交易，商户账户能够实际收到的金额

【示例值】15.25

**store_id**特殊可选string(32)

【描述】商户门店编号

【示例值】NJ_S_001

**terminal_id**特殊可选string(32)

【描述】商户机具终端编号

【示例值】NJ_T_001

**store_name**特殊可选string(512)

【描述】请求交易支付中的商户店铺的名称

【示例值】证大五道口店

**buyer_open_id**特殊可选string(128)

【描述】买家支付宝用户唯一标识  详情可查看 [openid简介](https://opendocs.alipay.com/mini/0ai2i6?pathHash=13dd5946)

【示例值】01501o8f93I3nJAGB1jG4ONxtxV25DCN3Gec3uggnC4CJU0

**discount_amount**特殊可选string(11)

【描述】平台优惠金额

【示例值】88.88

**ext_infos**特殊可选string(1024)

【描述】交易额外信息，特殊场景下与支付宝约定返回。  
json格式。

【示例值】{"action":"cancel"}

**buyer_user_type**特殊可选string(18)

【描述】买家用户类型。CORPORATE:企业用户；PRIVATE:个人用户。

【枚举值】

企业用户: CORPORATE

个人用户: PRIVATE

【示例值】PRIVATE

**mdiscount_amount**特殊可选string(11)

【描述】商家优惠金额

【示例值】88.88

**buyer_logon_id**可选string(100)

【描述】买家支付宝账号

【注意事项】在未生成真实交易时，不返回，需要商户多次调用该接口或支付通知，获取最终的用户信息

【示例值】159****5620

**buyer_pay_amount**可选price(11)

【描述】买家实付金额，单位为元，两位小数。该金额代表该笔交易买家实际支付的金额，不包含商户折扣等金额

【示例值】8.88

**invoice_amount**可选price(11)

【描述】交易中用户支付的可开具发票的金额，单位为元，两位小数。该金额代表该笔交易中可以给用户开具发票的金额

【示例值】12.11

**point_amount**可选price(11)

【描述】积分支付的金额，单位为元，两位小数。该金额代表该笔交易中用户使用积分支付的金额，比如集分宝或者支付宝实时优惠等

【示例值】10

响应示例

正常示例

异常示例

```json
{
    "alipay_trade_query_response": {
        "code": "10000",
        "msg": "Success",
        "trade_no": "2013112011001004330000121536",
        "out_trade_no": "6823789339978248",
        "buyer_logon_id": "159****5620",
        "trade_status": "TRADE_CLOSED",
        "total_amount": "88.88",
        "send_pay_date": "2014-11-27 15:45:57",
        "buyer_pay_amount": "8.88",
        "invoice_amount": "12.11",
        "point_amount": "10",
        "receipt_amount": "15.25",
        "store_id": "NJ_S_001",
        "terminal_id": "NJ_T_001",
        "fund_bill_list": [
            {
                "fund_channel": "ALIPAYACCOUNT",
                "amount": "10",
                "real_amount": "11.21"
            }
        ],
        "store_name": "证大五道口店",
        "buyer_open_id": "01501o8f93I3nJAGB1jG4ONxtxV25DCN3Gec3uggnC4CJU0",
        "buyer_user_id": "2088101117955611",
        "discount_amount": "88.88",
        "ext_infos": "{\"action\":\"cancel\"}",
        "buyer_user_type": "PRIVATE",
        "mdiscount_amount": "88.88"
    },
    "sign": "ERITJKEIJKJHKKKKKKKHJEREEEEEEEEEEE"
}
```

说明：本示例仅供参考。

## 公共错误码

[前往查看](https://opendoc.alipay.com/common/02km9f)

## 业务错误码

| 错误码                   | 错误描述     | 解决方案                   |
| --------------------- | -------- | ---------------------- |
| ACQ.INVALID_PARAMETER | 参数无效     | 检查请求参数，修改后重新发起请求       |
| ACQ.SYSTEM_ERROR      | 系统错误     | 重新发起请求                 |
| ACQ.TRADE_NOT_EXIST   | 查询的交易不存在 | 检查传入的交易号是否正确，修改后重新发起请求 |