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
  reporter: process.env.CI ? "github" : "html",

  // Global settings
  use: {
    baseURL: process.env.BASE_URL || "https://localhost",
    ignoreHTTPSErrors: true,

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
