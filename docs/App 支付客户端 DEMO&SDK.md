在接入 App 支付客户端 SDK 前，请先阅读 [App 支付产品介绍文档](https://opendocs.alipay.com/open/repo-0038v9)。

如果开发者使用的是老版本移动支付接口 `mobile.securitypay.pay`，请参考 [老版本移动支付文档](https://opendocs.alipay.com/pre-open/01x2n2)。

## DEMO&SDK 获取

集成前请仔细阅读 [App 支付客户端 SDK 隐私政策](https://opendocs.alipay.com/common/02kiq3)。

|   |   |   |
|---|---|---|
|**资源内容**|**资源版本**|**获取方式**|
|Android 支付 SDK 和示例 Demo|*|目前 SDK 已发布到 Maven Central，开发者可使用 gradle 编译、更新支付宝支付 SDK。<br><br>在 **build.gradle** 文件中，需添加如下依赖：<br><br>`dependencies {`<br><br>  `api 'com.alipay.sdk:alipaysdk-android:+@aar'`<br><br>`}`|
|点击下载 [示例 Demo](https://mdn.alipayobjects.com/portal_mdssth/afts/file/A*4QUWTImg-lUAAAAAgcAAAAgAegAAAQ/AlipaySDK-standard-15.8.42.1.zip)。|
|iOS 支付 SDK 和示例 Demo|15.8.42|点击下载 [SDK & 示例 Demo](https://mdn.alipayobjects.com/portal_mdssth/afts/file/A*4QUWTImg-lUAAAAAgcAAAAgAegAAAQ/AlipaySDK-standard-15.8.42.1.zip)。|
|鸿蒙单框架支付 SDK 和示例 Demo|*|SDK 已发布 OpenHarmony 三方库中心仓，开发者可使用 ohpm 直接安装依赖。详见 [cashiersdk](https://ohpm.openharmony.cn/#/cn/detail/@cashier_alipay%2Fcashiersdk)|
|点击下载 [示例 Demo](https://mdn.alipayobjects.com/portal_sre7ta/afts/file/A*20kpSZkaREMAAAAARwAAAAgAAQAAAQ/alipay-harmony-sdk-demo.zip)。|

## 注意

1. 接入后请对设备上 **已安装****支付宝 App** 和 **未安装****支付宝 App** 两种情况分别测试支付流程。
2. 支付参数签名过程和私钥 **必须在 App 服务端** 完成，示例项目中的签名流程仅供开发时参考。
3. iOS 构建时可能发生 utdid 库相关的冲突，此时请改用 [兼容版 SDK](https://mdn.alipayobjects.com/portal_mdssth/afts/file/A*FzwNQInCgyIAAAAAgCAAAAgAegAAAQ/AlipaySDK-noutdid-15.8.42.1.zip)。
4. 如果 iOS App 需要支持 arm64 架构模拟器上运行，使用 [XCFramework 格式的 SDK](https://mdn.alipayobjects.com/portal_mdssth/afts/file/A*rwRIQ5JEDSoAAAAAgGAAAAgAegAAAQ/AlipaySDK-XCFramework-15.8.42.1.zip)。
5. 如果 App 正在使用 **mPaaS 移动开发平台**，构建时可能发生内部类冲突，请查看 [mPaaS 技术文档](https://help.aliyun.com/document_detail/169806.html) 提供的冲突解决方案，接入 mPaaS 版本的支付 SDK。
6. 接入过程中如果遇到问题，请点击查看 [App 支付常见问题](https://opendocs.alipay.com/open/00dn7g)，或在 [文档中心](https://opendocs.alipay.com/home) 搜索。
7. 开发者可以根据需要，[下载](https://mdn.alipayobjects.com/portal_mdssth/afts/file/A*-6xGSrvPHioAAAAAAAAAAAAAAQAAAQ/PrivacyInfo.xcprivacy.zip) 解压 .xcprivacy 文件，放置到对应 SDK 的 framework 内。

## SDK版本更新说明

|   |   |   |
|---|---|---|
|**版本号**|**更新时间**|**更新内容**|
|15.8.42|2026-05-10|性能优化。|
|15.8.41|2026-05-10|性能优化。|
|15.8.40|2025-11-17|性能优化。|
|15.8.37|2025-08-14|权限和数据使用相关优化。<br><br>支付体验优化。|
|15.8.35|2025-06-12|支付体验优化，支付稳定性优化。|
|15.8.32|2025-03-03|支付体验优化。|
|15.8.30|2024-12-18|稳定性问题修复。|
|15.8.28|2024-11-06|代扣签约链路防护机制更新。<br><br>Xcode16 相关稳定性问题修复。|
|15.8.18|2024-04-10|安卓跳端文案新增多语言。|
|15.8.17|2023-12-05|iOS Framework 增加 Modules 支持。<br><br>Android 优化权限和数据使用相关功能。|
|15.8.16|2023-08-08|提升稳定性。<br><br>优化权限和数据使用相关功能。|
|15.8.15|2023-06-30|iOS 提供 XCFramework 格式的 SDK。<br><br>Android 沙箱环境能力优化。|
|15.8.14|2023-05-24|优化权限和数据使用相关功能。<br><br>iOS 支持 UniversalLink 回跳到商户 App<br><br>Android 沙箱环境能力优化。|
|15.8.11|2022-08-02|提升稳定性。<br><br>优化权限和数据使用相关功能。|
|15.8.10|2022-04-21|稳定性提升。<br><br>iOS 提升在 Apple Silicon(M1) 平台的稳定性|
|15.8.09|2022-03-23|权限和数据使用相关优化。<br><br>Android 由 aar 依赖改成 maven 依赖。|
|15.8.08|2022-02-21|iOS 支持 Apple Silicon(M1) 平台编译。|
|15.8.07|2022-02-09|权限和数据使用相关优化。<br><br>支付宝 App 唤起体验优化。|
|15.8.06|2021-11-29|稳定性提升。|
|15.8.05|2021-10-20|稳定性问题修复。|
|15.8.03|2021-04-29|跳转支付宝稳定性优化。|
|15.7.9|2020-07-27|iOS SDK<br><br>- 适配 iOS 14 系统。|
|15.7.3|2020-02-14|iOS SDK<br><br>- 支持 iOS Universal Link，提高支付稳定性。|
|15.6.8|2019-10-31|Android SDK<br><br>- 解决应用在 Google Play 上架时，可能收到“加密模式”警告的问题。<br><br>iOS SDK<br><br>- 按 App Store 规范，使用 WKWebView 替换 UIWebView。|
|15.6.3|2019-06-05|Android SDK<br><br>- Android O 兼容性优化。|