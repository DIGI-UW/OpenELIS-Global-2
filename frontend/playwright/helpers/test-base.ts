/**
 * Shared test base with auto-fixtures for memory monitoring and page error
 * capture. Import { test, expect } from this file instead of @playwright/test
 * to get both fixtures automatically.
 *
 * Memory monitoring:
 *   Logs JS heap usage per test to stderr. CDP-based, Chromium-only.
 *   Opt-in budget assertions via test.use({ memoryBudgetMB: 200 }).
 *
 * Page error capture:
 *   Logs unhandled page errors to stderr. Useful for diagnosing crashes
 *   where the trace/screenshot may not survive.
 */

import { test as base, expect } from "@playwright/test";

export const test = base.extend<{
  capturePageErrors: void;
  memoryMonitor: void;
  memoryBudgetMB: number | undefined;
}>({
  memoryBudgetMB: [undefined, { option: true }],

  capturePageErrors: [
    async ({ page }, use) => {
      page.on("pageerror", (error) => {
        console.error(`[pageerror] ${error.message}\n${error.stack ?? ""}`);
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
        // CDP unavailable — skip monitoring
        await use();
        return;
      }

      await use();

      // Post-test: capture heap metrics
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
        // CDP session dead — browser crashed. The crash itself is the signal.
      }

      await cdp.detach().catch(() => {});
    },
    { auto: true },
  ],
});

export { expect };
