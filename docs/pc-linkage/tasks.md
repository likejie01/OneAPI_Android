# OneAPI Android PC Linkage Tasks

## 1. 数据模型

- [ ] 新增 `DesktopDevice`：`id`、`name`、`platform`、`status`、`lastHeartbeatAt`、`capabilities`、`currentJobId`。
- [ ] 新增 `MobileBinding`：`userId`、`deviceId`、`createdAt`、`lastUsedAt`。
- [ ] 新增 `DesktopJob`：`jobId`、`client`、`deviceId`、`sessionId`、`prompt`、`model`、`reasoningEffort`、`permissionMode`、`status`。
- [ ] 新增 `DesktopJobEvent`：日志、输出、AI 回复、错误、交互确认、文件变更统一事件。
- [ ] 新增 `DesktopInteractionResponse`：`interactionId`、`jobId`、`action`、`createdAt`、`ackedAt`。

## 2. API 客户端

- [ ] 封装设备列表接口：`GET /api/mobile/desktop-devices`。
- [ ] 封装绑定接口：`POST /api/mobile/desktop-bindings`。
- [ ] 封装解绑接口：`DELETE /api/mobile/desktop-bindings/{device_id}`。
- [ ] 封装创建 job：`POST /api/mobile/desktop-jobs`。
- [ ] 封装 job 详情：`GET /api/mobile/desktop-jobs/{job_id}`。
- [ ] 封装事件流：`GET /api/mobile/desktop-jobs/{job_id}/events`。
- [ ] 封装交互确认：`POST /api/mobile/desktop-interactions/{interaction_id}/responses`。
- [ ] 所有接口复用现有登录 token 和服务器地址配置。

## 3. 设备绑定页面

- [ ] 在 Me 页面新增“设备绑定”区块。
- [ ] 实现设备卡片：平台、在线状态、最近心跳、能力标签。
- [ ] 实现绑定、解绑、切换按钮。
- [ ] 实现空状态：提示用户打开 PC/Mac 客户端并登录同一账号。
- [ ] 实现离线状态：禁止选择离线设备作为新任务执行端。

## 4. Codex/Claude 工作区

- [ ] 新增 Codex 工作区页面，布局与 PC 当前工作区一致。
- [ ] 新增 Claude 工作区页面，布局与 Codex 共用组件。
- [ ] 顶部显示绑定设备状态和切换入口。
- [ ] 模型选择支持全部、OpenAI、Claude、DeepSeek、XiaomiMIMO 过滤。
- [ ] 输入区支持多行输入、发送、停止、权限模式、推理强度。
- [ ] 发送前校验：已登录、已绑定在线设备、有兼容模型、prompt 非空。

## 5. 时间线与日志

- [ ] 实现用户消息气泡。
- [ ] 实现执行日志组：标题、状态、事件列表、文件变更。
- [ ] 实现 AI 回复气泡。
- [ ] 实现等待确认事件卡。
- [ ] 实现日志去重：子级内容等于父级标题且无 detail 时隐藏。
- [ ] 实现空内容过滤：没有可显示内容时不渲染空点。
- [ ] 实现状态映射：进行中、已完成、已停止、等待确认、执行失败。

## 6. 同步机制

- [ ] 创建 job 后立即进入 pending 时间线。
- [ ] 设备 claim 后更新状态为 executing。
- [ ] 使用轮询或 SSE 拉取 event。
- [ ] 支持 `last_event_id` 增量补拉。
- [ ] 当前会话事件写入本地缓存。
- [ ] App 重启后恢复最近会话和未完成 job 状态。

## 7. 交互确认

- [ ] 解析 interaction event。
- [ ] 底部显示确认条。
- [ ] 支持允许、拒绝、总是允许本次会话。
- [ ] 提交后本地状态变为“已处理，等待桌面端确认”。
- [ ] 收到 ack 后移除确认条。

## 8. 通知

- [ ] job 完成通知。
- [ ] job 失败通知。
- [ ] 等待确认通知。
- [ ] 通知点击进入对应会话。

## 9. 测试

- [ ] 单元测试模型过滤规则。
- [ ] 单元测试日志事件归并和去重。
- [ ] 单元测试设备状态映射。
- [ ] 集成测试：创建 job、拉取 events、提交 interaction。
- [ ] 手工测试：PC 在线、Mac 在线、设备离线、执行失败、手动停止。
