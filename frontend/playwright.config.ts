import { defineConfig, devices } from "@playwright/test";

/**
 * OpenELIS Global Playwright Configuration
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: "./playwright/tests",

  // Parallelization
  fullyParallel: true,
  workers: process.env.CI ? 1 : undefined,

  // CI safeguards
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,

  // Timeouts
  timeout: 30_000,
  expect: { timeout: 5_000 },

  // Reporting
  reporter: process.env.CI
    ? [
        ["github"],
        ["json", { outputFile: "playwright-report/results.json" }],
        ["html", { outputFolder: "playwright-report", open: "never" }],
      ]
    : [
        ["list"],
        ["html", { outputFolder: "playwright-report", open: "never" }],
      ],

  // Keep artifacts in a consistent location for parity pipelines.
  outputDir: "test-results/playwright",

  // Global settings
  use: {
    baseURL: process.env.BASE_URL || "https://localhost",
    ignoreHTTPSErrors: true,
    testIdAttribute: "data-testid",

    // Evidence collection
    trace: "on-first-retry",
    screenshot: "only-on-failure",
    video: "off",
  },

  projects: [
    // Auth setup - runs once, saves session
    {
      name: "setup",
      testMatch: /.*\.setup\.ts/,
    },
    // Main tests - depend on auth
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
      },
      dependencies: ["setup"],
    },
  ],
});
