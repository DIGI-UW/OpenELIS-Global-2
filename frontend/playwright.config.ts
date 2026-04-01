import { defineConfig, devices } from "@playwright/test";
import * as path from "path";

// Load .env from repo root — provides TEST_USER, TEST_PASS, BASE_URL, etc.
// No manual `set -a && . .env` needed.
require("dotenv").config({ path: path.resolve(__dirname, "../.env") });

/**
 * OpenELIS Global Playwright Configuration
 *
 * Projects are organized by environment requirement:
 *
 *   core-app            — CI build stack (no plugins, bridge, or import dirs)
 *   core-demo           — UI workflow demos provable on build stack + fixtures
 *   core-demo-video     — core-demo with slowMo + video (local recording)
 *   harness-demo        — Analyzer-stack UI tests requiring full harness
 *   harness-demo-video  — harness-demo with slowMo + video (local recording)
 *
 * New test files must be explicitly added to a project's testMatch.
 * Unmatched files won't run — this is intentional (allowlist, not blocklist).
 *
 * @see https://playwright.dev/docs/test-configuration
 */

// Demos that must run on the minimal build stack only (see e2e-playwright.yml)
const CORE_DEMO_TESTS = ["**/ogc-284-barcode-workflow.spec.ts"];

// Analyzer-stack UI tests that require harness overlay + seeded analyzers
const HARNESS_DEMO_TESTS = [
  "**/analyzer-test-connection.spec.ts",
  "**/analyzer-plugin-config.spec.ts",
  "**/analyzer-simulator.spec.ts",
  "**/demo-quantstudio*.spec.ts",
  "**/file-import-ui.spec.ts",
  "**/file-import-results.spec.ts",
  "**/astm-genexpert-results.spec.ts",
  "**/analyzer-demo-flow.spec.ts",
];

export default defineConfig({
  testDir: "./playwright/tests",

  // Parallelization
  fullyParallel: true,
  workers: process.env.CI ? 2 : undefined,
  // Shard tests in CI via CLI: --shard=current/total (see e.g. analyzer-e2e workflow)

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

    // Analyzer-stack UI tests (CI: reusable harness workflow only)
    {
      name: "harness-demo",
      testMatch: HARNESS_DEMO_TESTS,
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
      },
      dependencies: ["setup"],
    },
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
