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
      const MAX_CONSOLE = 20;

      // Capture console messages — keep a rolling buffer of the last N
      page.on("console", (msg) => {
        if (msg.type() === "error" || msg.type() === "warning") {
          recentConsole.push(`[${msg.type()}] ${msg.text()}`);
          if (recentConsole.length > MAX_CONSOLE) recentConsole.shift();
        }
      });

      // Capture unhandled page errors (replaces old capturePageErrors fixture)
      page.on("pageerror", (error) => {
        console.error(`[pageerror] ${error.message}\n${error.stack ?? ""}`);
      });

      // Capture renderer crashes — this is the key diagnostic
      page.on("crash", () => {
        console.error(
          `[CRASH] Page renderer crashed during: ${testInfo.title}`,
        );
        console.error(`[CRASH] URL at crash: ${page.url()}`);
        console.error(
          `[CRASH] Recent console (last ${recentConsole.length} messages):`,
        );
        for (const msg of recentConsole) {
          console.error(`  ${msg}`);
        }
      });

      // Capture unexpected page close (different from crash)
      page.on("close", () => {
        if (testInfo.status === undefined) {
          // Test still running — page closed unexpectedly
          console.error(
            `[CLOSE] Page closed unexpectedly during: ${testInfo.title}`,
          );
          console.error(`[CLOSE] URL at close: ${page.url()}`);
        }
      });

      // Capture browser disconnect (entire process died)
      browser.on("disconnected", () => {
        console.error(
          `[DISCONNECT] Browser process died during: ${testInfo.title}`,
        );
        console.error(
          `[DISCONNECT] Recent console (last ${recentConsole.length} messages):`,
        );
        for (const msg of recentConsole) {
          console.error(`  ${msg}`);
        }
      });

      // Capture failed network requests (backend health signal)
      page.on("requestfailed", (request) => {
        const failure = request.failure();
        console.error(
          `[NET-FAIL] ${request.method()} ${request.url()} → ${failure?.errorText ?? "unknown"}`,
        );
      });

      await use();
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
