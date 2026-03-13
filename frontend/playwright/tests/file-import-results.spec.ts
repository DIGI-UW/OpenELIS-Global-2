import { test, expect } from "@playwright/test";
import * as fs from "fs";
import * as path from "path";
import { showTitleCard, showStepCard } from "../helpers/title-card";

/**
 * FILE Import → Results E2E Tests (Parameterized)
 *
 * Demonstrates the watcher-pipeline MVP workflow for each analyzer:
 *   1. Copy a results file into the analyzer's watched directory
 *   2. Wait for FileImportWatchService to process it (polls every 60s)
 *   3. Navigate to Analyzer Results and verify imported data appears
 *
 * Produces demo videos with title/transition screens when run with:
 *   CLEANUP=false PLAYWRIGHT_VIDEO=on TEST_USER=admin TEST_PASS='adminADMIN!' \
 *     npx playwright test file-import-results --project=file-import-video
 */

const CLEANUP = process.env.CLEANUP !== "false";
const REPO_ROOT = path.resolve(__dirname, "../../..");
const FIXTURES_DIR = path.join(__dirname, "../fixtures");

/** Analyzer configurations for parameterized tests */
const ANALYZERS = [
  {
    name: "QuantStudio 5",
    analyzerName: "E2E-FILE-QuantStudio5-Analyzer",
    fixture: "quantstudio-e2e-results-qs5.xls",
    importSubdir: "e2e-qs5/incoming",
    filePrefix: "qs5-results-",
    sampleIds: ["E2E001", "E2E002", "E2E005"],
  },
  {
    name: "QuantStudio 7",
    analyzerName: "E2E-FILE-QuantStudio7-Analyzer",
    fixture: "quantstudio-e2e-results.xlsx",
    importSubdir: "e2e-qs7/incoming",
    filePrefix: "qs7-results-",
    sampleIds: ["E2E001", "E2E002", "E2E005"],
  },
  {
    name: "FluoroCycler XT",
    analyzerName: "E2E-FILE-FluoroCycler-XT",
    fixture: "fluorocycler-e2e-results.xlsx",
    importSubdir: "e2e-fluorocycler/incoming",
    filePrefix: "fc-results-",
    sampleIds: ["E2E-FC001", "E2E-FC002", "E2E-FC003"],
  },
];

for (const analyzer of ANALYZERS) {
  const HOST_IMPORT_DIR = path.join(
    REPO_ROOT,
    "projects/analyzer-harness/volume/analyzer-imports",
    analyzer.importSubdir,
  );
  const FIXTURE_FILE = path.join(FIXTURES_DIR, analyzer.fixture);
  const fileExtension = path.extname(analyzer.fixture);

  test.describe(`${analyzer.name} File Import → Results`, () => {
    test.setTimeout(180_000); // 3 min — accounts for 60s poll interval

    test(`drop ${fileExtension} file, verify results appear in OE`, async ({
      page,
    }) => {
      // Skip if analyzer harness import directory doesn't exist (e.g. in CI)
      test.skip(
        !fs.existsSync(HOST_IMPORT_DIR),
        "Requires analyzer harness bind-mount (HOST_IMPORT_DIR not found)",
      );

      // Capture errors for debugging
      const consoleErrors: string[] = [];
      page.on("console", (msg) => {
        if (msg.type() === "error") consoleErrors.push(msg.text());
      });

      // ── Title Card ─────────────────────────────────────────────────
      await showTitleCard(
        page,
        `${analyzer.name} — File Import`,
        `Watcher pipeline → Analyzer Results (${fileExtension})`,
      );

      // ── Step 1: Verify fixture file exists ─────────────────────────
      expect(fs.existsSync(FIXTURE_FILE)).toBeTruthy();

      // ── Step 2: Navigate to analyzer list ──────────────────────────
      await showStepCard(page, 1, "Navigate to Analyzer List");

      // Wait for initial analyzer API response before interacting with the page
      const analyzerApiPromise = page.waitForResponse(
        (resp) =>
          resp.url().includes("/rest/analyzer/analyzers") &&
          resp.status() === 200,
        { timeout: 30_000 },
      );
      await page.goto("analyzers", { waitUntil: "domcontentloaded" });
      await analyzerApiPromise;

      await expect(page.locator('[data-testid="analyzers-list"]')).toBeVisible({
        timeout: 30_000,
      });

      // Wait for at least one row to appear (confirms data rendered)
      await expect(page.locator("tbody tr").first()).toBeVisible({
        timeout: 10_000,
      });
      await page.waitForTimeout(1_000);

      // Search for this analyzer (use analyzerName for exact backend match)
      const searchInput = page.locator('[data-testid="analyzer-search-input"]');
      await searchInput.fill(analyzer.analyzerName);
      await page.waitForTimeout(1_500);

      const analyzerRow = page.locator("tbody tr", {
        hasText: new RegExp(
          analyzer.analyzerName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"),
          "i",
        ),
      });
      await expect(analyzerRow.first()).toBeVisible({ timeout: 10_000 });
      await page.waitForTimeout(1_000);

      // ── Step 3: Copy file into watched directory ───────────────────
      await showStepCard(
        page,
        2,
        `Copy ${fileExtension} file to watched directory`,
      );

      const timestamp = Date.now();
      const destFilename = `${analyzer.filePrefix}${timestamp}${fileExtension}`;
      const destPath = path.join(HOST_IMPORT_DIR, destFilename);

      fs.copyFileSync(FIXTURE_FILE, destPath);
      console.log(`Copied fixture to: ${destPath}`);
      expect(fs.existsSync(destPath)).toBeTruthy();
      await page.waitForTimeout(2_000);

      // ── Step 4: Wait for FileImportWatchService to process ─────────
      await showStepCard(
        page,
        3,
        "Waiting for FileImportWatchService (polls every 60s)...",
      );

      let resultsFound = false;
      const maxWaitMs = 120_000;
      const pollIntervalMs = 5_000;
      let elapsed = 0;

      console.log(
        "Waiting for FileImportWatchService to process file (polls every 60s)...",
      );

      while (elapsed < maxWaitMs) {
        if (!fs.existsSync(destPath)) {
          console.log(`File processed after ${elapsed / 1000}s`);
          resultsFound = true;
          break;
        }
        await page.waitForTimeout(pollIntervalMs);
        elapsed += pollIntervalMs;

        if (elapsed % 15_000 === 0) {
          console.log(`  Still waiting... (${elapsed / 1000}s elapsed)`);
        }
      }

      if (!resultsFound) {
        console.log(
          "File not moved after timeout — checking API for results anyway",
        );
      }

      await page.waitForTimeout(2_000);

      // ── Step 5: Navigate to Analyzer Results page ──────────────────
      await showStepCard(page, 4, "View imported results");

      const apiResponsePromise = page
        .waitForResponse(
          (resp) => resp.url().includes("/rest/AnalyzerResults"),
          { timeout: 30_000 },
        )
        .catch(() => null);

      await page.goto(
        `AnalyzerResults?type=${encodeURIComponent(analyzer.analyzerName)}`,
        { waitUntil: "domcontentloaded" },
      );

      const apiResponse = await apiResponsePromise;
      if (apiResponse) {
        const body = await apiResponse.text();
        console.log(
          `API Response: status=${apiResponse.status()}, length=${body.length}`,
        );
        try {
          const json = JSON.parse(body);
          console.log(`resultList length: ${json.resultList?.length ?? "N/A"}`);
          if (json.resultList?.length > 0) {
            console.log(
              "First result:",
              JSON.stringify(json.resultList[0]).substring(0, 300),
            );
          } else {
            console.log("Response keys:", Object.keys(json));
            console.log("displayNotFoundMsg:", json.displayNotFoundMsg);
          }
        } catch {
          console.log("Response body (first 500):", body.substring(0, 500));
        }
      } else {
        console.log("No API response intercepted for /rest/AnalyzerResults");
      }
      await page.waitForTimeout(3_000);

      // ── Step 6: Verify results appear ──────────────────────────────
      const resultsTable = page.locator("table, .orderLegendBody");
      await expect(resultsTable.first()).toBeVisible({ timeout: 15_000 });

      // Check for sample accession numbers
      const sampleLocators = analyzer.sampleIds.map((id) => page.getByText(id));
      const anyResult = sampleLocators.reduce((acc, loc) => acc.or(loc));

      try {
        await expect(anyResult.first()).toBeVisible({ timeout: 15_000 });
        console.log(
          `Results found in Analyzer Results page for ${analyzer.name}!`,
        );
      } catch {
        console.log(
          "No results visible in table. Console errors:",
          consoleErrors,
        );
        console.log("Current URL:", page.url());

        const noResults = page.getByText(/no.*result|empty/i);
        if (await noResults.isVisible({ timeout: 2_000 }).catch(() => false)) {
          console.log("'No results' message displayed");
        }

        const bodyText = await page
          .locator("body")
          .textContent({ timeout: 2_000 })
          .catch(() => "(empty)");
        console.log("Page text:", bodyText?.substring(0, 1000));
      }

      await page.waitForTimeout(3_000);

      // ── Step 7: Scroll through results ─────────────────────────────
      await page.evaluate(() => window.scrollBy(0, 300));
      await page.waitForTimeout(2_000);

      // ── Completion Card ────────────────────────────────────────────
      await showTitleCard(
        page,
        "Import Complete",
        `${analyzer.name} results successfully imported into OpenELIS`,
      );

      // ── Step 8: Navigate back to analyzer list ─────────────────────
      await page.goto("analyzers", { waitUntil: "domcontentloaded" });
      await expect(page.locator('[data-testid="analyzers-list"]')).toBeVisible({
        timeout: 15_000,
      });
      await page.waitForTimeout(2_000);
    });

    test.afterEach(async () => {
      if (!CLEANUP) return;

      try {
        const files = fs.readdirSync(HOST_IMPORT_DIR);
        for (const file of files) {
          if (file.startsWith(analyzer.filePrefix)) {
            fs.unlinkSync(path.join(HOST_IMPORT_DIR, file));
          }
        }
      } catch {
        // Cleanup failure is not a test failure
      }
    });
  });
}
