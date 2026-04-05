/**
 * Shared test base with auto-fixtures for crash diagnostics, memory
 * monitoring, and page error capture.
 *
 * Import { test, expect } from this file instead of @playwright/test.
 *
 * Crash diagnostics:
 *   Captures page.on('crash'), page.on('close'), console errors, and
 *   network failures. Logs everything to stderr so CI preserves it even
 *   when the browser dies before trace/screenshot capture.
 *
 * Memory monitoring:
 *   Logs JS heap usage per test via CDP. Opt-in budget via memoryBudgetMB.
 */

import { test as base, expect } from "@playwright/test";

export const test = base.extend<{
  crashDiagnostics: void;
  memoryMonitor: void;
  memoryBudgetMB: number | undefined;
}>({
  memoryBudgetMB: [undefined, { option: true }],

  crashDiagnostics: [
    async ({ page, browser }, use, testInfo) => {
      const recentConsole: string[] = [];
      const recentNav: string[] = [];
      const MAX_BUFFER = 20;
      let lastUrl = "";
      let navCount = 0;

      function safeUrl(): string {
        try {
          return page.url();
        } catch {
          return lastUrl || "(unknown — page dead)";
        }
      }

      function dumpContext(tag: string) {
        console.error(`[${tag}] Test: ${testInfo.title}`);
        console.error(`[${tag}] URL: ${safeUrl()} (nav #${navCount})`);
        if (recentNav.length) {
          console.error(`[${tag}] Recent navigations:`);
          for (const n of recentNav) console.error(`  ${n}`);
        }
        if (recentConsole.length) {
          console.error(`[${tag}] Recent console:`);
          for (const c of recentConsole) console.error(`  ${c}`);
        }
      }

      // Named handlers so we can remove them in teardown
      const onFrameNavigated = (frame: import("@playwright/test").Frame) => {
        if (frame === page.mainFrame()) {
          navCount++;
          lastUrl = frame.url();
          recentNav.push(
            `#${navCount} ${new Date().toISOString()} → ${lastUrl}`,
          );
          if (recentNav.length > MAX_BUFFER) recentNav.shift();
        }
      };
      const onConsole = (msg: import("@playwright/test").ConsoleMessage) => {
        if (msg.type() === "error" || msg.type() === "warning") {
          recentConsole.push(`[${msg.type()}] ${msg.text()}`);
          if (recentConsole.length > MAX_BUFFER) recentConsole.shift();
        }
      };
      const onPageError = (error: Error) => {
        console.error(`[pageerror] ${error.message}\n${error.stack ?? ""}`);
      };
      const onCrash = () => dumpContext("CRASH");
      const onClose = () => {
        if (testInfo.status === undefined) dumpContext("CLOSE");
      };
      const onDisconnect = () => dumpContext("DISCONNECT");
      const onRequestFailed = (request: import("@playwright/test").Request) => {
        const failure = request.failure();
        console.error(
          `[NET-FAIL] ${request.method()} ${request.url()} → ${failure?.errorText ?? "unknown"}`,
        );
      };

      // Capture HTTP 500+ responses with URL path (strip query params to
      // avoid leaking sensitive data like patient IDs into CI logs)
      const onResponse = (response: import("@playwright/test").Response) => {
        if (response.status() >= 500) {
          const url = new URL(response.url());
          console.error(
            `[HTTP-${response.status()}] ${response.request().method()} ${url.pathname}`,
          );
        }
      };

      page.on("framenavigated", onFrameNavigated);
      page.on("console", onConsole);
      page.on("pageerror", onPageError);
      page.on("crash", onCrash);
      page.on("close", onClose);
      browser.once("disconnected", onDisconnect); // once — browser is worker-scoped
      page.on("requestfailed", onRequestFailed);
      page.on("response", onResponse);

      try {
        await use();
      } finally {
        page.off("framenavigated", onFrameNavigated);
        page.off("console", onConsole);
        page.off("pageerror", onPageError);
        page.off("crash", onCrash);
        page.off("close", onClose);
        page.off("requestfailed", onRequestFailed);
        page.off("response", onResponse);
      }
    },
    { auto: true },
  ],

  memoryMonitor: [
    async ({ page, browserName, memoryBudgetMB }, use, testInfo) => {
      if (browserName !== "chromium") {
        await use();
        return;
      }

      let cdp: Awaited<
        ReturnType<typeof page.context.prototype.newCDPSession>
      > | null = null;
      try {
        cdp = await page.context().newCDPSession(page);
        await cdp.send("Performance.enable");
      } catch {
        await use();
        return;
      }

      await use();

      try {
        const { metrics } = await cdp.send("Performance.getMetrics");
        const heapUsed = metrics.find(
          (m: { name: string }) => m.name === "JSHeapUsedSize",
        );
        const heapTotal = metrics.find(
          (m: { name: string }) => m.name === "JSHeapTotalSize",
        );
        if (heapUsed && heapTotal) {
          const usedMB = (heapUsed.value / (1024 * 1024)).toFixed(1);
          const totalMB = (heapTotal.value / (1024 * 1024)).toFixed(1);
          console.error(
            `[memory] ${testInfo.title}: ${usedMB}MB used / ${totalMB}MB total`,
          );

          if (memoryBudgetMB !== undefined) {
            expect(
              heapUsed.value / (1024 * 1024),
              `Heap budget exceeded: ${usedMB}MB > ${memoryBudgetMB}MB`,
            ).toBeLessThan(memoryBudgetMB);
          }
        }
      } catch {
        console.error(
          `[memory] ${testInfo.title}: CDP unavailable (browser likely crashed)`,
        );
      }

      await cdp.detach().catch(() => {});
    },
    { auto: true },
  ],
});

export { expect };
