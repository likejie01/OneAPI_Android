# OneAPI Android PC Linkage Roadmap

## Phase 0: 对齐基线

目标：Android 现有模块与 PC 当前版本保持一致，Codex/Claude 进入移动联动设计。

交付：

- 确认 Android 登录、Chat、Image、Assistants、Subscriptions、Wallet、Usage、Me 的接口与 PC 一致。
- 复用 PC 当前模型过滤规则，确保 DeepSeek/Mimo 在 Codex/Claude 可见。
- 定义移动端会话、job、event、device binding 数据结构。

验收：

- Android 可登录同一账号。
- 可拉取 `/api/pricing` 并按 Codex/Claude 规则过滤模型。
- 设备绑定页能显示空状态。

## Phase 1: 设备绑定

目标：用户可以在 Android 端选择并绑定 PC/Mac 执行设备。

交付：

- Me 页面新增设备绑定卡片。
- 设备列表、在线状态、绑定/解绑/切换。
- 本地保存当前绑定设备 ID。
- 绑定设备离线时 Codex/Claude 输入区禁用。

验收：

- 同一账号下 PC/Mac 上线后，Android 可看到设备。
- 切换设备后，新 job 发往新设备。
- 解绑后 Codex/Claude 发送入口要求重新绑定。

## Phase 2: 移动端 Codex/Claude 会话

目标：Android 能创建 Codex/Claude job，并展示服务器同步回来的执行日志和回复。

交付：

- Codex/Claude 工作区 UI。
- 会话列表与会话详情。
- 发送消息创建 job。
- 轮询或 SSE 获取 job events。
- 时间线组件按 PC 的线性日志规则呈现。

验收：

- Android 发送任务后，PC/Mac 能 claim 并执行。
- Android 能看到执行日志、AI 回复、失败状态、停止状态。
- 日志不重复父级内容，不显示空白点。

## Phase 3: 交互确认

目标：Android 可处理桌面端 CLI 的权限确认和人工确认。

交付：

- 确认条 UI。
- 允许、拒绝、总是允许本次会话。
- interaction response 提交和 ack 状态。
- 超时、设备离线、已处理冲突提示。

验收：

- 桌面端等待确认时 Android 出现确认条。
- Android 点击允许后桌面端继续执行。
- Android 点击拒绝后桌面端结束或按 CLI 结果失败。

## Phase 4: 稳定性与离线体验

目标：移动端联动在弱网、后台、长任务中稳定。

交付：

- 当前会话事件本地缓存。
- 断网重连后按 `last_event_id` 补拉。
- Android 后台通知：job 完成、失败、等待确认。
- 日志列表虚拟滚动。

验收：

- 断网恢复后日志无重复、无丢失。
- 后台收到等待确认通知。
- 500 条日志内滚动稳定。

## Phase 5: 发布前验证

目标：端到端质量达到可交付。

交付：

- 设备绑定 E2E。
- Codex/Claude 成功、失败、停止、确认、离线场景测试。
- 与 PC/Mac 当前版本的 UI 文案和状态名对齐。
- 灰度发布包。

验收：

- 关键路径 0 阻塞 bug。
- Codex/Claude 移动联动链路连续运行 30 分钟无状态错乱。
- 用户从 Android 发起任务，PC/Mac 执行，Android 查看结果的主路径可稳定复现。
