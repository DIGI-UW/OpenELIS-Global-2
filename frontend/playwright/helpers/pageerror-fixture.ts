import { test as base } from "@playwright/test";

/**
 * Extended test fixture that captures unhandled page errors.
 *
 * Usage: import { test } from "../helpers/pageerror-fixture" instead of
 * "@playwright/test" in specs where crash telemetry is needed.
 *
 * Page errors are logged to stderr so they appear in CI logs even when
 * the browser process crashes before Playwright can capture a trace.
 */
export const test = base.extend<{ capturePageErrors: void }>({
  capturePageErrors: [
    async ({ page }, use) => {
      const errors: Error[] = [];
      page.on("pageerror", (error) => {
        console.error(`[pageerror] ${error.message}\n${error.stack ?? ""}`);
        errors.push(error);
      });
      await use();
      if (errors.length > 0) {
        console.error(
          `[pageerror] ${errors.length} unhandled error(s) during test`,
        );
      }
    },
    { auto: true },
  ],
});

export { expect } from "@playwright/test";
