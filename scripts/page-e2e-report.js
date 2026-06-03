async (page) => {
  const report = {
    suite: "MsgBridge Console Playwright E2E",
    baseUrl: "http://localhost:8080/",
    startedAt: new Date().toISOString(),
    cases: [],
    consoleErrors: []
  };

  page.on("console", (message) => {
    if (message.type() === "error") {
      report.consoleErrors.push({
        type: message.type(),
        text: message.text()
      });
    }
  });

  async function record(name, fn) {
    const started = Date.now();
    try {
      const details = await fn();
      report.cases.push({
        name,
        status: "PASS",
        durationMs: Date.now() - started,
        details: details || {}
      });
    } catch (error) {
      report.cases.push({
        name,
        status: "FAIL",
        durationMs: Date.now() - started,
        error: String(error && error.message ? error.message : error)
      });
    }
  }

  async function expectText(selector, text, label) {
    const content = await page.locator(selector).innerText({ timeout: 5000 });
    if (!content.includes(text)) {
      throw new Error(`${label || selector} missing text: ${text}`);
    }
    return content;
  }

  await page.goto("http://localhost:8080/", { waitUntil: "networkidle" });

  await record("page loads and tabs render", async () => {
    const title = await page.title();
    const body = await page.locator("body").innerText();
    const tabs = ["仪表盘", "应用", "渠道", "模板", "路由", "消息", "审计", "系统", "用户", "发送测试"];
    const missing = tabs.filter((tab) => !body.includes(tab));
    if (title !== "MsgBridge Console") {
      throw new Error(`unexpected title: ${title}`);
    }
    if (missing.length) {
      throw new Error(`missing tabs: ${missing.join(", ")}`);
    }
    return { title, tabs };
  });

  await record("admin login and dashboard", async () => {
    await page.fill("#loginUser", "admin");
    await page.fill("#loginPassword", "admin123");
    await page.click("#loginButton");
    await page.waitForTimeout(800);
    const raw = await expectText("#dashboardRaw", "todayMessages", "dashboard raw");
    return { dashboardLength: raw.length };
  });

  await record("apps page loads demo app", async () => {
    await page.click('button[data-tab="apps"]');
    await page.click('button[data-action="load-apps"]');
    await page.waitForTimeout(500);
    await expectText("#appsTable", "demo", "apps table");
  });

  await record("channels page and health load", async () => {
    await page.click('button[data-tab="channels"]');
    await page.click('button[data-action="load-channels"]');
    await page.click('button[data-action="load-channel-health"]');
    await page.waitForTimeout(500);
    const table = await page.locator("#channelsTable").innerText();
    const health = await page.locator("#channelHealth").innerText();
    if (!table.length) {
      throw new Error("channels table did not render");
    }
    if (!health.includes("[") && !health.includes("channelCode")) {
      throw new Error("channel health did not render JSON");
    }
    return { tableLength: table.length, healthLength: health.length };
  });

  await record("template preview renders variables", async () => {
    await page.click('button[data-tab="templates"]');
    await page.click('button[data-action="preview-template"]');
    await page.waitForTimeout(500);
    const preview = await page.locator("#templatePreview").innerText();
    if (!preview.includes("张三") || !preview.includes("SO202606010001")) {
      throw new Error("template preview missing expected rendered data");
    }
    return { previewLength: preview.length };
  });

  await record("route simulation renders result", async () => {
    await page.click('button[data-tab="routes"]');
    await page.click('button[data-action="simulate-route"]');
    await page.waitForTimeout(500);
    const preview = await page.locator("#routePreview").innerText();
    if (!preview.includes("channelCodes") || !preview.includes("matchedRules")) {
      throw new Error("route simulation output missing expected fields");
    }
    return { previewLength: preview.length };
  });

  await record("send test accepts message", async () => {
    await page.click('button[data-tab="send"]');
    await page.click('button[data-action="send-message"]');
    await page.waitForTimeout(1000);
    const sendJson = await page.locator("#sendJson").inputValue();
    const sendResult = await page.locator("#sendResult").innerText();
    const requestId = JSON.parse(sendJson).requestId;
    if (!sendResult.includes("accepted")) {
      throw new Error(`message was not accepted: ${sendResult}`);
    }
    report.generatedRequestId = requestId;
    return { requestId };
  });

  await record("messages query finds sent request", async () => {
    await page.click('button[data-tab="messages"]');
    await page.fill("#filterRequestId", report.generatedRequestId);
    await page.fill("#filterStatus", "");
    await page.click('button[data-action="load-messages"]');
    await page.waitForTimeout(800);
    await expectText("#messagesTable", report.generatedRequestId, "messages table");
  });

  await record("message maintenance controls work", async () => {
    await page.fill("#purgeBefore", "2026-01-01T00:00:00Z");
    await page.click('button[data-action="preview-purge"]');
    await page.waitForTimeout(500);
    let detail = await page.locator("#messageDetail").innerText();
    if (!detail.includes("messages") || !detail.includes("sendLogs")) {
      throw new Error("purge preview missing message/log counts");
    }
    await page.fill("#bulkMessageIds", "missing-playwright-e2e-id");
    await page.click('button[data-action="bulk-retry"]');
    await page.waitForTimeout(500);
    detail = await page.locator("#messageDetail").innerText();
    if (!detail.includes("missing-playwright-e2e-id") || !detail.includes("failed")) {
      throw new Error("bulk retry result missing failed item detail");
    }
  });

  await record("system status loads diagnostics", async () => {
    await page.click('button[data-tab="system"]');
    await page.click('button[data-action="load-system"]');
    await page.waitForTimeout(500);
    const raw = await page.locator("#systemRaw").innerText();
    if (!raw.includes("warnings") || !raw.includes("worker")) {
      throw new Error("system status missing warnings or worker fields");
    }
    return { rawLength: raw.length };
  });

  await record("users and audit pages load", async () => {
    await page.click('button[data-tab="users"]');
    await page.click('button[data-action="load-users"]');
    await page.waitForTimeout(500);
    await expectText("#usersTable", "admin", "users table");
    await page.click('button[data-tab="audit"]');
    await page.click('button[data-action="load-audit"]');
    await page.waitForTimeout(500);
    await expectText("#auditTable", "ADMIN_LOGIN", "audit table");
  });

  await page.screenshot({ path: "reports/e2e-page-final.png", fullPage: true });
  report.screenshot = "reports/e2e-page-final.png";
  report.finishedAt = new Date().toISOString();
  report.summary = {
    total: report.cases.length,
    passed: report.cases.filter((item) => item.status === "PASS").length,
    failed: report.cases.filter((item) => item.status === "FAIL").length,
    consoleErrors: report.consoleErrors.length
  };
  return report;
}
