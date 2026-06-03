# Contributing to MsgBridge

## 中文

感谢你对 MsgBridge 感兴趣。这个项目的目标是成为可自托管、可扩展、可审计的企业协同消息网关。

### 开发环境

```bash
mvn test
mvn spring-boot:run
```

本地控制台：

```text
http://localhost:8080/
```

默认账号：

```text
admin / admin123
```

### 提交前检查

请至少运行：

```bash
mvn test
scripts/smoke-test.sh
```

如果改动了页面功能，也建议运行 Playwright 端到端脚本：

```text
scripts/page-e2e-report.js
```

### 贡献方向

适合贡献的方向：

- 新渠道适配器，例如 Slack、Teams、短信、邮件或自定义 HTTP webhook。
- 更丰富的模板语法和变量校验。
- 更强的路由表达式、灰度路由和租户隔离能力。
- 更完整的仪表盘、告警、任务追踪和审计查询。
- SDK 示例，例如 Java、Go、Node.js、Python。
- 部署模板，例如 Helm chart、Kubernetes manifest、Terraform 示例。

### 代码约定

- 使用 Java 17 和 Spring Boot 3。
- 保持接口响应格式统一。
- 敏感字段必须加密存储并脱敏返回。
- 新的业务规则应补充单元测试。
- 新接口或重要行为变更要同步更新 `docs/`。

### 许可证

除非另有说明，贡献内容会按照 Apache License 2.0 授权。

## English

Thank you for your interest in MsgBridge. The goal of this project is to provide a self-hosted, extensible, and auditable enterprise collaboration message gateway.

### Development

```bash
mvn test
mvn spring-boot:run
```

Local console:

```text
http://localhost:8080/
```

Default account:

```text
admin / admin123
```

### Checks Before Submitting

Please run at least:

```bash
mvn test
scripts/smoke-test.sh
```

If you change the console UI, also run the Playwright E2E helper:

```text
scripts/page-e2e-report.js
```

### Good Contribution Areas

- New channel adapters, such as Slack, Teams, SMS, email, or generic HTTP webhooks.
- Richer template syntax and variable validation.
- Stronger route expressions, traffic shifting, and tenant isolation.
- Better dashboards, alerts, task tracing, and audit queries.
- SDK examples for Java, Go, Node.js, and Python.
- Deployment templates, such as Helm charts, Kubernetes manifests, and Terraform examples.

### Code Guidelines

- Use Java 17 and Spring Boot 3.
- Keep API responses consistent.
- Encrypt stored secrets and mask them in responses.
- Add unit tests for new business rules.
- Update `docs/` for new APIs or behavior changes.

### License

Unless explicitly stated otherwise, contributions are licensed under the Apache License 2.0.
