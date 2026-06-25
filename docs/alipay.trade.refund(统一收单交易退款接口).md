当交易发生之后一段时间内，由于买家或者卖家的原因需要退款时，卖家可以通过退款接口将支付款退还给买家，支付宝将在收到退款请求并且验证成功之后，按照退款规则将支付款按原路退到买家帐号上。  
交易超过约定时间（签约时设置的可退款时间）的订单无法进行退款。  
支付宝退款支持单笔交易分多次退款，多次退款需要提交原支付订单的订单号和设置不同的退款请求号。一笔退款失败后重新提交，要保证重试时退款请求号不能变更，防止该笔交易重复退款。  
同一笔交易累计提交的退款金额不能超过原始交易总金额。

注意：

1. 同一笔交易的退款至少间隔3s后发起  
2. 请严格按照接口文档中的参数进行接入。若在此接口中传入【非当前接口文档中的参数】会造成【退款失败或重复退款】。  
3. 该接口不可与其他退款产品混用。若商户侧同一笔退款请求已使用了当前接口退款的情况下，【再使用其他退款产品进行退款】可能会造成【重复退款】。  
4. 退款成功判断说明：接口返回fund_change=Y为退款成功，fund_change=N或无此字段值返回时需通过退款查询接口进一步确认退款状态。详见[退款成功判断指导](https://opendocs.alipay.com/support/01rawa)。注意，接口中code=10000，仅代表本次退款请求成功，不代表退款成功。  
5. 若退款原订单涉及分账，在发起退款前，需要分账接收方在 [支付宝商家平台](https://b.alipay.com/page/portal/home) > 资金管理 > 资金服务 > 分账接收 设置中，开启分账回退授权 后，才可以正常退款。且 不支持接收方为个人 的分账单发起退款退分账。

## 公共请求参数

## 业务请求参数

展开所有属性

**refund_amount**必选price(16)

【描述】退款金额。 需要退款的金额，该金额不能大于订单金额，单位为元，支持两位小数。 注：如果正向交易使用了营销，该退款金额包含营销金额，支付宝会按业务规则分配营销和买家自有资金分别退多少，默认优先退买家的自有资金。如交易总金额100元，用户支付时使用了80元自有资金和20元无资金流的营销券，商家实际收款80元。如果首次请求退款60元，则60元全部从商家收款资金扣除退回给用户自有资产；如果再请求退款40元，则从商家收款资金扣除20元退回用户资产以及把20元的营销券退回给用户（券是否可再使用取决于券的规则配置）。

【示例值】200.12

以下参数 二选一 传入

**out_trade_no**string(64)

【描述】商户订单号。 订单支付时传入的商户订单号，商家自定义且保证商家系统中唯一。与支付宝交易号 trade_no 不能同时为空。

【示例值】20150320010101001

**trade_no**string(64)

【描述】支付宝交易号。 和商户订单号 out_trade_no 不能同时为空，两者同时存在时，优先取值trade_no

【示例值】2014112611001004680073956707

**refund_reason**可选string(256)

【描述】退款原因说明。 商家自定义，将在会在商户和用户的pc退款账单详情中展示

【示例值】正常退款

**out_request_no**可选string(64)

【描述】退款请求号。 标识一次退款请求，需要保证在交易号下唯一，如需部分退款，则此参数必传。 注：针对同一次退款请求，如果调用接口失败或异常了，重试时需要保证退款请求号不能变更，防止该笔交易重复退款。支付宝会保证同样的退款请求号多次请求只会退一次。

【必选条件】部分退款时必选

【示例值】HZ01RF001

**refund_goods_detail**可选RefundGoodsDetail[]

【描述】退款包含的商品列表信息

**refund_royalty_parameters**可选OpenApiRoyaltyDetailInfoPojo[]

【描述】退分账明细信息。 注： 1.当面付且非直付通模式无需传入退分账明细，系统自动按退款金额与订单金额的比率，从收款方和分账收入方退款，不支持指定退款金额与退款方。 2.直付通模式，电脑网站支付，手机 APP 支付，手机网站支付产品，须在退款请求中明确是否退分账，从哪个分账收入方退，退多少分账金额；如不明确，默认从收款方退款，收款方余额不足退款失败。不支持系统按比率退款。

**query_options**可选string[](1024)

【描述】查询选项。 商户通过上送该参数来定制同步需要额外返回的信息字段，数组格式。

【枚举值】

本次退款使用的资金渠道: refund_detail_item_list

银行卡冲退信息: deposit_back_info

本次退款退的券信息: refund_voucher_detail_list

【示例值】["refund_detail_item_list"]

**related_settle_confirm_no**可选string(64)

【描述】针对账期交易，在确认结算后退款的话，需要指定确认结算时的结算单号。

【示例值】2024041122001495000530302869

常见请求示例

默认示例

cURLJavaC#PHPNode.js

```bash
curl 'https://openapi.alipay.com/gateway.do?charset=UTF-8&method=alipay.trade.refund&format=json&sign=${sign}&app_id=${appid}&version=1.0&sign_type=RSA2&timestamp=${now}' \
 -F 'app_auth_token=${app_auth_token}' \
 -F 'biz_content={
	"out_trade_no":"20150320010101001",
	"trade_no":"2014112611001004680073956707",
	"refund_amount":"200.12",
	"refund_reason":"正常退款",
	"out_request_no":"HZ01RF001",
	"refund_goods_detail":[
		{
			"out_sku_id":"outSku_01",
			"out_item_id":"outItem_01",
			"goods_id":"apple-01",
			"refund_amount":"19.50",
			"out_certificate_no_list":[
				"202407013232143241231243243423"
			]
		}
	],
	"refund_royalty_parameters":[
		{
			"amount":"0.1",
			"trans_in":"2088101126708402",
			"royalty_type":"transfer",
			"trans_out":"2088101126765726",
			"trans_out_type":"userId",
			"royalty_scene":"达人佣金",
			"trans_in_type":"userId",
			"trans_in_name":"张三",
			"desc":"分账给2088101126708402"
		}
	],
	"query_options":[
		"refund_detail_item_list"
	],
	"related_settle_confirm_no":"2024041122001495000530302869"
}' 
```

说明：本示例仅供参考。

## 公共响应参数

## 业务响应参数

展开所有属性

**trade_no**必选string(64)

【描述】支付宝交易号

【示例值】2013112011001004330000121536

**out_trade_no**必选string(64)

【描述】商户订单号

【示例值】6823789339978248

**buyer_logon_id**必选string(100)

【描述】用户的登录id

【示例值】159****5620

**refund_fee**必选price(11)

【描述】退款总金额。单位：元。 指该笔交易累计已经退款成功的金额。

【示例值】88.88

**refund_detail_item_list**特殊可选TradeFundBill[]

【描述】退款使用的资金渠道。 只有在签约中指定需要返回资金明细，或者入参的query_options中指定时才返回该字段信息。

**store_name**特殊可选string(512)

【描述】交易在支付时候的门店名称

【必选条件】交易在支付时候的门店名称

【示例值】望湘园联洋店

**buyer_user_id**特殊可选string(28)

【描述】买家在支付宝的用户id

新商户建议使用buyer_open_id替代该字段。对于新商户，buyer_user_id字段未来计划逐步回收，存量商户可继续使用。如使用buyer_open_id，请确认 应用-开发配置-openid配置管理 已启用。无该配置项，可查看[openid配置申请](https://opendocs.alipay.com/mini/0ai9ok?pathHash=de631c06)。

【示例值】2088101117955611

**buyer_open_id**特殊可选string(128)

【描述】买家支付宝用户唯一标识  详情可查看 [openid简介](https://opendocs.alipay.com/mini/0ai2i6?pathHash=13dd5946)

【示例值】074a1CcTG1LelxKe4xQC0zgNdId0nxi95b5lsNpazWYoCo5

**send_back_fee**特殊可选string(11)

【描述】本次商户实际退回金额。单位：元。 说明：如需获取该值，需在入参query_options中传入 refund_detail_item_list。

【示例值】1.8

**pre_auth_cancel_fee****｜撤销的预授权金额**特殊可选string(12)

【描述】当用户使用芝麻信用先享后付时，且当前的操作为预授权撤销动作时，会返回该字段，代表当前撤销的预授权金额，单位元。

【必选条件】当用户使用芝麻信用先享后付时，且当前的操作为预授权撤销动作时，会返回该字段。

【示例值】12.45

**fund_change**可选string(1)

【描述】本次退款是否发生了资金变化

【示例值】Y

**refund_hyb_amount**可选string(11)

【描述】本次请求退惠营宝金额。单位：元。

【示例值】10.24

**refund_charge_info_list**可选RefundChargeInfo[]

【描述】退费信息

**refund_voucher_detail_list**可选VoucherDetail[]

【描述】本交易支付时使用的所有优惠券信息。 只有在query_options中指定了refund_voucher_detail_list时才返回该字段信息。

响应示例

正常示例

异常示例

```json
{
    "alipay_trade_refund_response": {
        "code": "10000",
        "msg": "Success",
        "trade_no": "2013112011001004330000121536",
        "out_trade_no": "6823789339978248",
        "buyer_logon_id": "159****5620",
        "fund_change": "Y",
        "refund_fee": "88.88",
        "refund_detail_item_list": [
            {
                "fund_channel": "ALIPAYACCOUNT",
                "amount": "10",
                "real_amount": "11.21",
                "fund_type": "DEBIT_CARD"
            }
        ],
        "store_name": "望湘园联洋店",
        "buyer_user_id": "2088101117955611",
        "buyer_open_id": "074a1CcTG1LelxKe4xQC0zgNdId0nxi95b5lsNpazWYoCo5",
        "send_back_fee": "1.8",
        "refund_hyb_amount": "10.24",
        "refund_charge_info_list": [
            {
                "refund_charge_fee": "0.01",
                "switch_fee_rate": "0.01",
                "charge_type": "trade",
                "refund_sub_fee_detail_list": [
                    {
                        "refund_charge_fee": "0.10",
                        "switch_fee_rate": "0.01"
                    }
                ]
            }
        ],
        "refund_voucher_detail_list": [
            {
                "id": "2015102600073002039000002D5O",
                "name": "XX超市5折优惠",
                "type": "ALIPAY_FIX_VOUCHER",
                "amount": "10.00",
                "merchant_contribute": "9.00",
                "other_contribute": "1.00",
                "memo": "学生专用优惠",
                "template_id": "20171030000730015359000EMZP0",
                "other_contribute_detail": [
                    {
                        "contribute_type": "BRAND",
                        "contribute_amount": "8.00"
                    }
                ],
                "purchase_buyer_contribute": "2.01",
                "purchase_merchant_contribute": "1.03",
                "purchase_ant_contribute": "0.82"
            }
        ],
        "pre_auth_cancel_fee": "12.45"
    },
    "sign": "ERITJKEIJKJHKKKKKKKHJEREEEEEEEEEEE"
}
```

说明：本示例仅供参考。

## 公共错误码

[前往查看](https://opendoc.alipay.com/common/02km9f)

## 业务错误码

| 错误码                                        | 错误描述               | 解决方案                                                                                                                                                                  |
| ------------------------------------------ | ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| ACQ.ALLOC_AMOUNT_VALIDATE_ERROR            | 退分账金额超限            | 请调整退分账金额后重试                                                                                                                                                           |
| ACQ.BUYER_ENABLE_STATUS_FORBID             | 买家状态异常             | 联系支付宝小二确认买家状态异常原因，或者可联系买家进行线下退款处理                                                                                                                                     |
| ACQ.BUYER_ERROR                            | 买家状态异常。            | 联系支付宝小二确认买家状态异常原因，或者可联系买家进行线下退款处理                                                                                                                                     |
| ACQ.BUYER_NOT_EXIST                        | 买家不存在              | 买家已经注销账号，建议联系买家进行线下退款处理                                                                                                                                               |
| ACQ.CURRENCY_NOT_SUPPORT                   | 退款币种不支持            | 请确认传入的退款币种是否正确                                                                                                                                                        |
| ACQ.CUSTOMER_VALIDATE_ERROR                | 账户已注销或者被冻结         | 请查询账户状态：1. 如果账户已注销，请线下处理；2. 如果账户已冻结，请联系支付宝小二确认冻结原因。                                                                                                                   |
| ACQ.DISCORDANT_REPEAT_REQUEST              | 请求信息不一致            | 退款请求号对应的退款已经执行成功，且本次请求的退款金额与之前请求的金额不一致，请检查传入的退款金额是否正确。 或者通过退款查询接口获取退款执行结果。                                                                                            |
| ACQ.ENTERPRISE_PAY_BIZ_ERROR               | 因公付业务异常            | 如果提示“当前交易不含企业出资”，请确认交易是否包含企业出资，如果不包含则接口入参不能指定enterprise_pay_info参数，如果确认包含则联系支付宝核实。 如果提示“无效企业退款金额”，请检查指定的企业退款金额是否超过当前交易企业支付的金额。 其它情况，请联系支付宝小二。                         |
| ACQ.INVALID_PARAMETER                      | 参数无效               | 请根据接口返回的错误信息，检查请求参数，修改后重新发起请求                                                                                                                                         |
| ACQ.NOT_ALLOW_PARTIAL_REFUND               | 不支持部分退款            | 由于交易使用了特定的优惠券等场景，该笔交易不支持部分退款，请对交易进行全额退款或者联系买家进行线下退款处理                                                                                                                 |
| ACQ.ONLINE_TRADE_VOUCHER_NOT_ALLOW_REFUND  | 交易不允许退款            | 此交易中核销了购买的代金券，不允许进行退款，可联系买家进行线下退款处理                                                                                                                                   |
| ACQ.OVERDRAFT_AGREEMENT_NOT_MATCH          | 垫资退款接口传入模式和签约配置不一致 | 请检查垫资退款合约中的出资方式，修改合约或接口传参后重试                                                                                                                                          |
| ACQ.OVERDRAFT_ASSIGN_ACCOUNT_INVALID       | 垫资退款出资账号和商户信息不一致   | 垫资退款出资账号必须为商户名下支付宝账号，请更换出资账号后重试                                                                                                                                       |
| ACQ.REASON_TRADE_BEEN_FREEZEN              | 请求退款的交易被冻结         | 联系支付宝小二，确认该笔交易的具体情况                                                                                                                                                   |
| ACQ.REASON_TRADE_REFUND_FEE_ERR            | 退款金额无效             | 同一笔交易累计请求的退款金额不能大于交易总金额，请检查退款请求的金额是否正确。                                                                                                                               |
| ACQ.REASON_TRADE_STATUS_INVALID            | 交易状态异常             | 查询交易，确认交易是否是支付成功状态，是的话可联系支付宝小二确认交易状态                                                                                                                                  |
| ACQ.REFUNDALLOC_UNAUTH_LIMIT               | 分账接收方未开启分账回退，退分账失败 | 由于分账接收方未开启分账回退，退分账暂时无法调用，请联系分账接收方在 支付宝商家平台 > 资金管理 > 资金服务 > 分账接收 设置中，开启分账回退授权 后再发起调用                                                                                   |
| ACQ.REFUND_ACCOUNT_NOT_EXIST               | 退款出资账号不存在或账号异常     | 检查退款出资账号状态，账号正常后重试                                                                                                                                                    |
| ACQ.REFUND_AMT_NOT_EQUAL_TOTAL             | 退款金额超限             | 1、请检查退款金额是否正确，请求的退款金额不能大于交易总金额； 2、如果不是全额退款，退款请求号必填，请检查是否传入了退款请求号；                                                                                                     |
| ACQ.REFUND_CHARGE_ERROR                    | 退收费异常              | 请过一段时间后再重试发起退款                                                                                                                                                        |
| ACQ.REFUND_FEE_ERROR                       | 交易退款金额有误           | 请检查传入的退款金额是否正确                                                                                                                                                        |
| ACQ.REFUND_ROYALTY_PAYEE_ACCOUNT_NOT_EXIST | 退分账收入方账户不存在        | 退分账收入方账户不存在，请确认收入方账号是否正确，更换账号后重新发起                                                                                                                                    |
| ACQ.SELLER_BALANCE_NOT_ENOUGH              | 卖家余额不足             | 商户支付宝账户充值后重新发起退款即可                                                                                                                                                    |
| ACQ.SYSTEM_ERROR                           | 系统错误               | 接口返回系统错误情况下，当前请求的退款可能成功也可能失败。 1、请使用相同的参数再次重试调用，需要保证退款请求号和退款金额不能变更。如果前一次退款请求已经处理成功，接口会幂等返回成功；如果前一次退款请求没有成功，接口会重试执行退款操作； 2、或者通过退款查询接口查询退款执行结果，发起退款查询接口需要保证间隔退款请求大于5秒以上； |
| ACQ.TRADE_HAS_CLOSE                        | 交易已关闭              | 该交易已关闭，不能再进行退款，请确认请求退款的交易是否未支付或者已完成退款                                                                                                                                 |
| ACQ.TRADE_HAS_FINISHED                     | 交易已完结              | 该交易已完结（已超过退款期限），不允许进行退款（即使重试也无法成功），建议联系买家进行线下退款处理。                                                                                                                    |
| ACQ.TRADE_NOT_ALLOW_REFUND                 | 当前交易不允许退款          | 检查当前交易的状态是否为交易成功状态以及签约的退款属性是否允许退款，确认后，重新发起请求                                                                                                                          |
| ACQ.TRADE_NOT_EXIST                        | 交易不存在              | 检查请求中的交易号和商户订单号是否正确，确认后重新发起                                                                                                                                           |
| ACQ.TRADE_SETTLE_ERROR                     | 交易结算异常             | 请检查传入的退结算项信息是否正确，如果正确请联系支付宝小二                                                                                                                                         |
| ACQ.TRADE_STATUS_ERROR                     | 交易状态非法             | 查询交易，确认交易是否已经付款                                                                                                                                                       |
| ACQ.USER_NOT_MATCH_ERR                     | 交易用户不匹配            | 请联系支付宝小二处理                                                                                                                                                            |