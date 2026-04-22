import { test as base } from "@playwright/test";
import * as fs from "fs";
import * as path from "path";

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
    async ({ page }, use) => {
      const errors: Error[] = [];
      const consoleMessages: string[] = [];

      // Path for error context file
      const errorContextPath = path.join(process.cwd(), "error-context.md");

      // Initialize error context file with timestamp
      const timestamp = new Date().toISOString();
      const header = `\n\n=== Test Session: ${timestamp} ===\n`;
      fs.appendFileSync(errorContextPath, header, "utf8");

      // Capture unhandled page errors
      page.on("pageerror", (error) => {
        const errorMessage = `[pageerror] ${error.message}\n${error.stack ?? ""}`;
        console.error(errorMessage);
        errors.push(error);

        // Save to file
        const logMessage = `Page Error: ${error.message}\n${error.stack || ""}\n`;
        fs.appendFileSync(errorContextPath, logMessage, "utf8");
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

          console.log(logMessage); // appears in terminal
          consoleMessages.push(logMessage);

          // Save to file for artifact
          fs.appendFileSync(errorContextPath, `${logMessage}\n`, "utf8");
        }
      });

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
      fs.appendFileSync(errorContextPath, summary, "utf8");
    },
    { auto: true },
  ],
});

export { expect } from "@playwright/test";
