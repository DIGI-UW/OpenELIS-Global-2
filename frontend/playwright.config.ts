import { defineConfig, devices } from "@playwright/test";
import * as dotenv from "dotenv";

// Load .env from repo root — provides TEST_USER, TEST_PASS, BASE_URL, etc.
// No manual `set -a && . .env` needed.
dotenv.config({ path: new URL("../.env", import.meta.url).pathname });
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
const CORE_DEMO_TESTS = [
  "**/demo/core/**/*.spec.ts",
  "playwright/tests/demo/core/ogc-782-microbiology-mvp.spec.ts",
];

// Core foundational verification on the build stack.
const CORE_FOUNDATIONAL_TESTS = ["**/foundational/core/**/*.spec.ts"];

// Focused WCAG 2.1 AA qualification for stable core-app feature surfaces.
const CORE_ACCESSIBILITY_TESTS = ["**/accessibility/core/**/*.spec.ts"];

// Explicit disposable-stack performance qualification for core-app surfaces.
const CORE_PERFORMANCE_TESTS = ["**/performance/core/**/*.spec.ts"];

// Explicit operator-run verification against a deployed review target.
const CORE_LIVE_UAT_TESTS = ["**/manual-only/core/**/*.spec.ts"];

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
  // Single worker EVERYWHERE, not just CI: all projects share one webapp + one
  // database, so parallel workers interleave writes into shared state that CI
  // (which has always run 1 worker per shard) can never reproduce — local
  // multi-worker runs were a parity gap that produced local-only "mystery"
  // failures. Parallelism comes from CI sharding (--shard=current/total), each
  // shard owning its own full stack. CI also needs 1 worker to avoid OOM
  // browser crashes on GH Actions runners (7GB RAM).
  workers: 1,

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

    // CI stability: prevent Chromium renderer crashes ("Target page closed")
    ...(process.env.CI && {
      launchOptions: {
        args: [
          "--disable-dev-shm-usage", // use /tmp instead of /dev/shm
          "--disable-gpu", // skip GPU compositing (no GPU in CI)
          "--disable-extensions", // no extension overhead
          "--no-first-run", // skip first-run setup
          "--js-flags=--max-old-space-size=1024", // cap V8 heap (Carbon doesn't tree-shake)
        ],
      },
      serviceWorkers: "block", // block SW registration — self-signed certs cause constant SSL errors
    }),
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

    {
      name: "core-accessibility",
      testMatch: CORE_ACCESSIBILITY_TESTS,
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
      },
      dependencies: ["setup"],
    },

    {
      name: "core-accessibility-mobile",
      testMatch: CORE_ACCESSIBILITY_TESTS,
      testIgnore: "**/microbiology-keyboard.spec.ts",
      use: {
        ...devices["Pixel 5"],
        storageState: "playwright/.auth/user.json",
      },
      dependencies: ["setup"],
    },

    {
      name: "core-performance",
      testMatch: CORE_PERFORMANCE_TESTS,
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

    {
      name: "core-live-uat",
      testMatch: CORE_LIVE_UAT_TESTS,
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
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
