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
  reporter: process.env.CI ? "blob" : "html",

  // Global settings
  use: {
    baseURL: process.env.BASE_URL || "https://localhost",
    ignoreHTTPSErrors: true,

    // Evidence collection
    trace: "on-first-retry",
    screenshot: "only-on-failure",
    video: process.env.PLAYWRIGHT_VIDEO === "on" ? "on" : "off",
  },

  projects: [
    // Auth setup - runs once, saves session
    {
      name: "setup",
      testMatch: /.*\.setup\.ts/,
    },
    // Core app tests — default stack + seeded FILE fixtures
    {
      name: "core-app",
      testIgnore: [
        /.*analyzer-test-connection\.spec\.ts/,
        /.*analyzer-plugin-config\.spec\.ts/,
        /.*analyzer-simulator\.spec\.ts/,
        /.*analyzer-hl7-simulate\.spec\.ts/,
        /.*demo-quantstudio.*\.spec\.ts/,
      ],
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
      },
      dependencies: ["setup"],
    },
    // Analyzer harness tests — needs bridge + simulator, runs via analyzer-e2e.yml
    {
      name: "analyzer-harness",
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
      },
      dependencies: ["setup"],
      testMatch: [
        "**/analyzer-test-connection*",
        "**/analyzer-plugin-config*",
        "**/analyzer-simulator*",
        "**/analyzer-hl7-simulate*",
        "**/demo-quantstudio*",
      ],
    },
  ],
});
