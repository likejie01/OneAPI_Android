# 从旧的 AAR 依赖更新为 Maven 依赖

支付宝 SDK 从 15.8.09 开始，依赖方式变为 maven，替代之前的 AAR 打包依赖，SDK 支付接口部分不变。

如果已经在应用中接入了之前的 `alipaySdk-xxx.aar` 包，请先按照以下步骤，移除旧的 AAR 配置：

1. 在项目中 App Module（而非整个项目）的 `build.gradle` 文件中，**移除对支付宝 SDK AAR 包的依赖**，示例代码：

```
dependencies {
    ... ...删除下方的依赖项
    compile files('libs/alipaysdk-15.8.08.220323151201.aar')
    ... ... 
}
```

2. 从 `libs` 目录 **删除旧的 alipaySdk-xxx.aar 文件**，如：  
    ![](https://cdn.nlark.com/yuque/0/2022/png/179989/1648432675735-192f9293-6afc-46fa-a4b6-8f4ca38287b7.png)

之后请按照以下的章节，为项目导入支付宝 SDK 依赖。

# 导入支付宝 SDK

以下内容可参考 alipay_demo 的实现。

1. 在主项目的 `build.gradle` 中，添加以下内容：**注意**：只有"**mavenCentral**"的仓库可以同步到依赖。如果发现获取不到依赖库，请确认下获取的链接是否有问题。可以尝试将mavenCentral() 放到所有依赖库的第一个来保证优先从这个仓库获取依赖。

```
allprojects {
    repositories {
        // 添加下方的内容
        mavenCentral()
        // ... jcenter() 等其它仓库
    }
}
```

2. 在 App Module 的 `build.gradle` 中，添加以下内容，将支付宝 SDK 作为项目依赖。

```
dependencies {
    // 添加下方的内容
    api 'com.alipay.sdk:alipaysdk-android:+@aar'
    // ... 其它依赖项
}
```

至此，支付宝 SDK 开发资源导入完成。

# 运行权限

为正常完成良好的支付流程体验，支付宝 SDK 需要使用以下权限：

```
android.permission.INTERNET
android.permission.ACCESS_NETWORK_STATE
android.permission.ACCESS_WIFI_STATE
```

开发者需要在 AndroidManifest 里配置以上 3 个权限，支付宝 SDK 在运行时需要进行网络连接，并在必要的时候判断网络连接的状态（4G/Wi-Fi）等来进行支付体验的优化。

# 商户appId注册

在支付前注册商户appId，支付宝主要用于“防黑产”等增加支付安全性逻辑，以及对支付体验会有较明显优化（加快唤起支付速度，对支付成功率有正向影响）

补充说明：考虑到商户改造成本，目前此接口非强依赖。如果不调用不会影响支付流程，但是基于appId的防黑产相关安全能力不会生效，且相关体验优化逻辑也不会生效。

```
@Override
 protected void onCreate(Bundle savedInstanceState) {
 super.onCreate(savedInstanceState);
 setContentView(R.layout.pay_main);

 AlipayApi.registerApp(this,APPID);
 }
```

## 建议调用时机

建议商户在“选择支付方式的支付订单页”就调用此方法，以保证在调用支付宝支付接口前体验优化策略可以完全生效。（没有生效也不会影响支付链路）

商户可以通过使用 [Android 支付sdk调试功能](https://opendocs.alipay.com/open/0hcvna?referPath=00dn75_22ed0058) 来确认链路是否命中体验优化规则

# 支付接口调用

需要在新线程中调用支付接口（可参考 [alipay_demo](https://opendocs.alipay.com/common/02km9l) 实现）。

PayTask 对象主要为商家提供订单支付、查询功能，及获取当前开发包版本号。获取 PayTask 支付对象调用支付（支付行为需要在独立的非 ui 线程中执行）。

## 示例代码

```
final String orderInfo = info;   // 订单信息
 Runnable payRunnable = new Runnable() {
 @Override
 public void run() {
 PayTask alipay = new PayTask(DemoActivity.this);
               Map <String,String> result = alipay.payV2(orderInfo,true);
 Message msg = new Message();
 msg.what = SDK_PAY_FLAG;
 msg.obj = result;
 mHandler.sendMessage(msg);
 }
 };
      // 必须异步调用
 Thread payThread = new Thread(payRunnable);
 payThread.start();
```

|   |   |
|---|---|
|**参数名称**|**参数说明**|
|String orderInfo|App 支付请求参数字符串，主要包含商家的订单信息，key=value 形式，以 & 连接。|
|boolean isShowPayLoading|用户在商家 App 内部点击付款，是否需要一个 loading 做为在支付宝客户端唤起之前的过渡，这个值设置为 true，将会在调用 pay 接口的时候直接唤起一个 loading，直到唤起 H5 支付页面或者唤起外部的支付宝客户端付款页面 loading 才消失。建议将该值设置为 true，优化点击付款到支付唤起支付页面的过渡过程。|

orderInfo 示例如下，参数说明可查看 [请求参数说明](https://opendocs.alipay.com/open/00dn77?referPath=00dn75_22ed0058)，orderInfo 的获取必须来源于服务端：

```
app_id=2015052600090779&biz_content=%7B%22timeout_express%22%3A%2230m%22%2C%22seller_id%22%3A%22%22%2C%22product_code%22%3A%22QUICK_MSECURITY_PAY%22%2C%22total_amount%22%3A%220.02%22%2C%22subject%22%3A%221%22%2C%22body%22%3A%22%E6%88%91%E6%98%AF%E6%B5%8B%E8%AF%95%E6%95%B0%E6%8D%AE%22%2C%22out_trade_no%22%3A%22314VYGIAGG7ZOYY%22%7D&charset=utf-8&method=alipay.trade.app.pay&sign_type=RSA2&timestamp=2016-08-15%2012%3A12%3A15&version=1.0&sign=MsbylYkCzlfYLy9PeRwUUIg9nZPeN9SfXPNavUCroGKR5Kqvx0nEnd3eRmKxJuthNUx4ERCXe552EV9PfwexqW%2B1wbKOdYtDIb4%2B7PL3Pc94RZL0zKaWcaY3tSL89%2FuAVUsQuFqEJdhIukuKygrXucvejOUgTCfoUdwTi7z%2BZzQ%3D
```

**返回值：** 本方法调用的返回结果，参数说明可查看 [同步通知说明](https://opendocs.alipay.com/open/00iki4?referPath=00dn75_22ed0058)。

# 支付结果获取和处理

调用 pay 方法支付后，将通过2种途径获得支付结果：

## 同步返回

商家应用客户端通过当前调用支付的 Activity 的 Handler 对象，通过它的回调函数获取支付结果。（可参考 [alipay_demo](https://opendocs.alipay.com/open/04km1h) 实现）

### 示例代码

客户端返回回调信息详见 [同步通知说明](https://opendocs.alipay.com/open/00iki4?referPath=00dn75_22ed0058)。

```
private Handler mHandler = new Handler() {
 public void handleMessage(Message msg) {
 Result result = new Result((String) msg.obj);
 Toast.makeText(DemoActivity.this, result.getResult(),
 Toast.LENGTH_LONG).show();
 };
 };
```

## 异步通知

商家需要提供一个 HTTP 协议的接口，包含在请求支付的入参中，其 key 对应 notify_url。支付宝服务器在支付完成后，会以 POST 方式调用 notify_url 传输数据。

# 获取当前开发包版本号

调用 PayTask 对象的 getVersion() 方法查询。

## 示例代码

```
PayTask payTask = new PayTask(activity);
String version = payTask.getVersion();
```

# 联调问题排查

联调过程中的问题可查看 [常见问题](https://opendocs.alipay.com/open/00dn7g?referPath=00dn75_22ed0058)。