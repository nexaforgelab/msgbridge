# MsgBridge Scenario Playbooks

## 中文

### 场景 1：CRM 或订单系统发送成交提醒

适用场景：

- 新订单支付成功。
- 高价值商机进入关键阶段。
- 客户退款、发票、合同状态变化。

推荐配置：

| 项目 | 示例 |
| --- | --- |
| App | `crm` |
| Scene | `ORDER_PAID` |
| Route key | `sales_order_group` |
| Channels | `wecom_sales`、`dingtalk_sales_manager` |
| Template | `tpl_order_paid_wecom_v1` |

路由规则：

```json
{
  "ruleCode": "route_order_paid_high_value",
  "ruleName": "高金额订单提醒销售负责人",
  "sceneCode": "ORDER_PAID",
  "routeKey": "sales_order_group",
  "conditionExpr": "amount >= 10000",
  "targetChannels": ["wecom_sales", "dingtalk_sales_manager"],
  "priority": 100,
  "status": 1
}
```

发送消息：

```json
{
  "requestId": "crm-order-20260601-000001",
  "appId": "crm",
  "sceneCode": "ORDER_PAID",
  "priority": "HIGH",
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
      "amount": 12800,
      "orderNo": "SO202606010001",
      "url": "https://crm.example.com/orders/10001"
    }
  },
  "options": {
    "deduplicate": true,
    "retry": true,
    "maxRetryCount": 3
  }
}
```

运维建议：

- 用 `requestId` 绑定业务订单号，便于排查。
- 金额、客户名称、订单号放入 `message.data`，方便模板渲染和条件路由。
- 高价值订单可以路由到负责人群，普通订单路由到普通销售群。

### 场景 2：监控系统发送故障告警

适用场景：

- Prometheus、Grafana、Zabbix、云监控发送告警。
- 服务不可用、错误率升高、磁盘不足、队列积压。

推荐配置：

| 项目 | 示例 |
| --- | --- |
| App | `ops-monitor` |
| Scene | `ALERT_CRITICAL` |
| Route key | `runtime_ops` |
| Channels | `dingtalk_ops`、`feishu_oncall` |

模板示例：

```json
{
  "templateCode": "tpl_alert_critical_markdown",
  "templateName": "严重告警 Markdown",
  "sceneCode": "ALERT_CRITICAL",
  "channelType": "DINGTALK_ROBOT",
  "msgType": "MARKDOWN",
  "contentTemplate": "### ${severity} 告警\n服务：${service}\n实例：${instance}\n摘要：${summary}\n时间：${triggeredAt}\n详情：${url}",
  "variables": {
    "severity": "告警级别",
    "service": "服务名",
    "instance": "实例",
    "summary": "摘要",
    "triggeredAt": "触发时间",
    "url": "详情链接"
  }
}
```

路由建议：

- `severity == "critical"` 推送值班群和负责人群。
- `severity == "warning"` 只推送普通运维群。
- 告警恢复可以使用 `ALERT_RESOLVED` 场景。

运维建议：

- 开启重试，避免机器人服务短暂不可用时丢消息。
- 使用消息查询页按 `sceneCode`、状态和时间过滤。
- 对误报或过期告警使用终止操作，避免继续重试。

### 场景 3：审批和流程系统发送待办提醒

适用场景：

- OA 审批待处理。
- 合同、采购、报销进入审批节点。
- SLA 即将超时提醒。

推荐配置：

| 项目 | 示例 |
| --- | --- |
| App | `workflow` |
| Scene | `APPROVAL_PENDING` |
| Route key | `approval_notice` |
| Channels | `feishu_workflow`、`wecom_workflow` |

发送建议：

```json
{
  "requestId": "workflow-approval-20260601-0008",
  "appId": "workflow",
  "sceneCode": "APPROVAL_PENDING",
  "receiver": {
    "type": "ROUTE",
    "routeKey": "approval_notice"
  },
  "message": {
    "type": "MARKDOWN",
    "title": "审批待处理",
    "content": "采购申请 PR-1008 等待审批",
    "url": "https://workflow.example.com/tasks/1008",
    "data": {
      "taskNo": "PR-1008",
      "applicant": "李四",
      "amount": 6800,
      "dueMinutes": 30,
      "url": "https://workflow.example.com/tasks/1008"
    }
  },
  "options": {
    "deduplicate": true,
    "retry": true
  }
}
```

运维建议：

- `requestId` 使用流程任务 ID，重复提醒时保持一致可以避免重复入队。
- 超时升级可使用不同 routeKey，例如 `approval_escalation`。
- 审计日志可追踪后台是否有人手动重试或终止提醒。

### 场景 4：SaaS 平台多租户消息统一出口

适用场景：

- 多个租户、多个业务模块需要共享消息发送能力。
- 不希望每个租户系统单独保存机器人 webhook。

设计建议：

| 维度 | 建议 |
| --- | --- |
| App | 按业务系统创建，例如 `saas-billing`、`saas-ticket` |
| Scene | 按事件类型创建，例如 `INVOICE_OVERDUE`、`TICKET_ESCALATED` |
| Route key | 可使用租户、区域或套餐，例如 `tenant_a_ops` |
| Data | 带上 `tenantId`、`plan`、`region` 等字段 |

路由规则示例：

```json
{
  "ruleCode": "route_ticket_enterprise_tenant",
  "ruleName": "企业版客户工单升级",
  "sceneCode": "TICKET_ESCALATED",
  "routeKey": "tenant_support",
  "conditionExpr": "plan == 'enterprise'",
  "targetChannels": ["feishu_enterprise_support"],
  "priority": 90,
  "status": 1
}
```

运维建议：

- 每个系统使用独立 appSecret，便于撤销和轮换。
- 对关键租户配置专属渠道和更高优先级路由。
- 开启 rate limit，避免某个租户异常刷爆机器人。

### 场景 5：报表平台发送日报、周报和经营指标

适用场景：

- 每日销售日报。
- 周度经营看板。
- 数据异常提醒。

推荐配置：

| 项目 | 示例 |
| --- | --- |
| App | `bi-report` |
| Scene | `DAILY_REPORT` |
| Route key | `management_report` |
| Channels | `wecom_management` |

模板建议：

```text
【${reportName}】
日期：${reportDate}
销售额：${salesAmount}
新增客户：${newCustomers}
异常指标：${warnings}
详情：${url}
```

运维建议：

- 报表类消息建议使用固定 `requestId` 前缀，例如 `daily-report-20260603`。
- 对同一天的同一报表启用幂等，避免重复推送。
- CSV 导出可用于复盘某段时间的报表发送情况。

### 场景 6：CI/CD 发布通知

适用场景：

- GitHub Actions、GitLab CI、Jenkins 发布结果通知。
- 生产发布开始、成功、失败、回滚。

推荐配置：

| 项目 | 示例 |
| --- | --- |
| App | `cicd` |
| Scene | `DEPLOYMENT_STATUS` |
| Route key | `release_ops` |
| Channels | `dingtalk_release`、`feishu_rd` |

消息数据建议：

```json
{
  "service": "payment-api",
  "environment": "prod",
  "version": "v1.8.0",
  "status": "failed",
  "actor": "release-bot",
  "commit": "a1b2c3d",
  "url": "https://ci.example.com/jobs/9001"
}
```

路由建议：

- `environment == "prod"` 推生产发布群。
- `status == "failed"` 同时推值班群。
- 测试环境发布可以只推研发群。

### 场景 7：替代直接 webhook 调用

适用场景：

- 老系统已经有 webhook 出口，但希望统一鉴权、审计和重试。

可以使用：

```http
POST /webhook/{appId}/{sceneCode}
```

建议：

- 老系统保留原 payload，MsgBridge 作为统一入口。
- 仍然使用 HMAC 签名，避免 webhook 被外部滥用。
- 逐步把老系统的硬编码 webhook 迁移到渠道配置中。

## English

### Scenario 1: CRM or Order Paid Notification

Use when:

- A new order is paid.
- A high-value opportunity reaches a key stage.
- Refund, invoice, or contract status changes.

Recommended setup:

| Item | Example |
| --- | --- |
| App | `crm` |
| Scene | `ORDER_PAID` |
| Route key | `sales_order_group` |
| Channels | `wecom_sales`, `dingtalk_sales_manager` |
| Template | `tpl_order_paid_wecom_v1` |

Route rule:

```json
{
  "ruleCode": "route_order_paid_high_value",
  "ruleName": "High-value paid order",
  "sceneCode": "ORDER_PAID",
  "routeKey": "sales_order_group",
  "conditionExpr": "amount >= 10000",
  "targetChannels": ["wecom_sales", "dingtalk_sales_manager"],
  "priority": 100,
  "status": 1
}
```

Operational tips:

- Bind `requestId` to the business order ID.
- Put amount, customer name, and order number in `message.data`.
- Route high-value orders to owner groups and normal orders to regular sales groups.

### Scenario 2: Monitoring and Incident Alerts

Use when:

- Prometheus, Grafana, Zabbix, or cloud monitoring sends alerts.
- A service is down, error rate grows, disk is low, or queue backlog increases.

Recommended setup:

| Item | Example |
| --- | --- |
| App | `ops-monitor` |
| Scene | `ALERT_CRITICAL` |
| Route key | `runtime_ops` |
| Channels | `dingtalk_ops`, `feishu_oncall` |

Routing ideas:

- `severity == "critical"` routes to the on-call group and owner group.
- `severity == "warning"` routes to the standard operations group.
- Recovery notifications can use `ALERT_RESOLVED`.

Operational tips:

- Enable retry to survive short robot outages.
- Query messages by `sceneCode`, status, and time range.
- Terminate outdated alerts to stop further retries.

### Scenario 3: Approval and Workflow Reminders

Use when:

- An approval task is pending.
- Contract, purchase, or reimbursement flows enter an approval node.
- SLA is close to timeout.

Recommended setup:

| Item | Example |
| --- | --- |
| App | `workflow` |
| Scene | `APPROVAL_PENDING` |
| Route key | `approval_notice` |
| Channels | `feishu_workflow`, `wecom_workflow` |

Operational tips:

- Use the workflow task ID as `requestId`.
- Use another route key, such as `approval_escalation`, for timeout escalation.
- Use audit logs to see manual retries and terminations.

### Scenario 4: SaaS Multi-Tenant Message Gateway

Use when:

- Multiple tenants and modules need one message gateway.
- Tenant systems should not store robot webhooks.

Design tips:

| Dimension | Recommendation |
| --- | --- |
| App | Create one app per business system, such as `saas-billing` |
| Scene | Use event names, such as `INVOICE_OVERDUE` |
| Route key | Use tenant, region, or plan, such as `tenant_a_ops` |
| Data | Include `tenantId`, `plan`, `region`, and owner fields |

Operational tips:

- Use independent app secrets for each system.
- Configure dedicated channels for key tenants.
- Enable rate limits to protect robot endpoints from noisy tenants.

### Scenario 5: BI Reports and Scheduled Digests

Use when:

- Daily sales reports are sent.
- Weekly business dashboards are shared.
- Data anomalies need notification.

Recommended setup:

| Item | Example |
| --- | --- |
| App | `bi-report` |
| Scene | `DAILY_REPORT` |
| Route key | `management_report` |
| Channels | `wecom_management` |

Operational tips:

- Use stable request IDs such as `daily-report-20260603`.
- Enable deduplication for the same report and date.
- Use CSV export to review report delivery history.

### Scenario 6: CI/CD Deployment Notification

Use when:

- GitHub Actions, GitLab CI, or Jenkins reports deployment status.
- Release start, success, failure, or rollback needs notification.

Recommended setup:

| Item | Example |
| --- | --- |
| App | `cicd` |
| Scene | `DEPLOYMENT_STATUS` |
| Route key | `release_ops` |
| Channels | `dingtalk_release`, `feishu_rd` |

Routing ideas:

- `environment == "prod"` routes to production release groups.
- `status == "failed"` also routes to the on-call group.
- Test environment deployments can route only to development groups.

### Scenario 7: Replace Direct Webhook Calls

Use when:

- Legacy services already emit webhook payloads.
- You want centralized signing, auditing, retry, and channel secret storage.

Endpoint:

```http
POST /webhook/{appId}/{sceneCode}
```

Tips:

- Keep legacy payloads and migrate the destination into MsgBridge channels.
- Still sign requests with HMAC to prevent abuse.
- Gradually remove hard-coded robot webhooks from old systems.
