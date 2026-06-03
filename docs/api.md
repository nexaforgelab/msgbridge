# MsgBridge API Reference

## 中文

### 1. 认证模型

MsgBridge 有两类接口：

| 类型 | 路径 | 鉴权 |
| --- | --- | --- |
| 业务接口 | `/api/v1/messages/**`、`/webhook/**` | HMAC-SHA256 |
| 管理接口 | `/admin/**` | Bearer token 或 `X-MB-Admin-Key` |

### 2. 业务 HMAC 鉴权

业务系统调用 `/api/v1/messages/**` 与 `/webhook/**` 时必须带 HMAC 头。

| Header | 说明 |
| --- | --- |
| `X-MB-App-Id` | 调用方应用 ID。兼容 Webhook 路径时可省略，系统会从 `/webhook/{appId}/{sceneCode}` 取值 |
| `X-MB-Timestamp` | 秒级或毫秒级 Unix 时间戳 |
| `X-MB-Nonce` | 随机串，建议 UUID |
| `X-MB-Signature` | 小写 hex HMAC-SHA256 |

签名原文：

```text
HTTP_METHOD + "\n" +
REQUEST_URI_WITH_QUERY + "\n" +
TIMESTAMP + "\n" +
NONCE + "\n" +
RAW_BODY
```

签名密钥：当前 app 的 `appSecret`。

### 3. 管理鉴权

登录：

```http
POST /admin/auth/login
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "admin123"
}
```

后续请求：

```text
Authorization: Bearer <token>
```

兼容模式：

```text
X-MB-Admin-Key: <MSGBRIDGE_ADMIN_KEY>
```

### 4. 统一响应格式

成功响应通常为：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

失败响应示例：

```json
{
  "code": 401,
  "message": "invalid signature"
}
```

### 5. 业务消息接口

| Method | Path | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/messages/send` | 发送单条消息 |
| `POST` | `/api/v1/messages/batch-send` | 批量发送消息 |
| `GET` | `/api/v1/messages/{messageId}` | 查询当前 app 的消息详情 |
| `GET` | `/api/v1/messages/by-request-id?requestId=...` | 按业务请求 ID 查询消息 |
| `POST` | `/api/v1/messages/{messageId}/retry` | 业务侧自助重试当前 app 的消息 |
| `POST` | `/webhook/{appId}/{sceneCode}` | 兼容 webhook 风格入口 |

#### 发送消息

```http
POST /api/v1/messages/send
```

```json
{
  "requestId": "crm-order-20260601-000001",
  "appId": "demo",
  "sceneCode": "ORDER_PAID",
  "priority": "NORMAL",
  "receiver": {
    "type": "ROUTE",
    "routeKey": "sales_order_group"
  },
  "message": {
    "type": "ALERT",
    "title": "新订单提醒",
    "summary": "客户张三已支付订单",
    "content": "客户张三已支付订单，金额 12800 元",
    "level": "INFO",
    "url": "https://crm.example.com/orders/10001",
    "data": {
      "customerName": "张三",
      "amount": 12800,
      "orderNo": "SO202606010001"
    }
  },
  "options": {
    "deduplicate": true,
    "timeoutSeconds": 10,
    "retry": true,
    "maxRetryCount": 3
  }
}
```

响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "messageId": "msg_20260603025554_xxxxx",
    "requestId": "crm-order-20260601-000001",
    "status": "PENDING",
    "accepted": true
  }
}
```

#### 批量发送

```http
POST /api/v1/messages/batch-send
```

```json
{
  "messages": [
    {
      "requestId": "batch-1",
      "appId": "demo",
      "sceneCode": "ORDER_PAID",
      "receiver": {
        "type": "ROUTE",
        "routeKey": "sales_order_group"
      },
      "message": {
        "type": "TEXT",
        "content": "hello"
      }
    }
  ]
}
```

### 6. 数据模型

#### `SendMessageRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `requestId` | string | 是 | 业务侧幂等 ID |
| `appId` | string | 是 | 调用方应用 ID |
| `sceneCode` | string | 是 | 业务场景 |
| `priority` | string | 否 | 优先级标记 |
| `receiver` | object | 是 | 接收和路由信息 |
| `message` | object | 是 | 消息内容 |
| `options` | object | 否 | 幂等、超时和重试选项 |

#### `Receiver`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `type` | string | 常用 `ROUTE`，也可直接指定渠道 |
| `routeKey` | string | 路由键 |
| `channelCodes` | string[] | 直接发送到指定渠道 |

#### `Message`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `type` | string | `TEXT`、`MARKDOWN`、`ALERT` |
| `title` | string | 标题 |
| `summary` | string | 摘要 |
| `content` | string | 正文 |
| `level` | string | 告警或通知级别 |
| `url` | string | 详情链接 |
| `at` | object | @ 人配置，适配器可扩展 |
| `data` | object | 模板变量和条件表达式数据 |

#### `Options`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `deduplicate` | boolean | 是否使用 `requestId` 幂等 |
| `timeoutSeconds` | number | 单次发送超时 |
| `retry` | boolean | 是否允许自动重试，默认允许 |
| `maxRetryCount` | number | 最大重试次数，限制为 0 到 10 |

### 7. 管理接口

| Method | Path | 说明 |
| --- | --- | --- |
| `POST` | `/admin/auth/login` | 后台登录 |
| `GET` | `/admin/dashboard` | 首页指标 |
| `GET` | `/admin/apps` | 查询应用 |
| `POST` | `/admin/apps` | 创建应用 |
| `PUT` | `/admin/apps/{appId}` | 更新应用 |
| `POST` | `/admin/apps/{appId}/status` | 启用或停用应用 |
| `POST` | `/admin/apps/{appId}/reset-secret` | 重置应用密钥 |
| `GET` | `/admin/channels` | 查询渠道 |
| `GET` | `/admin/channels/health` | 查询渠道健康状态 |
| `GET` | `/admin/channels/{channelCode}` | 查询渠道详情 |
| `POST` | `/admin/channels` | 创建渠道 |
| `PUT` | `/admin/channels/{channelCode}` | 更新渠道 |
| `POST` | `/admin/channels/{channelCode}/status` | 启用或停用渠道 |
| `POST` | `/admin/channels/{channelCode}/test` | 发送渠道测试消息 |
| `GET` | `/admin/templates` | 查询模板 |
| `POST` | `/admin/templates` | 创建模板 |
| `PUT` | `/admin/templates/{templateCode}` | 更新模板 |
| `POST` | `/admin/templates/{templateCode}/status` | 启用或停用模板 |
| `POST` | `/admin/templates/preview` | 预览模板 |
| `GET` | `/admin/routes` | 查询路由规则 |
| `POST` | `/admin/routes` | 创建路由规则 |
| `PUT` | `/admin/routes/{ruleCode}` | 更新路由规则 |
| `POST` | `/admin/routes/{ruleCode}/status` | 启用或停用路由规则 |
| `POST` | `/admin/routes/simulate` | 模拟路由 |
| `GET` | `/admin/messages` | 查询消息任务 |
| `GET` | `/admin/messages/export` | 导出消息 CSV |
| `GET` | `/admin/messages/{messageId}` | 查询消息详情 |
| `POST` | `/admin/messages/{messageId}/retry` | 手动重试 |
| `POST` | `/admin/messages/{messageId}/terminate` | 终止消息 |
| `POST` | `/admin/messages/bulk-retry` | 批量重试 |
| `POST` | `/admin/messages/bulk-terminate` | 批量终止 |
| `GET` | `/admin/messages/purge-preview?before=...` | 清理预览 |
| `POST` | `/admin/messages/purge?before=...` | 清理消息和发送日志 |
| `GET` | `/admin/audit-logs` | 查询审计日志 |
| `GET` | `/admin/system/status` | 系统诊断 |
| `GET` | `/admin/users` | 查询后台用户 |
| `POST` | `/admin/users` | 创建后台用户 |
| `PUT` | `/admin/users/{username}` | 更新后台用户 |
| `POST` | `/admin/users/{username}/status` | 启用或停用用户 |
| `POST` | `/admin/users/{username}/reset-password` | 重置用户密码 |

### 8. 后台角色

| 角色 | 说明 |
| --- | --- |
| `SUPER_ADMIN` | 所有权限 |
| `PLATFORM_ADMIN` | 管理应用、渠道、模板、路由 |
| `BUSINESS_ADMIN` | 业务消息运维动作 |
| `OPS` | 查看和处理消息、渠道测试 |
| `AUDITOR` | 只读仪表盘、消息、审计和渠道健康 |
| `DEVELOPER` | 调试模板、路由模拟和渠道测试 |

### 9. 渠道示例

企业微信群机器人：

```json
{
  "channelCode": "wecom_sales",
  "channelName": "企业微信销售群",
  "channelType": "WE_COM_ROBOT",
  "config": {
    "default_msg_type": "markdown"
  },
  "secrets": {
    "webhook_url": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx"
  }
}
```

钉钉自定义机器人：

```json
{
  "channelCode": "dingtalk_ops",
  "channelName": "钉钉运维群",
  "channelType": "DINGTALK_ROBOT",
  "config": {
    "sign_enabled": true
  },
  "secrets": {
    "webhook_url": "https://oapi.dingtalk.com/robot/send?access_token=xxx",
    "secret": "SECxxx"
  }
}
```

飞书自定义机器人：

```json
{
  "channelCode": "feishu_rd",
  "channelName": "飞书研发群",
  "channelType": "FEISHU_ROBOT",
  "config": {
    "sign_enabled": true
  },
  "secrets": {
    "webhook_url": "https://open.feishu.cn/open-apis/bot/v2/hook/xxx",
    "secret": "xxx"
  }
}
```

### 10. 模板和路由示例

模板：

```json
{
  "templateCode": "tpl_order_paid_wecom_v1",
  "templateName": "订单支付提醒",
  "sceneCode": "ORDER_PAID",
  "channelType": "WE_COM_ROBOT",
  "msgType": "MARKDOWN",
  "contentTemplate": "【新订单提醒】\n客户：${customerName}\n订单号：${orderNo}\n金额：${amount} 元\n详情：${url}",
  "variables": {
    "customerName": "客户名称",
    "orderNo": "订单号",
    "amount": "金额",
    "url": "详情地址"
  }
}
```

路由：

```json
{
  "ruleCode": "route_order_paid_sales",
  "ruleName": "订单支付推送销售群",
  "sceneCode": "ORDER_PAID",
  "routeKey": "sales_order_group",
  "conditionExpr": "amount >= 10000",
  "targetChannels": ["wecom_sales", "dingtalk_ops"],
  "priority": 10,
  "status": 1
}
```

### 11. 本地可用性检查

```bash
scripts/smoke-test.sh
```

覆盖环境变量：

```bash
BASE_URL=http://localhost:8080 \
ADMIN_USER=admin \
ADMIN_PASSWORD=admin123 \
ADMIN_KEY=dev-admin-key \
APP_ID=demo \
APP_SECRET=demo-secret \
scripts/smoke-test.sh
```

## English

### 1. Authentication Model

MsgBridge has two API groups:

| Type | Path | Authentication |
| --- | --- | --- |
| Business API | `/api/v1/messages/**`, `/webhook/**` | HMAC-SHA256 |
| Admin API | `/admin/**` | Bearer token or `X-MB-Admin-Key` |

### 2. Business HMAC Authentication

Requests to `/api/v1/messages/**` and `/webhook/**` must include HMAC headers.

| Header | Description |
| --- | --- |
| `X-MB-App-Id` | Business application ID. For `/webhook/{appId}/{sceneCode}`, it can be inferred from the path |
| `X-MB-Timestamp` | Unix timestamp in seconds or milliseconds |
| `X-MB-Nonce` | Random value, UUID recommended |
| `X-MB-Signature` | Lowercase hex HMAC-SHA256 signature |

Canonical string:

```text
HTTP_METHOD + "\n" +
REQUEST_URI_WITH_QUERY + "\n" +
TIMESTAMP + "\n" +
NONCE + "\n" +
RAW_BODY
```

Signing key: the current app's `appSecret`.

### 3. Admin Authentication

Login:

```http
POST /admin/auth/login
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Then send:

```text
Authorization: Bearer <token>
```

Compatible admin key mode:

```text
X-MB-Admin-Key: <MSGBRIDGE_ADMIN_KEY>
```

### 4. Unified Response

Success response:

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

Failure response:

```json
{
  "code": 401,
  "message": "invalid signature"
}
```

### 5. Business Message APIs

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/messages/send` | Send one message |
| `POST` | `/api/v1/messages/batch-send` | Send messages in batch |
| `GET` | `/api/v1/messages/{messageId}` | Query current app's message detail |
| `GET` | `/api/v1/messages/by-request-id?requestId=...` | Query by business request ID |
| `POST` | `/api/v1/messages/{messageId}/retry` | Current app self-service retry |
| `POST` | `/webhook/{appId}/{sceneCode}` | Webhook-style compatibility endpoint |

#### Send Message

```http
POST /api/v1/messages/send
```

```json
{
  "requestId": "crm-order-20260601-000001",
  "appId": "demo",
  "sceneCode": "ORDER_PAID",
  "priority": "NORMAL",
  "receiver": {
    "type": "ROUTE",
    "routeKey": "sales_order_group"
  },
  "message": {
    "type": "ALERT",
    "title": "New paid order",
    "summary": "Customer Alice paid an order",
    "content": "Customer Alice paid order SO202606010001, amount 12800",
    "level": "INFO",
    "url": "https://crm.example.com/orders/10001",
    "data": {
      "customerName": "Alice",
      "amount": 12800,
      "orderNo": "SO202606010001"
    }
  },
  "options": {
    "deduplicate": true,
    "timeoutSeconds": 10,
    "retry": true,
    "maxRetryCount": 3
  }
}
```

Response:

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "messageId": "msg_20260603025554_xxxxx",
    "requestId": "crm-order-20260601-000001",
    "status": "PENDING",
    "accepted": true
  }
}
```

### 6. Data Models

#### `SendMessageRequest`

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `requestId` | string | yes | Business idempotency ID |
| `appId` | string | yes | Business application ID |
| `sceneCode` | string | yes | Business scene |
| `priority` | string | no | Priority marker |
| `receiver` | object | yes | Receiver and route information |
| `message` | object | yes | Message content |
| `options` | object | no | Idempotency, timeout, and retry options |

#### `Receiver`

| Field | Type | Description |
| --- | --- | --- |
| `type` | string | Commonly `ROUTE`; direct channels are also supported |
| `routeKey` | string | Route key |
| `channelCodes` | string[] | Direct target channels |

#### `Message`

| Field | Type | Description |
| --- | --- | --- |
| `type` | string | `TEXT`, `MARKDOWN`, `ALERT` |
| `title` | string | Title |
| `summary` | string | Summary |
| `content` | string | Body content |
| `level` | string | Notification or alert level |
| `url` | string | Detail URL |
| `at` | object | Mention configuration for adapters |
| `data` | object | Template variables and route condition data |

#### `Options`

| Field | Type | Description |
| --- | --- | --- |
| `deduplicate` | boolean | Use `requestId` idempotency |
| `timeoutSeconds` | number | Single-send timeout |
| `retry` | boolean | Enable automatic retry, enabled by default |
| `maxRetryCount` | number | Max retry count, clamped to 0 through 10 |

### 7. Admin APIs

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/admin/auth/login` | Admin login |
| `GET` | `/admin/dashboard` | Dashboard metrics |
| `GET` | `/admin/apps` | List apps |
| `POST` | `/admin/apps` | Create app |
| `PUT` | `/admin/apps/{appId}` | Update app |
| `POST` | `/admin/apps/{appId}/status` | Enable or disable app |
| `POST` | `/admin/apps/{appId}/reset-secret` | Reset app secret |
| `GET` | `/admin/channels` | List channels |
| `GET` | `/admin/channels/health` | Channel health |
| `GET` | `/admin/channels/{channelCode}` | Channel detail |
| `POST` | `/admin/channels` | Create channel |
| `PUT` | `/admin/channels/{channelCode}` | Update channel |
| `POST` | `/admin/channels/{channelCode}/status` | Enable or disable channel |
| `POST` | `/admin/channels/{channelCode}/test` | Send channel test |
| `GET` | `/admin/templates` | List templates |
| `POST` | `/admin/templates` | Create template |
| `PUT` | `/admin/templates/{templateCode}` | Update template |
| `POST` | `/admin/templates/{templateCode}/status` | Enable or disable template |
| `POST` | `/admin/templates/preview` | Preview template |
| `GET` | `/admin/routes` | List route rules |
| `POST` | `/admin/routes` | Create route rule |
| `PUT` | `/admin/routes/{ruleCode}` | Update route rule |
| `POST` | `/admin/routes/{ruleCode}/status` | Enable or disable route rule |
| `POST` | `/admin/routes/simulate` | Simulate route |
| `GET` | `/admin/messages` | Query message tasks |
| `GET` | `/admin/messages/export` | Export message CSV |
| `GET` | `/admin/messages/{messageId}` | Message detail |
| `POST` | `/admin/messages/{messageId}/retry` | Manual retry |
| `POST` | `/admin/messages/{messageId}/terminate` | Terminate message |
| `POST` | `/admin/messages/bulk-retry` | Bulk retry |
| `POST` | `/admin/messages/bulk-terminate` | Bulk terminate |
| `GET` | `/admin/messages/purge-preview?before=...` | Purge preview |
| `POST` | `/admin/messages/purge?before=...` | Purge messages and send logs |
| `GET` | `/admin/audit-logs` | Query audit logs |
| `GET` | `/admin/system/status` | System diagnostics |
| `GET` | `/admin/users` | List admin users |
| `POST` | `/admin/users` | Create admin user |
| `PUT` | `/admin/users/{username}` | Update admin user |
| `POST` | `/admin/users/{username}/status` | Enable or disable user |
| `POST` | `/admin/users/{username}/reset-password` | Reset user password |

### 8. Admin Roles

| Role | Description |
| --- | --- |
| `SUPER_ADMIN` | All permissions |
| `PLATFORM_ADMIN` | Manage apps, channels, templates, and routes |
| `BUSINESS_ADMIN` | Business message operations |
| `OPS` | View and process messages, run channel tests |
| `AUDITOR` | Read-only dashboard, messages, audit logs, and channel health |
| `DEVELOPER` | Debug templates, route simulation, and channel tests |

### 9. Channel Examples

WeCom group robot:

```json
{
  "channelCode": "wecom_sales",
  "channelName": "WeCom sales group",
  "channelType": "WE_COM_ROBOT",
  "config": {
    "default_msg_type": "markdown"
  },
  "secrets": {
    "webhook_url": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx"
  }
}
```

DingTalk custom robot:

```json
{
  "channelCode": "dingtalk_ops",
  "channelName": "DingTalk ops group",
  "channelType": "DINGTALK_ROBOT",
  "config": {
    "sign_enabled": true
  },
  "secrets": {
    "webhook_url": "https://oapi.dingtalk.com/robot/send?access_token=xxx",
    "secret": "SECxxx"
  }
}
```

Feishu custom robot:

```json
{
  "channelCode": "feishu_rd",
  "channelName": "Feishu R&D group",
  "channelType": "FEISHU_ROBOT",
  "config": {
    "sign_enabled": true
  },
  "secrets": {
    "webhook_url": "https://open.feishu.cn/open-apis/bot/v2/hook/xxx",
    "secret": "xxx"
  }
}
```

### 10. Local Smoke Test

```bash
scripts/smoke-test.sh
```

Override settings:

```bash
BASE_URL=http://localhost:8080 \
ADMIN_USER=admin \
ADMIN_PASSWORD=admin123 \
ADMIN_KEY=dev-admin-key \
APP_ID=demo \
APP_SECRET=demo-secret \
scripts/smoke-test.sh
```
