为方便商户快速查账，支持商户通过本接口获取商户离线账单下载地址

## 公共请求参数

## 业务请求参数

**bill_type**必选string(20)

【描述】账单类型，商户通过接口或商户经开放平台授权后其所属服务商通过接口可以获取以下账单类型。

【枚举值】

商户基于支付宝交易收单的业务账单: trade

基于商户支付宝余额收入及支出等资金变动的账务账单: signcustomer

营销活动账单，包含营销活动的发放，核销记录: merchant_act

更多

【示例值】trade

**bill_date**必选string(15)

【描述】账单时间：

* 日账单格式为yyyy-MM-dd，最早可下载近6年的日账单。不支持下载当日账单，只能下载前一日24点前的账单数据（T+1），当日数据一般于次日 9 点前生成，特殊情况可能延迟。

* 月账单格式为yyyy-MM，最早可下载近6年的月账单。不支持下载当月账单，只能下载上一月账单数据，当月账单一般在次月 3 日生成，特殊情况可能延迟。

* 当biz_type为settlementMerge时候，时间为汇总批次结算资金到账的日期，日期格式为yyyy-MM-dd，最早可下载2023年4月17日及以后的账单。

* 接口调用仅支持下载近6年的账单，更多账单请前往 b.alipay.com 对账中心-账单下载页下载。

【示例值】2025-05-01

**smid**可选string(20)

【描述】二级商户smid，这个参数只在bill_type是trade_zft_merchant时才能使用

【示例值】2088123412341234

**secure****｜是否需要使用安全链接**可选string(10)

【描述】true表示使用安全链接，即返回的下载链接为https。 非true值的情况下，统一为false，使用http链接。

【枚举值】

https链接: true

http链接: false

【示例值】true

常见请求示例

默认示例

cURLJavaC#PHP

```bash
curl 'https://openapi.alipay.com/gateway.do?charset=UTF-8&method=alipay.data.dataservice.bill.downloadurl.query&format=json&sign=${sign}&app_id=${appid}&version=1.0&sign_type=RSA2&timestamp=${now}' \
 -F 'app_auth_token=${app_auth_token}' \
 -F 'biz_content={
	"bill_type":"trade",
	"bill_date":"2025-05-01",
	"smid":"2088123412341234",
	"secure":"true"
}' 
```

说明：本示例仅供参考。

## 公共响应参数

## 业务响应参数

**bill_download_url**可选string(2048)

【描述】当账单可获取时，返回账单下载地址链接，获取链接后30秒后未下载，链接地址失效。

【示例值】http://dwbillcenter.alipay.com/downloadBillFile.resource?bizType=X&pid=X&fileType=X&bizDates=X&downloadFileName=X&fileId=X

**bill_file_code****｜账单文件结果说明**可选string(64)

【描述】描述本次申请的账单文件状态。 EMPTY_DATA_WITH_BILL_FILE：当天无账单业务数据&&可以获取到空数据账单文件。

【枚举值】

空账单数据文件：当前周期无数据时候产生的账单文件: EMPTY_DATA_WITH_BILL_FILE

【注意事项】目前仅对默认配置用户生效，主动配置的不会返回当前字段。

【示例值】EMPTY_DATA_WITH_BILL_FILE

响应示例

正常示例

异常示例

```json
{
    "alipay_data_dataservice_bill_downloadurl_query_response": {
        "code": "10000",
        "msg": "Success",
        "bill_download_url": "http://dwbillcenter.alipay.com/downloadBillFile.resource?bizType=X&pid=X&fileType=X&bizDates=X&downloadFileName=X&fileId=X",
        "bill_file_code": "EMPTY_DATA_WITH_BILL_FILE"
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
|BILL_DATE_BEFORE_REGISTRATION|请求的账单时间早于注册时间|请求的账单时间早于注册时间 确认参数后重新查询|
|BILL_NOT_EXIST|账单不存在|确认参数后重新查询|
|INVAILID_ARGUMENTS|入参不合法|确认参数后重新查询|
|NO_BILL_DATA|商户在请求的账单时间内没有发生当前账单类型的业务|确认参数后重新查询|
|SYSTEM_RATE_LIMIT|系统当前负载较高，请求被限流，请稍后重试|间隔1分钟后重试|
|TYPE_NOT_SUPPORTED|此账单类型不支持下载|请根据不同的 bill_type 传参，确定不同解决方案  <br>（1）bill_type = trade，需签约支付宝支付产品（支付产品范围详见：[支付宝产品大全](https://b.alipay.com/page/product-mall/all-product?mrchportalwebServer=https%3A%2F%2Fmrchportalweb.alipay.com#J_wysk)），且有对应产品产生的实际流水。  <br>（2）bill_type = signcustomer，非支付宝商家身份，建议先签约收钱码或经营码产品。  <br>（3）bill_type = merchant_act，一年内无营销动作，具体请参考：[营销账单使用](https://opendocs.alipay.com/b/03ae7i?pathHash=83ffda2d)  <br>（4）bill_type = trade_zft_merchant，请联系直付通平台商提供对应账单数据  <br>（5）bill_type = zft_acc，请先签约【互联网平台直付通】产品：[互联网平台直付通](https://b.alipay.com/page/product-mall/solution-detail?solutionCode=SP000001000000007155&sceneCode=PAYMENT)  <br>（6）bill_type = settlementMerge，请先签约【收款到银行账户】产品：[收款到银行账户](https://b.alipay.com/page/product-mall/product-detail/I1080300001000041457/ALL?)  <br>（7）其他，该账单类型不支持下载操作，请再次确认账单类型，以及所选日期。  <br>如有需求，请联系支付宝小二排查|
|UNKNOWN_ERROR|未知错误|稍后重试或联系小二排查问题|
|USER_RATE_LIMIT|调用频率超限|间隔1分钟后重试，查看 [调用频率限制](https://opendocs.alipay.com/open/01inen?pathHash=4fae1c5e) 中新增的2.4请求频次|