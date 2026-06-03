# MsgBridge

MsgBridge is an open-source enterprise collaboration message gateway. It lets business systems call one signed API and route messages to WeCom, DingTalk, Feishu, or future adapters through configurable apps, channels, templates, routes, retry policies, audit logs, and an operations console.

中文：MsgBridge 是一个开源企业协同消息网关。业务系统只需要接入一个带签名的统一 API，就可以通过应用、渠道、模板、路由、重试策略、审计日志和后台控制台，将消息发送到企业微信、钉钉、飞书或后续扩展渠道。

License: Apache-2.0

## Documentation

| Document | English | 中文 |
| --- | --- | --- |
| Full usage guide | [docs/USAGE.md](docs/USAGE.md#english) | [docs/USAGE.md](docs/USAGE.md#中文) |
| API reference | [docs/api.md](docs/api.md#english) | [docs/api.md](docs/api.md#中文) |
| Scenario playbooks | [docs/SCENARIOS.md](docs/SCENARIOS.md#english) | [docs/SCENARIOS.md](docs/SCENARIOS.md#中文) |
| Testing report | [docs/TESTING.md](docs/TESTING.md#english) | [docs/TESTING.md](docs/TESTING.md#中文) |
| Contributing | [CONTRIBUTING.md](CONTRIBUTING.md#english) | [CONTRIBUTING.md](CONTRIBUTING.md#中文) |

## What It Solves

Many teams start with direct robot webhooks in each service. Over time this creates duplicated signing code, leaked webhook URLs, inconsistent templates, missing retry logic, and no centralized audit trail. MsgBridge moves those concerns into one gateway.

很多团队一开始会在各业务系统里直接写群机器人 webhook。系统变多后，就会出现签名代码重复、webhook 泄露、模板不统一、失败无法重试、缺少审计链路等问题。MsgBridge 把这些能力收敛到一个网关里。

## Core Features

- Unified message API with HMAC-SHA256 request signing.
- Application management with per-app secret, rate limit, owner, and IP whitelist.
- Channel management for WeCom robot, DingTalk robot, and Feishu robot.
- Template rendering with variables for different scenes and channel types.
- Route rules by scene, route key, condition expression, and priority.
- Async message task worker, retry, manual retry, termination, CSV export, and purge preview.
- Admin console for dashboard, apps, channels, templates, routes, messages, audit logs, system status, users, and send testing.
- Admin login with bearer token and compatible admin API key mode.
- Secret encryption and masking for webhook URLs, robot secrets, access tokens, and app secrets.
- Spring Boot Actuator health, metrics, and Prometheus endpoints.

## Quick Start

Requirements:

- JDK 17+
- Maven 3.9+
- Optional: Docker and Docker Compose for MySQL deployment

Run locally with the embedded H2 file database:

```bash
mvn spring-boot:run
```

Open:

- Console: <http://localhost:8080/>
- Health: <http://localhost:8080/actuator/health>
- Prometheus: <http://localhost:8080/actuator/prometheus>

Default local credentials:

```text
Admin username: admin
Admin password: admin123
Admin key:      dev-admin-key
Demo appId:     demo
Demo appSecret: demo-secret
```

Run the smoke test:

```bash
scripts/smoke-test.sh
```

The smoke test covers admin login, dashboard, users, apps, channel health, templates, routes, audit logs, system status, template preview, route simulation, business message send, message query, CSV export, purge preview, bulk retry, bulk terminate, and admin key compatibility.

## Docker Compose With MySQL

Build the jar first:

```bash
mvn -DskipTests package
docker compose up --build
```

The compose file starts MySQL 8.4 and MsgBridge with the `mysql` Spring profile.

## Production Checklist

Before using MsgBridge outside local development, change these values:

```bash
export MSGBRIDGE_MASTER_KEY='a-long-random-master-key'
export MSGBRIDGE_ADMIN_KEY='a-strong-admin-key'
export MSGBRIDGE_DEFAULT_ADMIN_PASSWORD='a-strong-admin-password'
export MSGBRIDGE_SEED_DEMO=false
```

Recommended production controls:

- Use MySQL or another durable database through Spring datasource settings.
- Keep `MSGBRIDGE_MASTER_KEY` stable, because it encrypts stored channel secrets.
- Disable demo seed data after the first evaluation.
- Configure app IP whitelists and per-app rate limits.
- Put the service behind TLS and a trusted reverse proxy.
- Monitor `/actuator/health` and `/actuator/prometheus`.
- Review `/admin/system/status` warnings before go-live.

## Minimal Send Example

Send requests must be signed with the app secret. The canonical string is:

```text
HTTP_METHOD + "\n" +
REQUEST_URI_WITH_QUERY + "\n" +
TIMESTAMP + "\n" +
NONCE + "\n" +
RAW_BODY
```

Example payload:

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

See [docs/api.md](docs/api.md) for endpoint details and [docs/SCENARIOS.md](docs/SCENARIOS.md) for real-world integration playbooks.

## Project Structure

```text
src/main/java/com/msgbridge     Spring Boot source code
src/main/resources/db/migration Flyway database migrations
src/main/resources/static       Admin console
src/test/java/com/msgbridge     Unit and integration tests
docs                            Usage, API, and scenario documentation
scripts                         Smoke test and page E2E helpers
```

## 中文快速开始

本地启动：

```bash
mvn spring-boot:run
```

打开后台控制台：

```text
http://localhost:8080/
```

默认账号：

```text
用户名：admin
密码：admin123
管理密钥：dev-admin-key
开发应用：demo / demo-secret
```

执行全链路烟测：

```bash
scripts/smoke-test.sh
```

详细接入步骤、配置说明和场景化用法请阅读：

- [完整使用文档](docs/USAGE.md#中文)
- [API 参考](docs/api.md#中文)
- [场景使用说明](docs/SCENARIOS.md#中文)

## License

MsgBridge is released under the [Apache License 2.0](LICENSE).
