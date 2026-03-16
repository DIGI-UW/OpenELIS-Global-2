import { defineConfig, devices } from "@playwright/test";

/**
 * OpenELIS Global Playwright Configuration
 *
 * Projects are organized by environment requirement:
 *
 *   core-app      — CI build stack (no plugins, bridge, or import dirs)
 *   harness       — Analyzer harness infra tests (bridge, simulator, plugins)
 *   demo          — End-to-end workflow demos (run on harness, normal speed)
 *   demo-video    — Same demo tests with slowMo + video (local recording only)
 *
 * New test files must be explicitly added to a project's testMatch.
 * Unmatched files won't run — this is intentional (allowlist, not blocklist).
 *
 * @see https://playwright.dev/docs/test-configuration
 */

// Demo workflow tests — shared between demo and demo-video projects
const DEMO_TESTS = [
  "**/demo-quantstudio*.spec.ts",
  "**/file-import-ui.spec.ts",
  "**/file-import-results.spec.ts",
  "**/astm-genexpert-results.spec.ts",
];

export default defineConfig({
  testDir: "./playwright/tests",

  // Parallelization
  fullyParallel: true,
  workers: process.env.CI ? 1 : undefined,

  // CI safeguards
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,

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
    // Auth setup — runs once, saves session state
    {
      name: "setup",
      testMatch: /.*\.setup\.ts/,
    },

    // Core app — runs on CI build stack (no plugins, bridge, or import dirs)
    {
      name: "core-app",
      testMatch: [
        "**/analyzer-form.spec.ts",
        "**/analyzer-list.spec.ts",
        "**/analyzer-navigation.spec.ts",
        "**/error-dashboard.spec.ts",
        "**/navbar.spec.ts",
        "**/sidenav.spec.ts",
      ],
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
      },
      dependencies: ["setup"],
    },

    // Harness — infrastructure tests needing bridge, simulator, plugins
    {
      name: "harness",
      testMatch: [
        "**/analyzer-test-connection.spec.ts",
        "**/analyzer-plugin-config.spec.ts",
        "**/analyzer-simulator.spec.ts",
        "**/file-import.spec.ts",
      ],
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
      },
      dependencies: ["setup"],
    },

    // Demo — workflow tests at normal speed (CI validation on harness)
    {
      name: "demo",
      testMatch: DEMO_TESTS,
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
      },
      dependencies: ["setup"],
    },

    // Demo video — same tests with slowMo for watchable recordings (local only)
    {
      name: "demo-video",
      testMatch: DEMO_TESTS,
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
        video: "on",
        launchOptions: {
          slowMo: parseInt(process.env.PLAYWRIGHT_SLOWMO || "500"),
        },
      },
      dependencies: ["setup"],
    },
  ],
});
