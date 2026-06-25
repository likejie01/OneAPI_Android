## 接口说明

该接口是 签名数据准备接口，用于生成可信签名字符串（orderStr）。可信签名串中包含业务参数及商户身份信息，可防止数据被篡改，一般用于打开支付宝客户端。请在服务端执行支付宝SDK中sdkExecute方法，读取响应中的body()结果。具体使用方法请参考 [接入指南](https://opendocs.alipay.com/open/01dcc0?pathHash=cf89b2be)

APP支付

外部商户APP唤起快捷SDK创建订单并支付

## 公共请求参数

## 业务请求参数

展开所有属性

**out_trade_no**必选string(64)

【描述】商户网站唯一订单号。  
由商家自定义，64个字符以内，仅支持字母、数字、下划线且需保证在商户端不重复。

【示例值】70501111111S001111119

**total_amount**必选string(9)

【描述】订单总金额，单位为元，精确到小数点后两位，取值范围[0.01,100000000]，金额不能为0

【示例值】9.00

**subject**必选string(256)

【描述】订单标题。  
注意：256个字符以内，不可使用特殊字符，如 /，=，& 等。

【示例值】大乐透

**product_code**可选string(64)

【描述】销售产品码，商家和支付宝签约的产品码

【示例值】QUICK_MSECURITY_PAY

**goods_detail**可选GoodsDetail[]

【描述】订单包含的商品列表信息，json格式，其它说明详见商品明细说明

**time_expire**可选string(32)

【描述】绝对超时时间，格式为yyyy-MM-dd HH:mm:ss

【示例值】2016-12-31 10:05:00

**extend_params**可选ExtendParams

【描述】业务扩展参数

**passback_params**可选string(512)

【描述】公用回传参数。如果请求时传递了该参数，则会在支付结果异步通知中将该参数原样返回。本参数必须进行UrlEncode之后才可以发送给支付宝。

【示例值】merchantBizType%3d3C%26merchantBizNo%3d2016010101111

**merchant_order_no**可选string(32)

【描述】商户原始订单号，最大长度限制32位

【示例值】20161008001

**ext_user_info**可选ExtUserInfo

【描述】外部指定买家

**query_options**可选string[](256)

【描述】返回参数选项。 商户通过传递该参数来定制同步需要额外返回的信息字段，数组格式。包括但不限于：["hyb_amount","enterprise_pay_info"]

【枚举值】

惠营宝回票金额: hyb_amount

因公付支付信息: enterprise_pay_info

医保信息: medical_insurance_info

【示例值】["hyb_amount","enterprise_pay_info"]

常见请求示例

默认示例

JavaC#PHPNode.js

```java
package com.java.sdk.demo;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.domain.ExtUserInfo;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.domain.ExtendParams;
import com.alipay.api.domain.GoodsDetail;

import com.alipay.api.FileItem;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

public class AlipayTradeAppPay {

    public static void main(String[] args) throws AlipayApiException {
        // 初始化SDK
        AlipayClient alipayClient = new DefaultAlipayClient(getAlipayConfig());

        // 构造请求参数以调用接口
        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        
        // 设置商户订单号
        model.setOutTradeNo("70501111111S001111119");
        
        // 设置订单总金额
        model.setTotalAmount("9.00");
        
        // 设置订单标题
        model.setSubject("大乐透");
        
        // 设置产品码
        model.setProductCode("QUICK_MSECURITY_PAY");
        
        // 设置订单包含的商品列表信息
        List<GoodsDetail> goodsDetail = new ArrayList<GoodsDetail>();
        GoodsDetail goodsDetail0 = new GoodsDetail();
        goodsDetail0.setGoodsName("ipad");
        goodsDetail0.setAlipayGoodsId("20010001");
        goodsDetail0.setQuantity(1L);
        goodsDetail0.setPrice("2000");
        goodsDetail0.setGoodsId("apple-01");
        goodsDetail0.setGoodsCategory("34543238");
        goodsDetail0.setCategoriesTree("124868003|126232002|126252004");
        goodsDetail0.setShowUrl("http://www.alipay.com/xxx.jpg");
        goodsDetail.add(goodsDetail0);
        model.setGoodsDetail(goodsDetail);
        
        // 设置订单绝对超时时间
        model.setTimeExpire("2016-12-31 10:05:00");
        
        // 设置业务扩展参数
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088511833207846");
        extendParams.setHbFqSellerPercent("100");
        extendParams.setHbFqNum("3");
        extendParams.setIndustryRefluxInfo("{\"scene_code\":\"metro_tradeorder\",\"channel\":\"xxxx\",\"scene_data\":{\"asset_name\":\"ALIPAY\"}}");
        extendParams.setRoyaltyFreeze("true");
        extendParams.setCardType("S0JP0000");
        model.setExtendParams(extendParams);
        
        // 设置公用回传参数
        model.setPassbackParams("merchantBizType%3d3C%26merchantBizNo%3d2016010101111");
        
        // 设置商户的原始订单号
        model.setMerchantOrderNo("20161008001");
        
        // 设置外部指定买家
        ExtUserInfo extUserInfo = new ExtUserInfo();
        extUserInfo.setCertType("IDENTITY_CARD");
        extUserInfo.setCertNo("362334768769238881");
        extUserInfo.setMobile("16587658765");
        extUserInfo.setName("李明");
        extUserInfo.setMinAge("18");
        extUserInfo.setNeedCheckInfo("F");
        extUserInfo.setIdentityHash("27bfcd1dee4f22c8fe8a2374af9b660419d1361b1c207e9b41a754a113f38fcc");
        model.setExtUserInfo(extUserInfo);
        
        // 设置通知参数选项
        List<String> queryOptions = new ArrayList<String>();
        queryOptions.add("hyb_amount");
        queryOptions.add("enterprise_pay_info");
        model.setQueryOptions(queryOptions);
        
        request.setBizModel(model);
        // 第三方代调用模式下请设置app_auth_token
        // request.putOtherTextParam("app_auth_token", "<-- 请填写应用授权令牌 -->");

        AlipayTradeAppPayResponse response = alipayClient.sdkExecute(request);
        String orderStr = response.getBody();
        System.out.println(orderStr);

        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
            // sdk版本是"4.38.0.ALL"及以上,可以参考下面的示例获取诊断链接
            // String diagnosisUrl = DiagnosisUtils.getDiagnosisUrl(response);
            // System.out.println(diagnosisUrl);
        }
    }

    private static AlipayConfig getAlipayConfig() {
        String privateKey  = "<-- 请填写您的应用私钥，例如：MIIEvQIBADANB ... ... -->";
        String alipayPublicKey = "<-- 请填写您的支付宝公钥，例如：MIIBIjANBg... -->";
        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl("https://openapi.alipay.com/gateway.do");
        alipayConfig.setAppId("<-- 请填写您的AppId，例如：2019091767145019 -->");
        alipayConfig.setPrivateKey(privateKey);
        alipayConfig.setFormat("json");
        alipayConfig.setAlipayPublicKey(alipayPublicKey);
        alipayConfig.setCharset("UTF-8");
        alipayConfig.setSignType("RSA2");
        return alipayConfig;
    }
}
```

说明：本示例仅供参考。

## 公共响应参数

无公共响应参数

## 业务响应参数

**orderStr****｜签名字符串**必选string(16384)

【描述】获取签名后的业务数据具体使用方法请参考 [接入指南](https://opendocs.alipay.com/open/01dcc0?pathHash=cf89b2be)

【示例值】请参考响应示例

响应示例

正常示例

```json
app_id=2017060101317939&biz_content=%7B%22time_expire%22%3A%222016-12-31+10%3A05%3A00%22%2C%22extend_params%22%3A%22%22%2C%22query_options%22%3A%22%5B%5C%22hyb_amount%5C%22%2C%5C%22enterprise_pay_info%5C%22%5D%22%2C%22subject%22%3A%22%E5%A4%A7%E4%B9%90%E9%80%8F%22%2C%22product_code%22%3A%22QUICK_MSECURITY_PAY%22%2C%22body%22%3A%22Iphone6+16G%22%2C%22passback_params%22%3A%22merchantBizType%253d3C%2526merchantBizNo%253d2016010101111%22%2C%22specified_channel%22%3A%22pcredit%22%2C%22goods_detail%22%3A%22%22%2C%22merchant_order_no%22%3A%2220161008001%22%2C%22enable_pay_channels%22%3A%22pcredit%2CmoneyFund%2CdebitCardExpress%22%2C%22out_trade_no%22%3A%2270501111111S001111119%22%2C%22ext_user_info%22%3A%22%22%2C%22total_amount%22%3A%229.00%22%2C%22timeout_express%22%3A%2290m%22%2C%22disable_pay_channels%22%3A%22pcredit%2CmoneyFund%2CdebitCardExpress%22%2C%22agreement_sign_params%22%3A%22%22%7D&charset=UTF-8&format=json&method=alipay.trade.app.pay&sign=ERITJKEIJKJHKKKKKKKHJEREEEEEEEEEEE&sign_type=RSA2&timestamp=2014-07-24+03%3A07%3A50&version=1.0
```

说明：本示例仅供参考。

## 公共错误码

[前往查看](https://opendoc.alipay.com/common/02km9f)

## 业务错误码

|错误码|错误描述|解决方案|
|---|---|---|
|ACQ.ACCESS_FORBIDDEN|无权限使用接口|联系支付宝小二签约|
|ACQ.BUYER_ENABLE_STATUS_FORBID|买家状态非法|用户联系支付宝小二，确认买家状态为什么非法|
|ACQ.BUYER_SELLER_EQUAL|买卖家不能相同|更换买家重新付款|
|ACQ.CONTEXT_INCONSISTENT|交易信息被篡改|确认该笔交易信息是否为当前买家的已存订单，如果是则认为本次请求参数与已存交易不一致，如果不是则更换商家订单号后，重新发起请求|
|ACQ.EXIST_FORBIDDEN_WORD|订单信息中包含违禁词|修改订单信息后，重新发起请求|
|ACQ.INVALID_PARAMETER|参数无效|若存在参数无效具体错误信息描述，请参考描述检查请求参数后，重新发起|
|ACQ.PARTNER_ERROR|应用APP_ID填写错误|联系支付宝小二，确认APP_ID的状态|
|ACQ.RISK_MERCHANT_IP_NOT_EXIST|当前交易未传入IP信息，创单失败，请传入IP后再发起支付|检查请求参数是否已经传入用户IP信息|
|ACQ.SELLER_BEEN_BLOCKED|商家账号被冻结|联系支付宝小二，解冻账号|
|ACQ.SYSTEM_ERROR|接口返回错误|请立即调用查询订单API，查询当前订单的状态，并根据订单状态决定下一步的操作|
|ACQ.TOTAL_FEE_EXCEED|订单总金额不在允许范围内|修改订单金额再发起请求|
|ACQ.TRADE_BUYER_NOT_MATCH|交易买家不匹配|该笔交易已经在支付宝端创建，但请求买家与已存交易中的买家不一致。请商户确认本次请求是否与已存交易有关，若为同一笔交易，则只能用原始买家付款，若无关更换商家订单号后，重新发起请求|
|ACQ.TRADE_HAS_CLOSE|交易已经关闭|确认该笔交易信息是否为当前买家的已存订单，如果是则认为交易已经关闭，如果不是则更换商家订单号后，重新发起请求|
|ACQ.TRADE_HAS_SUCCESS|交易已被支付|确认该笔交易信息是否为当前买家的，如果是则认为交易付款成功，如果不是则更换商家订单号后，重新发起请求|

## 触发通知类型

|通知类型|描述|默认开启|
|---|---|---|
|tradeStatus.TRADE_CLOSED|交易关闭|1|
|tradeStatus.TRADE_FINISHED|交易完结|1|
|tradeStatus.TRADE_SUCCESS|支付成功|1|
|tradeStatus.WAIT_BUYER_PAY|交易创建|0|

## 触发通知示例

```http
https://www.merchant.com/receive_notify.htm?notify_type=trade_status_sync&notify_id=91722adff935e8cfa58b3aabf4dead6ibe&notify_time=2017-02-16 21:46:15&sign_type=RSA2&sign=WcO+t3D8Kg71
```