import { defineConfig, devices } from "@playwright/test";
import * as path from "path";

// Load .env from repo root — provides TEST_USER, TEST_PASS, BASE_URL, etc.
// No manual `set -a && . .env` needed.
require("dotenv").config({ path: path.resolve(__dirname, "../.env") });

/**
 * OpenELIS Global Playwright Configuration
 *
 * Tests are classified along three axes:
 *
 * 1) Runtime: core vs harness
 * 2) Intent: demo (story proof) vs foundational (functional verification)
 * 3) Execution policy: ci-safe vs manual-only
 *
 * New test files must be explicitly added to exactly one bucket.
 *
 * @see https://playwright.dev/docs/test-configuration
 */

// Demo story proof on the build stack (video-ready).
const CORE_DEMO_TESTS = ["**/demo/core/**/*.spec.ts"];

// Core foundational verification (ci-safe).
const CORE_FOUNDATIONAL_TESTS = ["**/foundational/core/**/*.spec.ts"];

// Harness demo story proof (video-ready).
const HARNESS_DEMO_TESTS = ["**/demo/harness/**/*.spec.ts"];

// Harness foundational verification (ci-safe).
const HARNESS_FOUNDATIONAL_TESTS = ["**/foundational/harness/**/*.spec.ts"];

// Manual-only harness coverage (real hardware or operator-managed infra).
const HARNESS_MANUAL_ONLY_TESTS = [
  "**/manual-only/harness/analyzer-test-connection-manual-only.spec.ts",
];

export default defineConfig({
  testDir: "./playwright/tests",

  // Parallelization
  fullyParallel: true,
  workers: process.env.CI ? 2 : undefined,
  // Shard tests in CI via CLI: --shard=current/total (see e.g. analyzer-e2e workflow)

  // CI safeguards
  forbidOnly: !!process.env.CI,
  // CI must not mask failures through reruns.
  retries: 0,

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

    // Core foundational verification — runs on CI build stack.
    {
      name: "core-app",
      testMatch: CORE_FOUNDATIONAL_TESTS,
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

    // Analyzer-stack demo story proof (CI: reusable harness workflow only).
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

    // Analyzer-stack foundational verification (non-demo, ci-safe).
    {
      name: "harness-foundational",
      testMatch: HARNESS_FOUNDATIONAL_TESTS,
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
      },
      dependencies: ["setup"],
    },

    // Real-hardware / manual-only coverage (excluded from standard CI jobs).
    {
      name: "harness-manual-only",
      testMatch: HARNESS_MANUAL_ONLY_TESTS,
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
      },
      dependencies: ["setup"],
    },
  ],
});
