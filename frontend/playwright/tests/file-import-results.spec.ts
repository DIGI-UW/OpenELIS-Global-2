import { test, expect } from "@playwright/test";
import * as fs from "fs";
import * as path from "path";
import { showTitleCard, showStepCard } from "../helpers/title-card";

/**
 * FILE Import → Results E2E Tests (Parameterized)
 *
 * Demonstrates the full FILE analyzer workflow for each analyzer:
 *   1. View analyzer configuration (name, type, protocol)
 *   2. View file import settings (directory, pattern, column mappings)
 *   3. Copy a results file into the analyzer's watched directory
 *   4. Wait for FileImportWatchService to process it (polls every 60s)
 *   5. Navigate to Analyzer Results and verify imported data appears
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
    test.setTimeout(240_000); // 4 min — accounts for config viewing + 60s poll

    test(`full flow: config → file drop → results (${fileExtension})`, async ({
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

      // Verify fixture exists
      expect(fs.existsSync(FIXTURE_FILE)).toBeTruthy();

      // ── Title Card ─────────────────────────────────────────────────
      await showTitleCard(
        page,
        `${analyzer.name} — File Import`,
        `Full flow: Configuration → Import → Results (${fileExtension})`,
      );

      // ── Step 1: Navigate to analyzer list ──────────────────────────
      await showStepCard(page, 1, "Navigate to Analyzer Dashboard");

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
      await expect(page.locator("tbody tr").first()).toBeVisible({
        timeout: 10_000,
      });
      await page.waitForTimeout(1_500);

      // Search for this analyzer
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

      // ── Step 2: View analyzer configuration ────────────────────────
      await showStepCard(page, 2, "View Analyzer Configuration");

      // Click overflow menu on the analyzer row
      const overflowMenu = analyzerRow
        .first()
        .locator('[aria-label="Actions"], .cds--overflow-menu');
      await overflowMenu.click();
      await page.waitForTimeout(500);

      // Click "Edit" to open the analyzer form
      const editButton = page.locator(`[data-testid^="analyzer-action-edit-"]`);
      await editButton.click();
      await page.waitForTimeout(2_000);

      // The AnalyzerForm modal should be visible — show it for the video
      const analyzerFormModal = page.locator(".cds--modal.is-visible");
      if (
        await analyzerFormModal
          .first()
          .isVisible({ timeout: 5_000 })
          .catch(() => false)
      ) {
        console.log("Analyzer form modal visible");
        await page.waitForTimeout(3_000); // Pause for video viewers

        // Close the modal
        const closeButton = analyzerFormModal.locator(
          'button[aria-label="close"], .cds--modal-close',
        );
        if (
          await closeButton
            .first()
            .isVisible({ timeout: 2_000 })
            .catch(() => false)
        ) {
          await closeButton.first().click();
        } else {
          await page.keyboard.press("Escape");
        }
        await page.waitForTimeout(1_000);
      }

      // ── Step 3: View file import configuration ─────────────────────
      await showStepCard(page, 3, "View File Import Configuration");

      // Re-open overflow menu
      await overflowMenu.click();
      await page.waitForTimeout(500);

      // Click "Configure File Import"
      const fileImportButton = page.locator(
        `[data-testid^="analyzer-action-file-import-"]`,
      );
      await fileImportButton.click();
      await page.waitForTimeout(2_000);

      // The FileImportConfiguration modal should be visible
      const fileImportModal = page.locator(".cds--modal.is-visible");
      if (
        await fileImportModal
          .first()
          .isVisible({ timeout: 5_000 })
          .catch(() => false)
      ) {
        console.log("File import config modal visible");
        // Scroll down slowly to show all fields in the video
        await page.waitForTimeout(2_000);
        const modalContent = fileImportModal.locator(
          ".cds--modal-content, .cds--modal-container",
        );
        if (
          await modalContent
            .first()
            .isVisible({ timeout: 1_000 })
            .catch(() => false)
        ) {
          await modalContent
            .first()
            .evaluate((el) => el.scrollBy({ top: 300, behavior: "smooth" }));
        }
        await page.waitForTimeout(2_000);

        // Close the modal
        const closeButton = fileImportModal.locator(
          'button[aria-label="close"], .cds--modal-close',
        );
        if (
          await closeButton
            .first()
            .isVisible({ timeout: 2_000 })
            .catch(() => false)
        ) {
          await closeButton.first().click();
        } else {
          await page.keyboard.press("Escape");
        }
        await page.waitForTimeout(1_000);
      }

      // ── Step 4: Copy file into watched directory ───────────────────
      await showStepCard(
        page,
        4,
        `Drop ${fileExtension} file into watched directory`,
      );

      const timestamp = Date.now();
      const destFilename = `${analyzer.filePrefix}${timestamp}${fileExtension}`;
      const destPath = path.join(HOST_IMPORT_DIR, destFilename);

      fs.copyFileSync(FIXTURE_FILE, destPath);
      console.log(`Copied fixture to: ${destPath}`);
      expect(fs.existsSync(destPath)).toBeTruthy();
      await page.waitForTimeout(2_000);

      // ── Step 5: Wait for FileImportWatchService to process ─────────
      await showStepCard(
        page,
        5,
        "Waiting for FileImportWatchService (polls every 60s)...",
      );

      let fileProcessed = false;
      const maxWaitMs = 120_000;
      const pollIntervalMs = 5_000;
      let elapsed = 0;

      console.log("Waiting for FileImportWatchService to process file...");

      while (elapsed < maxWaitMs) {
        if (!fs.existsSync(destPath)) {
          console.log(`File processed after ${elapsed / 1000}s`);
          fileProcessed = true;
          break;
        }
        await page.waitForTimeout(pollIntervalMs);
        elapsed += pollIntervalMs;

        if (elapsed % 15_000 === 0) {
          console.log(`  Still waiting... (${elapsed / 1000}s elapsed)`);
        }
      }

      if (!fileProcessed) {
        console.log(
          "File not moved after timeout — checking API for results anyway",
        );
      }

      await page.waitForTimeout(2_000);

      // ── Step 6: Navigate to Analyzer Results page ──────────────────
      await showStepCard(page, 6, "View Imported Results");

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
          }
        } catch {
          // Non-JSON response
        }
      }
      await page.waitForTimeout(3_000);

      // ── Step 7: Verify results appear ──────────────────────────────
      const resultsTable = page.locator("table, .orderLegendBody");
      await expect(resultsTable.first()).toBeVisible({ timeout: 15_000 });

      // Check for sample accession numbers — MUST find at least one
      const sampleLocators = analyzer.sampleIds.map((id) => page.getByText(id));
      const anyResult = sampleLocators.reduce((acc, loc) => acc.or(loc));

      await expect(anyResult.first()).toBeVisible({ timeout: 15_000 });
      console.log(
        `Results found in Analyzer Results page for ${analyzer.name}!`,
      );

      // Scroll through results for the video
      await page.waitForTimeout(2_000);
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
