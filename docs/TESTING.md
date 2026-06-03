# MsgBridge Testing Report

## 中文

### 最新验证结论

验证时间：2026-06-03 12:58:43 CST (+0800)

验证目标：在首次发布到 GitHub 后，重新验证最新本地代码、最新 jar 服务、接口正反向能力和页面端到端主流程。

| 测试类别 | 结果 | 通过 / 总数 |
| --- | --- | ---: |
| 后端单元测试 | PASS | 11 / 11 |
| 应用打包 | PASS | 1 / 1 |
| 服务健康检查 | PASS | 3 / 3 |
| 接口正向烟测 | PASS | 18 / 18 |
| 接口反向验证 | PASS | 6 / 6 |
| 页面端到端测试 | PASS | 11 / 11 |
| 页面截图抽查 | PASS | 1 / 1 |

最终结论：全量验证全部通过。服务运行在 `http://localhost:8080/`，Actuator 返回 `UP`，页面端到端测试没有捕获到 `console.error`。

### 执行链路

1. `mvn test`
   - 结果：`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`
2. `mvn -DskipTests package`
   - 结果：`BUILD SUCCESS`
3. 使用 `target/msgbridge-0.1.0-SNAPSHOT.jar` 启动服务。
4. 健康检查：
   - `GET /actuator/health` 返回 `{"status":"UP","groups":["liveness","readiness"]}`
   - 首页返回 HTTP 200
5. `scripts/smoke-test.sh`
   - 覆盖后台登录、仪表盘、用户、应用、渠道健康、模板、路由、审计、系统状态、模板预览、路由模拟、业务发送、消息查询、CSV 导出、清理预览、批量重试、批量终止、Admin Key 兼容访问。
6. 接口反向验证：
   - 覆盖错误密码、缺少认证、非法状态、清理时间过近、缺少业务签名、签名错误。
7. Playwright 页面端到端测试：
   - 覆盖首页导航、登录、应用、渠道、模板预览、路由模拟、发送测试、消息查询、消息维护、系统诊断、用户和审计页面。

### 本地报告文件

这些文件是本地测试产物，已被 `.gitignore` 忽略，不会提交到仓库：

| 文件 | 说明 |
| --- | --- |
| `reports/test-summary.md` | 本地完整测试汇总 |
| `reports/backend-unit-test-report.txt` | Maven 单元测试输出 |
| `reports/package-report.txt` | Maven 打包输出 |
| `reports/service-health-report.txt` | 服务健康检查输出 |
| `reports/api-interface-test-report.txt` | 接口正向烟测输出 |
| `reports/api-negative-test-report.txt` | 接口反向验证输出 |
| `reports/e2e-page-test-report.json` | 页面 E2E JSON 报告 |
| `reports/e2e-page-final.png` | 页面 E2E 最终截图 |

### 备注

- `reports/`、`data/`、`target/` 和 `.playwright-cli/` 都是本地运行或测试产物，不适合提交到 GitHub。
- 本地日志中的 Netty macOS native DNS warning 是 macOS 原生 DNS 库缺失告警，不影响本轮功能验证。

## English

### Latest Verification Result

Verification time: 2026-06-03 12:58:43 CST (+0800)

Goal: re-verify the latest local code, latest packaged jar, positive and negative API flows, and browser end-to-end console workflows after the first GitHub release.

| Test Category | Result | Passed / Total |
| --- | --- | ---: |
| Backend unit tests | PASS | 11 / 11 |
| Application packaging | PASS | 1 / 1 |
| Service health check | PASS | 3 / 3 |
| Positive API smoke test | PASS | 18 / 18 |
| Negative API validation | PASS | 6 / 6 |
| Page end-to-end test | PASS | 11 / 11 |
| Page screenshot review | PASS | 1 / 1 |

Final conclusion: all verification stages passed. The service ran on `http://localhost:8080/`, Actuator returned `UP`, and the browser E2E run captured zero `console.error` entries.

### Execution Flow

1. `mvn test`
   - Result: `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`
2. `mvn -DskipTests package`
   - Result: `BUILD SUCCESS`
3. Started the service from `target/msgbridge-0.1.0-SNAPSHOT.jar`.
4. Health checks:
   - `GET /actuator/health` returned `{"status":"UP","groups":["liveness","readiness"]}`
   - The home page returned HTTP 200
5. `scripts/smoke-test.sh`
   - Covered admin login, dashboard, users, apps, channel health, templates, routes, audit logs, system status, template preview, route simulation, business send, message query, CSV export, purge preview, bulk retry, bulk terminate, and admin key compatibility.
6. Negative API validation:
   - Covered wrong admin password, missing admin auth, invalid message status, purge cutoff too recent, missing business signature, and invalid business signature.
7. Playwright page E2E:
   - Covered home navigation, login, apps, channels, template preview, route simulation, send test, message query, message maintenance controls, system diagnostics, users, and audit logs.

### Local Report Files

These files are local test artifacts and are intentionally ignored by `.gitignore`:

| File | Description |
| --- | --- |
| `reports/test-summary.md` | Local full test summary |
| `reports/backend-unit-test-report.txt` | Maven unit test output |
| `reports/package-report.txt` | Maven package output |
| `reports/service-health-report.txt` | Service health output |
| `reports/api-interface-test-report.txt` | Positive API smoke test output |
| `reports/api-negative-test-report.txt` | Negative API validation output |
| `reports/e2e-page-test-report.json` | Page E2E JSON report |
| `reports/e2e-page-final.png` | Final page E2E screenshot |

### Notes

- `reports/`, `data/`, `target/`, and `.playwright-cli/` are local runtime or test artifacts and should not be committed to GitHub.
- The local Netty macOS native DNS warning is a missing native DNS library warning and did not affect this verification run.
