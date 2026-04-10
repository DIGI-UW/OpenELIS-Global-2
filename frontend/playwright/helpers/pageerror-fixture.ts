import { test as base } from "@playwright/test";
import * as fs from "fs";

/**
 * Extended test fixture that captures unhandled page errors and console messages.
 *
 * Usage: import { test } from "../helpers/pageerror-fixture" instead of
 * "@playwright/test" in specs where crash telemetry is needed.
 *
 * Page errors and console messages are logged to stderr and saved to error-context.md
 * so they appear in CI logs and artifacts even when the browser process crashes.
 */
export const test = base.extend<{ capturePageErrors: void }>({
  capturePageErrors: [
    async ({ page }, use, testInfo) => {
      const errors: Error[] = [];
      const consoleMessages: string[] = [];
      const errorBuffer: string[] = [];

      // Initialize a per-test error context file for persistent logging
      const errorContextPath = testInfo.outputPath(
        `error-context-worker-${testInfo.workerIndex}.md`,
      );

      // Initialize error context file with timestamp and test info
      const timestamp = new Date().toISOString();
      const header =
        `\n\n=== Test Session: ${timestamp} ===\n` +
        `Test: ${testInfo.title}\n` +
        `Worker: ${testInfo.workerIndex}\n`;
      errorBuffer.push(header);

      // Capture unhandled page errors
      page.on("pageerror", (error) => {
        const errorMessage = `[pageerror] ${error.message}\n${error.stack ?? ""}`;
        console.error(errorMessage);
        errors.push(error);

        // Buffer for batch write at end (avoid sync I/O on hot path)
        const logMessage = `Page Error: ${error.message}\n${error.stack || ""}\n`;
        errorBuffer.push(logMessage);
      });

      // Capture console errors and warnings systematically
      page.on("console", (msg) => {
        const type = msg.type();
        if (type === "error" || type === "warning") {
          const text = msg.text();
          const logMessage =
            type === "error"
              ? `Browser Console Error: ${text}`
              : `Browser Console Warning: ${text}`;

          if (type === "error") {
            console.error(logMessage);
          } else {
            console.warn(logMessage);
          }
          consoleMessages.push(logMessage);

          // Buffer for batch write at end (avoid sync I/O on hot path)
          errorBuffer.push(`${logMessage}\n`);
        }
      });

      try {
        await use();

        if (errors.length > 0) {
          console.error(
            `[pageerror] ${errors.length} unhandled error(s) during test`,
          );
        }

        if (consoleMessages.length > 0) {
          console.error(
            `[console] ${consoleMessages.length} console message(s) during test`,
          );
        }

        // Add summary to error context file
        const summary = `\n--- Summary: ${errors.length} page errors, ${consoleMessages.length} console messages ---\n`;
        errorBuffer.push(summary);
      } finally {
        // Batch write all buffered error logs at once
        if (errorBuffer.length > 0) {
          fs.appendFileSync(errorContextPath, errorBuffer.join(""), "utf8");
        }
      }
    },
    { auto: true },
  ],
});

export { expect } from "@playwright/test";
