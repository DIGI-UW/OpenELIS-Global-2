#!/usr/bin/env node

import { execFileSync, execSync } from "child_process";
import fs from "fs";
import path from "path";

const frontendDir = process.cwd();
const blobDir = path.join(frontendDir, "blob-report");
const htmlDir = path.join(frontendDir, "playwright-report");
const resultsDir = path.join(frontendDir, "test-results");

if (!fs.existsSync(resultsDir)) {
  console.error("ERROR: test-results directory does not exist.");
  process.exit(1);
}

const blobEntries = fs.existsSync(blobDir) ? fs.readdirSync(blobDir) : [];
if (blobEntries.length > 0) {
  execSync("npx playwright merge-reports --reporter html ./blob-report", {
    stdio: "inherit",
  });
}

if (!fs.existsSync(path.join(htmlDir, "index.html"))) {
  console.error(
    "ERROR: playwright-report/index.html not found. Run Playwright tests first.",
  );
  process.exit(1);
}

try {
  execSync("command -v zip >/dev/null 2>&1", {
    stdio: "ignore",
    shell: "/bin/bash",
  });
} catch {
  console.error(
    "ERROR: zip CLI is required to create the Playwright evidence bundle.",
  );
  process.exit(1);
}

const stamp = new Date()
  .toISOString()
  .replace(/[-:]/g, "")
  .replace("T", "-")
  .slice(0, 15);
const bundlePrefix =
  process.env.PW_BUNDLE_REPORT_PREFIX ||
  "analyzer-harness-demo-video-playwright-report";
const zipName = `${bundlePrefix}-${stamp}.zip`;

execFileSync("zip", ["-r", zipName, "playwright-report", "test-results"], {
  stdio: "inherit",
  cwd: frontendDir,
});

console.log(`Created bundle: ${zipName}`);
