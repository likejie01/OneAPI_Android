# OneAPI Android PC Linkage Spec

## 目标

Android 端保持现有 OneAPI 客户端的账号、聊天、绘图、助手、订阅、钱包、用量、我的等能力一致；Codex 和 Claude 不在手机本地执行 CLI，而是把用户消息发送到服务器，由已绑定的 PC 或 Mac 客户端拉取任务、执行 CLI、同步执行日志和 AI 回复回服务器，Android 端实时呈现同一条会话。

## 角色与设备

- Android App：移动控制端，负责登录、设备绑定、发送 Codex/Claude 任务、展示执行日志、回复、确认弹窗和历史。
- PC/Mac 客户端：执行端，负责注册设备、轮询任务、执行 Codex/Claude CLI、上传执行日志、AI 回复、交互确认结果。
- Server：账号与同步中枢，保存设备、绑定关系、任务、事件流、会话快照、交互确认状态。

## 导航结构

- 底部导航保持桌面端主要模块一致：Chat、Image、Assistants、Subscriptions、Wallet、Usage、Me。
- Codex 和 Claude 入口在主工作区顶部使用分段控件呈现：`Chat | Image | Codex | Claude`。
- Me 页面新增“设备绑定”区块，只展示当前账号下 PC/Mac 设备，不影响已有账号、钱包、订阅信息。

## 登录与账号

- 启动未登录：显示与当前桌面端一致的登录/注册页，包含服务器地址、账号、密码、登录按钮。
- 登录成功：拉取用户资料、可用模型、设备列表、最近会话。
- 登录失效：清空本地 token，回到登录页；保留最近一次服务器地址。

## 设备绑定

### 设备列表

布局：

- 顶部标题：`绑定设备`
- 右侧操作：刷新图标按钮
- 设备卡片字段：设备名称、平台标签 `Windows/macOS`、在线状态、最近心跳、当前执行状态、已启用能力 `Codex/Claude`
- 主操作：`绑定` / `切换` / `解绑`

状态：

- 在线：绿色点，最近心跳小于 45 秒。
- 离线：灰色点，最近心跳超过 45 秒。
- 忙碌：蓝色点，设备存在执行中的 job。
- 异常：红色点，设备上报 `degraded` 或最近 job 失败。

交互：

- 首次进入 Codex/Claude 且未绑定设备：弹出设备选择抽屉。
- 当前绑定设备离线：输入框禁用，提示“绑定设备离线，无法执行 CLI 任务”。
- 切换设备时：保留会话历史，但新任务发往新设备。

## Codex/Claude 移动工作区

### 页面布局

- 顶部栏：返回/模块标题、绑定设备状态、更多菜单。
- 会话区：与 PC 时间线一致，按时间显示用户消息、执行日志组、AI 回复。
- 执行日志组：标题、状态徽标、折叠/展开按钮、日志事件列表。
- 输入区：多行输入框、附件入口、模型选择、权限模式、发送按钮。

### 模型选择

- Codex 显示支持 `openai-response` / `openai-response-compact` 的模型，以及兼容矩阵内的 DeepSeek/Mimo。
- Claude 显示支持 `anthropic` 的模型，以及兼容矩阵内的 DeepSeek/Mimo。
- 过滤标签：全部、OpenAI、Claude、DeepSeek、XiaomiMIMO。
- 默认模型与桌面端一致：Codex `gpt-5.4`，Claude `claude-sonnet-4-6`。

### 权限模式

- Android 端只发送执行意图，不直接读写文件。
- 权限选择只作为 job 参数传给 PC/Mac。
- 初版仅开放“受限模式”和“全权限模式”两个选项；实际读写行为由桌面端执行。
- 受限模式：默认使用绑定设备当前项目目录。
- 全权限模式：允许桌面端按用户要求访问任意路径。

### 发送任务

字段：

- `client`: `codex` 或 `claude`
- `device_id`: 当前绑定设备
- `session_id`: 移动端当前会话
- `prompt`: 用户输入原文
- `model`: 选择模型
- `reasoning_effort`: 推理强度
- `permission_mode`: 受限/全权限
- `extension_refs`: 选中的命令、技能、插件

发送后：

- Android 立即插入用户消息。
- 创建 pending job。
- 时间线显示第一条日志：“已发送到绑定设备，等待执行”。
- 设备 claim 后状态变为“执行中”。

## 执行日志呈现

日志必须和 PC 当前体验一致：

- 按时序展示，不用“执行具体内容”固定区块。
- 保留 AI 描述要做什么的 intent 日志。
- 执行日志状态支持：进行中、已完成、已停止、等待确认、执行失败。
- 子级日志不重复父级标题。
- 没有内容时不显示空白点。
- Codex 日志按 Claude 的解析方式做线性事件呈现。

事件字段：

- `event_id`
- `job_id`
- `session_id`
- `client`
- `type`: `status | output | error | interaction | assistant | file_change`
- `phase`: `queued | claimed | preparing | running | waiting_input | completed | failed | stopped`
- `title`
- `content`
- `detail`
- `assistant_chunk`
- `file_changes`
- `created_at`

## 交互确认

当桌面端 CLI 触发确认：

- Android 显示底部确认条，不打断会话滚动。
- 内容包括确认标题、风险描述、命令/路径摘要。
- 操作：允许、拒绝、总是允许本次会话。
- 操作结果写入 `desktop-interactions`，桌面端轮询并继续执行。

## 历史与同步

- Android 最近会话列表按 `updated_at` 倒序。
- 同一 session 可在 PC/Mac/Android 同步查看。
- Android 端不直接修改桌面本地 CLI 历史文件；所有同步以服务器 job events 为准。
- 网络断开后，Android 缓存最后 50 条会话摘要和当前会话事件。

## 错误处理

- 设备离线：禁止发送，允许查看历史。
- 设备执行失败：日志组显示执行失败，保留错误详情。
- 服务器超时：Android 显示“服务器暂时无响应”，任务不重复发送。
- 桌面端停止：状态显示已停止，并同步停止日志。
- 模型不可用：发送前拦截并提示“当前服务器没有可用的 Codex/Claude 模型”。

## 后端接口草案

- `GET /api/mobile/desktop-devices`：获取账号下设备。
- `POST /api/mobile/desktop-bindings`：绑定设备。
- `DELETE /api/mobile/desktop-bindings/{device_id}`：解绑。
- `POST /api/mobile/desktop-jobs`：创建 Codex/Claude job。
- `GET /api/mobile/desktop-jobs/{job_id}`：读取 job 状态。
- `GET /api/mobile/desktop-jobs/{job_id}/events`：读取事件列表。
- `POST /api/mobile/desktop-interactions/{interaction_id}/responses`：提交确认操作。
- `GET /api/mobile/sessions`：移动端会话列表。
- `GET /api/mobile/sessions/{session_id}`：移动端会话详情。

## 桌面端已存在对接点

当前 PC/Mac 客户端执行端应使用以下服务器路径：

- `POST /api/mobile/desktop-devices/register`
- `POST /api/mobile/desktop-devices/{device_id}/heartbeat`
- `POST /api/mobile/desktop-extensions/snapshot`
- `GET /api/mobile/desktop-jobs/pending`
- `POST /api/mobile/desktop-jobs/{job_id}/claim`
- `POST /api/mobile/desktop-jobs/{job_id}/events`
- `GET /api/mobile/desktop-interactions/pending`
- `POST /api/mobile/desktop-interactions/{response_id}/ack`
