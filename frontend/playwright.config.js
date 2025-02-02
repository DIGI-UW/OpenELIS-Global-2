const { defineConfig } = require("@playwright/test");

module.exports = defineConfig({
  retries: 1,
  workers: 2, // increasing workers to more than 2 causes tests to fail
  timeout: 20000,
  // try one retry as some tests are flaky
  // reporter: [["html", { outputFolder: "playwright-report", open: "never" }]],
  use: {
    headless: true,
    viewport: { width: 1280, height: 720 },
    // video: "only-on-failure",
    // screenshot: "only-on-failure",
    // trace: "retain-on-failure",
    baseURL: "https://localhost",
    ignoreHTTPSErrors: true,
  },
  projects: [
    {
      name: "E2E",
      testDir: "./playwright/e2e",
      testMatch: "**/*.spec.js",
    },
  ],
  // outputDir: "playwright/test-results/artifacts",
  // preserveOutput: "failures-only",
});
