# OneAPI Android

**简体中文** | [English](README.en.md)

OneAPI Android 是 [ai.oneapi.center](https://ai.oneapi.center) 的原生 Android 客户端，将移动端 AI 对话、图片生成、Codex/Claude 桌面协同、钱包充值、套餐订阅、服务状态与合规中心整合到一个轻量工作台中。它面向需要在手机上持续处理 AI 任务、查看额度消耗、接管桌面执行状态和管理多模型能力的用户。

## 平台价值

[ai.oneapi.center](https://ai.oneapi.center) 提供统一账号、模型路由、额度计费、支付充值、内容安全与多端同步能力。Android 客户端负责把这些能力带到移动端：

- **随时对话**：支持 Chat 会话、助手切换、模型选择、语音输入、附件和本地会话持久化。
- **图片工作流**：接入平台图片生成与编辑能力，适合移动端快速生成海报、头像、素材和创意草图。
- **桌面协同**：与 PC 客户端联动，在手机上向 Codex 或 Claude 发送任务，查看执行日志、工具输出和项目状态。
- **账户与支付**：内置钱包、额度消耗、最近账单和支付宝充值入口，充值金额与平台客户端保持一致。
- **稳定与合规**：内置服务状态、套餐订阅、版本更新、设备绑定、用户协议、隐私政策、生成式 AI 服务说明和内容安全规则。

## 功能预览

| Chat 会话 | 会话管理 | 模型目录 |
| --- | --- | --- |
| <img src="images/微信图片_20260627182421_530_3.jpg" alt="Chat conversation" width="220"> | <img src="images/微信图片_20260627182418_527_3.jpg" alt="Chat sessions" width="220"> | <img src="images/微信图片_20260627182419_528_3.jpg" alt="Model catalog" width="220"> |

| 助手选择 | Codex 协同 | Claude 协同 |
| --- | --- | --- |
| <img src="images/微信图片_20260627182420_529_3.jpg" alt="Assistant catalog" width="220"> | <img src="images/微信图片_20260627182415_524_3.jpg" alt="Codex remote task" width="220"> | <img src="images/微信图片_20260627182415_523_3.jpg" alt="Claude remote task" width="220"> |

| 钱包充值 | 系统设置 | 登录入口 |
| --- | --- | --- |
| <img src="images/微信图片_20260627182417_526_3.jpg" alt="Wallet and Alipay recharge" width="220"> | <img src="images/微信图片_20260627182421_531_3.jpg" alt="System settings" width="220"> | <img src="images/微信图片_20260627182422_532_3.jpg" alt="Login dialog" width="220"> |

## 核心能力

### AI 对话与图片

- 支持 Chat、Image、Codex、Claude、系统设置、钱包、服务状态、套餐订阅等主入口。
- Chat 支持多助手、多模型、历史会话、token 统计、附件、语音输入和本地优先缓存。
- Image 支持平台图片生成与编辑接口，结合不同助手和质量参数组织移动端创作流程。

### Codex / Claude 桌面协同

- Android 端可绑定桌面设备，并通过平台同步项目、最近任务、执行状态和工具输出。
- 手机端可向 Codex 或 Claude 发送任务，桌面端执行，移动端查看进度。
- 最近会话使用本地缓存和增量同步策略，优先保证项目列表、执行状态和高优先级业务请求。

### 账户、额度与支付

- 钱包页展示剩余额度、已用额度、请求数、最近账单与消耗分布。
- 支付宝充值由服务端创建订单，Android 端通过支付宝 SDK 拉起支付，支付结果再由服务端查询确认。
- 当前客户端充值金额固定为 50、100、200、500 元，避免自定义金额造成三端不一致。

### 合规与安全

- 首次登录需要确认用户协议和隐私政策，未同意无法进入 App。
- 系统设置内提供关于与合规入口，集中展示用户协议、隐私政策、生成式 AI 服务说明和内容安全规则。
- 新建会话时提供安全与隐私提醒，并支持用户选择后续不再提示。

## 与 ai.oneapi.center 的关系

Android App 默认连接：

```text
https://ai.oneapi.center
```

平台负责账号、认证、模型目录、API Key 中转、额度计费、套餐订阅、服务状态、支付订单、内容安全和跨端同步。Android 客户端不直接替代服务端能力，所有关键账户与支付请求都应通过 ai.oneapi.center 的服务端接口完成。

## 从源码构建

### 环境要求

- JDK 17
- Android SDK 35
- Gradle 8.10.2 或与项目兼容的 Gradle 版本
- Android Gradle Plugin 8.7.2
- Android 8.0+ 设备或模拟器

### 构建命令

```powershell
cd D:\WorkSpace\NewAPI\OneAPI_Android
gradle.bat --no-daemon assembleRelease
```

默认 release 输出：

```text
app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

当前配置仅生成 `arm64-v8a` APK，应用包名为 `center.oneapi.mobile`。

## 项目结构

```text
app/src/main/java/center/oneapi/mobile/
  core/                 基础 API 客户端与偏好设置
  data/                 Room 会话数据
  features/             账单、桌面协同、图片、Key、服务状态等业务模块
  navigation/           主功能入口定义
  ui/                   通用 UI、输入区、会话列表与消息渲染
docs/                   支付宝与平台接入文档
images/                 README 截图
```

## 配置与安全

- 不要提交真实签名证书、生产密钥、服务器凭据或本地环境文件。
- 默认服务地址在 `AppPrefs.DEFAULT_SERVER` 中配置。
- `server.env`、支付密钥、MinIO 凭据等敏感信息应仅保存在受控部署环境中。

## 许可证

本项目随仓库根目录 `LICENSE` 文件授权发布。使用、二次开发和分发前请确认平台服务协议、模型供应商条款及适用法律法规要求。
