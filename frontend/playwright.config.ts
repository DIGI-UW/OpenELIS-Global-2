import { defineConfig, devices } from "@playwright/test";

/**
 * OpenELIS Global Playwright Configuration
 *
 * Projects are organized by environment requirement:
 *
 *   core-app            — CI build stack core UI tests
 *   core-demo           — Build stack UI-only demos
 *   core-demo-video     — Same core demos with slowMo + video (local only)
 *   harness             — Analyzer harness infra tests (bridge, simulator, plugins)
 *   harness-demo        — Harness-backed UI-only demos
 *   harness-demo-video  — Same harness demos with slowMo + video (local only)
 *
 * New test files must be explicitly added to a project's testMatch.
 * Unmatched files won't run — this is intentional (allowlist, not blocklist).
 *
 * @see https://playwright.dev/docs/test-configuration
 */

// Core demos prove non-analyzer user stories on the build stack.
const CORE_DEMO_TESTS = ["**/ogc-284-demo-video.spec.ts"];

// Harness demos prove analyzer/file-import stories that require harness infra.
const HARNESS_DEMO_TESTS = [
  "**/demo-quantstudio*.spec.ts",
  "**/file-import-ui.spec.ts",
  "**/file-import-results.spec.ts",
  "**/astm-genexpert-results.spec.ts",
];

export default defineConfig({
  testDir: "./playwright/tests",

  // Parallelization
  fullyParallel: true,
  workers: process.env.CI ? 2 : undefined,
  shardingMode: "round-robin",

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
    trace: "retain-on-failure",
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

    // Core demo — build-stack UI-only demos validated in CI
    {
      name: "core-demo",
      testMatch: CORE_DEMO_TESTS,
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
      },
      dependencies: ["setup"],
    },

    // Core demo video — same core demos with slowMo and video (local only)
    {
      name: "core-demo-video",
      testMatch: CORE_DEMO_TESTS,
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

    // Harness demo — same UI-only story proof, but only for analyzer-backed flows
    {
      name: "harness-demo",
      testMatch: HARNESS_DEMO_TESTS,
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
      },
      dependencies: ["setup"],
    },

    // Harness demo video — same harness demos with slowMo and overlays (local only)
    {
      name: "harness-demo-video",
      testMatch: HARNESS_DEMO_TESTS,
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
