const { defineConfig } = require("@playwright/test");

module.exports = defineConfig({
  timeout: 30000,
  retries: 1,
  use: {
    viewport: { width: 1280, height: 700 },
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
