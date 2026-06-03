# MsgBridge Usage Guide

## 中文

### 1. 产品定位

MsgBridge 是企业协同消息网关，适合把 CRM、ERP、订单系统、监控系统、CI/CD、报表平台、审批系统等业务消息统一推送到企业微信、钉钉、飞书等协同工具。

它解决的问题：

- 业务系统不再直接保存群机器人 webhook 和 secret。
- 消息模板、路由、重试、审计、查询、导出、运维处理集中管理。
- 不同系统使用统一 API、统一签名、统一幂等策略。
- 运维团队可以在后台查看失败消息、手动重试、终止、导出和清理。

### 2. 运行要求

| 组件 | 要求 |
| --- | --- |
| Java | JDK 17 或更高 |
| Maven | 3.9 或更高 |
| 数据库 | 本地开发默认 H2，生产建议 MySQL |
| 容器 | 可选，Docker Compose 可启动 MySQL + MsgBridge |

### 3. 本地启动

```bash
mvn spring-boot:run
```

默认使用 H2 文件数据库：

```text
./data/msgbridge
```

启动后访问：

| 功能 | 地址 |
| --- | --- |
| 后台控制台 | `http://localhost:8080/` |
| 健康检查 | `http://localhost:8080/actuator/health` |
| Prometheus 指标 | `http://localhost:8080/actuator/prometheus` |

本地默认账号：

```text
username: admin
password: admin123
admin key: dev-admin-key
demo appId: demo
demo appSecret: demo-secret
```

### 4. Docker Compose 启动 MySQL 版本

```bash
mvn -DskipTests package
docker compose up --build
```

Compose 会启动：

- `mysql`: MySQL 8.4
- `msgbridge`: 使用 `mysql` profile 的 MsgBridge 服务

默认端口：

```text
MySQL: 3306
MsgBridge: 8080
```

### 5. 关键配置

配置文件：`src/main/resources/application.yml`

常用环境变量：

| 环境变量 | 说明 | 默认值 |
| --- | --- | --- |
| `SERVER_PORT` | HTTP 端口 | `8080` |
| `SPRING_DATASOURCE_URL` | 数据库连接 | H2 文件数据库 |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | 空 |
| `MSGBRIDGE_MASTER_KEY` | 敏感字段加密主密钥 | `change-me-dev-master-key` |
| `MSGBRIDGE_ADMIN_KEY` | 管理 API 兼容密钥 | `dev-admin-key` |
| `MSGBRIDGE_DEFAULT_ADMIN_USERNAME` | 默认后台用户名 | `admin` |
| `MSGBRIDGE_DEFAULT_ADMIN_PASSWORD` | 默认后台密码 | `admin123` |
| `MSGBRIDGE_ADMIN_TOKEN_TTL_SECONDS` | 后台 token 有效期 | `28800` |
| `MSGBRIDGE_TIMESTAMP_SKEW_SECONDS` | HMAC 时间戳容忍窗口 | `300` |
| `MSGBRIDGE_WORKER_ENABLED` | 是否启用异步发送 worker | `true` |
| `MSGBRIDGE_WORKER_BATCH_SIZE` | worker 每批处理数量 | `20` |
| `MSGBRIDGE_WORKER_FIXED_DELAY_MS` | worker 轮询间隔 | `1000` |
| `MSGBRIDGE_SEED_DEMO` | 是否写入 demo 数据 | `true` |

生产环境至少要覆盖：

```bash
export MSGBRIDGE_MASTER_KEY='a-long-random-master-key'
export MSGBRIDGE_ADMIN_KEY='a-strong-admin-key'
export MSGBRIDGE_DEFAULT_ADMIN_PASSWORD='a-strong-admin-password'
export MSGBRIDGE_SEED_DEMO=false
```

### 6. 控制台使用流程

打开 `http://localhost:8080/` 后，建议按以下顺序配置：

1. 登录后台：使用 `admin / admin123`，生产环境请改密码。
2. 应用：创建业务调用方，获取 `appId` 和 `appSecret`。
3. 渠道：创建企业微信、钉钉或飞书机器人渠道，填写 webhook 和 secret。
4. 模板：按业务场景创建消息模板，例如 `ORDER_PAID`、`ALERT_CRITICAL`。
5. 路由：为场景配置路由规则，指定 routeKey、条件表达式和目标渠道。
6. 发送测试：用控制台内置表单验证消息是否可以入队。
7. 消息：查询发送任务、查看详情、重试失败消息、终止无效消息、导出 CSV。
8. 审计：查看后台操作记录。
9. 系统：查看运行状态、worker 配置、生产风险提示。
10. 用户：创建不同角色的后台用户。

### 7. 管理鉴权

后台接口支持两种方式。

方式一：登录拿 bearer token。

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

方式二：兼容 Admin Key。

```text
X-MB-Admin-Key: dev-admin-key
```

生产环境推荐使用后台账号和角色权限，Admin Key 只用于早期集成或受控自动化脚本。

### 8. 业务接口签名

业务系统调用 `/api/v1/messages/**` 或 `/webhook/**` 需要 HMAC-SHA256 签名。

请求头：

| Header | 说明 |
| --- | --- |
| `X-MB-App-Id` | 调用方应用 ID |
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

Shell 生成签名示例：

```bash
body='{"requestId":"demo-1","appId":"demo","sceneCode":"SMOKE_NO_ROUTE","receiver":{"type":"ROUTE","routeKey":"missing"},"message":{"type":"TEXT","content":"hello"}}'
timestamp="$(date +%s)"
nonce="$(uuidgen)"
path="/api/v1/messages/send"
canonical="POST
$path
$timestamp
$nonce
$body"
signature="$(printf '%s' "$canonical" | openssl dgst -sha256 -hmac 'demo-secret' | awk '{print $NF}')"

curl -X POST "http://localhost:8080$path" \
  -H "Content-Type: application/json" \
  -H "X-MB-App-Id: demo" \
  -H "X-MB-Timestamp: $timestamp" \
  -H "X-MB-Nonce: $nonce" \
  -H "X-MB-Signature: $signature" \
  -d "$body"
```

### 9. 发送消息

核心接口：

```http
POST /api/v1/messages/send
```

请求体：

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
    "content": "客户张三已支付订单，金额 12800 元",
    "url": "https://crm.example.com/orders/10001",
    "data": {
      "customerName": "张三",
      "amount": "12800",
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

字段说明：

| 字段 | 说明 |
| --- | --- |
| `requestId` | 业务侧请求 ID，用于幂等和排查 |
| `appId` | 调用方应用 ID |
| `sceneCode` | 业务场景编码 |
| `priority` | 优先级，目前可用于排序和扩展 |
| `receiver.type` | 接收方式，常用 `ROUTE` |
| `receiver.routeKey` | 路由键，用于匹配规则 |
| `receiver.channelCodes` | 直接指定渠道时使用 |
| `message.type` | `TEXT`、`MARKDOWN`、`ALERT` |
| `message.data` | 模板变量和条件表达式数据 |
| `options.deduplicate` | 是否启用 requestId 幂等 |
| `options.retry` | 是否允许失败重试 |
| `options.maxRetryCount` | 最大重试次数，范围会被限制在 0 到 10 |

### 10. 渠道配置

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

敏感字段会加密保存，列表返回只展示脱敏占位。

### 11. 模板和路由

模板示例：

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

路由示例：

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

路由匹配逻辑：

- `sceneCode` 必须匹配。
- `routeKey` 为空时可作为该场景的默认规则。
- `conditionExpr` 可根据 `message.data` 中的变量判断。
- `priority` 越大越优先。
- `targetChannels` 指定最终发送渠道。

### 12. 运维和排障

常用操作：

| 操作 | 入口 |
| --- | --- |
| 查询消息 | 控制台消息页或 `GET /admin/messages` |
| 查看消息详情 | `GET /admin/messages/{messageId}` |
| 手动重试 | `POST /admin/messages/{messageId}/retry` |
| 业务侧自助重试 | `POST /api/v1/messages/{messageId}/retry` |
| 批量重试 | `POST /admin/messages/bulk-retry` |
| 终止消息 | `POST /admin/messages/{messageId}/terminate` |
| 导出 CSV | `GET /admin/messages/export` |
| 清理预览 | `GET /admin/messages/purge-preview?before=...` |
| 执行清理 | `POST /admin/messages/purge?before=...` |
| 查看审计 | `GET /admin/audit-logs` |
| 系统诊断 | `GET /admin/system/status` |

建议先使用清理预览确认影响范围，再执行清理。

### 13. 测试验证

单元测试：

```bash
mvn test
```

打包：

```bash
mvn -DskipTests package
```

接口烟测：

```bash
scripts/smoke-test.sh
```

可覆盖环境变量：

```bash
BASE_URL=http://localhost:8080 \
ADMIN_USER=admin \
ADMIN_PASSWORD=admin123 \
ADMIN_KEY=dev-admin-key \
APP_ID=demo \
APP_SECRET=demo-secret \
scripts/smoke-test.sh
```

页面端到端辅助脚本位于：

```text
scripts/page-e2e-report.js
```

它可配合 Playwright CLI 执行登录、页面切换、发送测试、消息查询和运维控件验证。

### 14. 生产建议

- 使用 MySQL 等持久化数据库，不要在生产使用本地 H2 文件库。
- 固定并妥善保管 `MSGBRIDGE_MASTER_KEY`，更换主密钥会影响已有密文读取。
- 禁用 demo seed：`MSGBRIDGE_SEED_DEMO=false`。
- 为每个业务系统单独创建 app，并配置 rate limit 与 IP whitelist。
- 不要在客户端或前端暴露 `appSecret`。
- 后台控制台应放在内网、VPN 或可信访问层之后。
- 使用 HTTPS，避免明文传输签名头和业务内容。
- 接入 Prometheus 监控健康状态和关键指标。
- 定期导出或归档历史消息，避免任务表无限增长。

## English

### 1. Product Purpose

MsgBridge is an enterprise collaboration message gateway. It helps CRM, ERP, order platforms, monitoring systems, CI/CD pipelines, reporting systems, approval workflows, and other services deliver messages to WeCom, DingTalk, Feishu, and future adapters through one unified gateway.

It centralizes:

- Signed business message APIs.
- Application secrets, rate limits, owners, and IP whitelists.
- Channel webhook secrets and masking.
- Templates, routes, retries, audit logs, exports, and operations.
- A browser-based admin console for daily operations.

### 2. Requirements

| Component | Requirement |
| --- | --- |
| Java | JDK 17+ |
| Maven | 3.9+ |
| Database | H2 for local development, MySQL recommended for production |
| Container runtime | Optional Docker Compose deployment |

### 3. Local Run

```bash
mvn spring-boot:run
```

The default database is a local H2 file database:

```text
./data/msgbridge
```

Open:

| Feature | URL |
| --- | --- |
| Admin console | `http://localhost:8080/` |
| Health | `http://localhost:8080/actuator/health` |
| Prometheus | `http://localhost:8080/actuator/prometheus` |

Default local credentials:

```text
username: admin
password: admin123
admin key: dev-admin-key
demo appId: demo
demo appSecret: demo-secret
```

### 4. Docker Compose With MySQL

```bash
mvn -DskipTests package
docker compose up --build
```

The compose stack starts:

- `mysql`: MySQL 8.4
- `msgbridge`: MsgBridge with the Spring `mysql` profile

### 5. Important Configuration

Common environment variables:

| Variable | Description | Default |
| --- | --- | --- |
| `SERVER_PORT` | HTTP port | `8080` |
| `SPRING_DATASOURCE_URL` | Database URL | H2 file database |
| `SPRING_DATASOURCE_USERNAME` | Database username | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | empty |
| `MSGBRIDGE_MASTER_KEY` | Master key for encrypted secrets | `change-me-dev-master-key` |
| `MSGBRIDGE_ADMIN_KEY` | Compatible admin API key | `dev-admin-key` |
| `MSGBRIDGE_DEFAULT_ADMIN_USERNAME` | Default admin username | `admin` |
| `MSGBRIDGE_DEFAULT_ADMIN_PASSWORD` | Default admin password | `admin123` |
| `MSGBRIDGE_ADMIN_TOKEN_TTL_SECONDS` | Admin token TTL | `28800` |
| `MSGBRIDGE_TIMESTAMP_SKEW_SECONDS` | HMAC timestamp skew window | `300` |
| `MSGBRIDGE_WORKER_ENABLED` | Enable async worker | `true` |
| `MSGBRIDGE_WORKER_BATCH_SIZE` | Worker batch size | `20` |
| `MSGBRIDGE_WORKER_FIXED_DELAY_MS` | Worker polling delay | `1000` |
| `MSGBRIDGE_SEED_DEMO` | Seed demo data | `true` |

For production:

```bash
export MSGBRIDGE_MASTER_KEY='a-long-random-master-key'
export MSGBRIDGE_ADMIN_KEY='a-strong-admin-key'
export MSGBRIDGE_DEFAULT_ADMIN_PASSWORD='a-strong-admin-password'
export MSGBRIDGE_SEED_DEMO=false
```

### 6. Console Workflow

Recommended setup order:

1. Log in to the admin console.
2. Create a business application and get its `appId` and `appSecret`.
3. Create channels for WeCom, DingTalk, or Feishu robots.
4. Create templates for scenes such as `ORDER_PAID` or `ALERT_CRITICAL`.
5. Create route rules with scene code, route key, condition expression, target channels, and priority.
6. Use the send-test page to verify message ingestion.
7. Use the messages page to query tasks, inspect details, retry, terminate, export, and preview purge operations.
8. Use audit logs to review admin actions.
9. Use system status to review runtime diagnostics and production warnings.
10. Create users with appropriate roles.

### 7. Admin Authentication

Option 1: Login and use bearer token.

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

Option 2: Compatible admin key.

```text
X-MB-Admin-Key: dev-admin-key
```

### 8. Business API Signing

Business requests to `/api/v1/messages/**` and `/webhook/**` must be signed with HMAC-SHA256.

Headers:

| Header | Description |
| --- | --- |
| `X-MB-App-Id` | Business application ID |
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

### 9. Send Message

Main endpoint:

```http
POST /api/v1/messages/send
```

Example:

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
    "content": "Customer Alice paid order SO202606010001, amount 12800",
    "url": "https://crm.example.com/orders/10001",
    "data": {
      "customerName": "Alice",
      "amount": "12800",
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

### 10. Operations

| Operation | Entry |
| --- | --- |
| Query messages | Console messages page or `GET /admin/messages` |
| Message detail | `GET /admin/messages/{messageId}` |
| Manual retry | `POST /admin/messages/{messageId}/retry` |
| Business self-service retry | `POST /api/v1/messages/{messageId}/retry` |
| Bulk retry | `POST /admin/messages/bulk-retry` |
| Terminate message | `POST /admin/messages/{messageId}/terminate` |
| Export CSV | `GET /admin/messages/export` |
| Purge preview | `GET /admin/messages/purge-preview?before=...` |
| Execute purge | `POST /admin/messages/purge?before=...` |
| Audit logs | `GET /admin/audit-logs` |
| System diagnostics | `GET /admin/system/status` |

### 11. Testing

```bash
mvn test
mvn -DskipTests package
scripts/smoke-test.sh
```

Override smoke-test settings:

```bash
BASE_URL=http://localhost:8080 \
ADMIN_USER=admin \
ADMIN_PASSWORD=admin123 \
ADMIN_KEY=dev-admin-key \
APP_ID=demo \
APP_SECRET=demo-secret \
scripts/smoke-test.sh
```

The page E2E helper is:

```text
scripts/page-e2e-report.js
```

### 12. Production Guidance

- Use a durable database such as MySQL.
- Keep `MSGBRIDGE_MASTER_KEY` stable and secret.
- Disable demo data with `MSGBRIDGE_SEED_DEMO=false`.
- Create one app per business system.
- Configure rate limits and IP whitelists per app.
- Never expose `appSecret` in browsers or mobile clients.
- Put the admin console behind a trusted access layer.
- Use HTTPS.
- Monitor health and Prometheus metrics.
- Archive or purge old message records after previewing the impact.
