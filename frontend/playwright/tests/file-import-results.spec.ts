import { test, expect } from "@playwright/test";
import * as fs from "fs";
import * as path from "path";
import { showTitleCard, showStepCard } from "../helpers/title-card";

/**
 * FILE Import → Results E2E Tests (Parameterized)
 *
 * Demonstrates the full FILE analyzer MVP workflow for each analyzer:
 *   1. Create a new analyzer via the UI (select plugin type + profile)
 *   2. Configure file import settings (directory, pattern, column mappings)
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
    profileText: "QuantStudio", // text to match in the profile dropdown
    fixture: "quantstudio-e2e-results-qs5.xls",
    importSubdir: "e2e-qs5/incoming",
    importDir: "/data/analyzer-imports/e2e-qs5/incoming",
    archiveDir: "/data/analyzer-imports/e2e-qs5/processed",
    errorDir: "/data/analyzer-imports/e2e-qs5/errors",
    filePattern: "*.xls",
    filePrefix: "qs5-results-",
    columnMappings: JSON.stringify(
      {
        "Sample Name": "sampleId",
        "Target Name": "testCode",
        "Quantity Mean": "result",
        CT: "ctValue",
        "Well Position": "position",
      },
      null,
      2,
    ),
    sampleIds: ["E2E001", "E2E002", "E2E005"],
  },
  {
    name: "QuantStudio 7",
    profileText: "QuantStudio",
    fixture: "quantstudio-e2e-results.xlsx",
    importSubdir: "e2e-qs7/incoming",
    importDir: "/data/analyzer-imports/e2e-qs7/incoming",
    archiveDir: "/data/analyzer-imports/e2e-qs7/processed",
    errorDir: "/data/analyzer-imports/e2e-qs7/errors",
    filePattern: "*.xlsx",
    filePrefix: "qs7-results-",
    columnMappings: JSON.stringify(
      {
        "Sample Name": "sampleId",
        "Target Name": "testCode",
        "Quantity Mean": "result",
        CT: "ctValue",
        "Well Position": "position",
      },
      null,
      2,
    ),
    sampleIds: ["E2E001", "E2E002", "E2E005"],
  },
  {
    name: "FluoroCycler XT",
    profileText: "FluoroCycler",
    fixture: "fluorocycler-e2e-results.xlsx",
    importSubdir: "e2e-fluorocycler/incoming",
    importDir: "/data/analyzer-imports/e2e-fluorocycler/incoming",
    archiveDir: "/data/analyzer-imports/e2e-fluorocycler/processed",
    errorDir: "/data/analyzer-imports/e2e-fluorocycler/errors",
    filePattern: "*.xlsx",
    filePrefix: "fc-results-",
    columnMappings: JSON.stringify(
      {
        SampleID: "sampleId",
        TargetName: "testCode",
        WellPosition: "position",
        CP: "result",
        Interpretation: "interpretation",
        RunDate: "testDate",
      },
      null,
      2,
    ),
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
    test.setTimeout(300_000); // 5 min — create + config + 60s poll + results

    let createdAnalyzerName: string;

    test(`full flow: create → configure → import → results (${fileExtension})`, async ({
      page,
    }) => {
      // Skip if analyzer harness import directory doesn't exist (e.g. in CI)
      test.skip(
        !fs.existsSync(HOST_IMPORT_DIR),
        "Requires analyzer harness bind-mount (HOST_IMPORT_DIR not found)",
      );

      // Verify fixture exists
      expect(fs.existsSync(FIXTURE_FILE)).toBeTruthy();

      // Use a unique name to avoid collisions
      createdAnalyzerName = `${analyzer.name} E2E ${Date.now()}`;

      // Capture errors for debugging
      const consoleErrors: string[] = [];
      page.on("console", (msg) => {
        if (msg.type() === "error") consoleErrors.push(msg.text());
      });

      // ── Title Card ─────────────────────────────────────────────────
      await showTitleCard(
        page,
        `${analyzer.name} — File Import MVP`,
        `Create → Configure → Import → Results (${fileExtension})`,
      );

      // ── Step 1: Navigate to analyzer dashboard ─────────────────────
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
      await page.waitForTimeout(1_500);

      // ── Step 2: Create analyzer ────────────────────────────────────
      await showStepCard(page, 2, `Create ${analyzer.name} Analyzer`);

      const addButton = page.locator('[data-testid="add-analyzer-button"]');
      await expect(addButton).toBeVisible({ timeout: 5_000 });
      await addButton.click();

      const analyzerForm = page.locator('[data-testid="analyzer-form"]');
      await expect(analyzerForm).toBeVisible({ timeout: 10_000 });
      await page.waitForTimeout(1_000);

      // Fill name
      const nameInput = page.locator(
        '[data-testid="analyzer-form-name-input"]',
      );
      await nameInput.fill(createdAnalyzerName);
      await page.waitForTimeout(500);

      // Select plugin type — Generic File (FILE)
      const pluginTypeDropdown = page.locator(
        '[data-testid="analyzer-form-plugin-type-dropdown"]',
      );
      await pluginTypeDropdown.click();
      await page.waitForTimeout(500);

      const filePluginOption = page
        .locator('[role="option"]')
        .filter({ hasText: /Generic File.*FILE|FILE.*Generic File/i });
      await expect(filePluginOption.first()).toBeVisible({ timeout: 3_000 });
      await filePluginOption.first().click();
      await page.waitForTimeout(1_000);

      // Select default config profile (QuantStudio or FluoroCycler)
      const defaultConfigDropdown = page.locator(
        '[data-testid="analyzer-form-default-config-dropdown"]',
      );
      await expect(defaultConfigDropdown).toBeVisible({ timeout: 5_000 });
      await defaultConfigDropdown.click();
      await page.waitForTimeout(500);

      const profileOption = page
        .locator('[role="option"]')
        .filter({ hasText: new RegExp(analyzer.profileText, "i") });
      await expect(profileOption.first()).toBeVisible({ timeout: 3_000 });
      await profileOption.first().click();
      await page.waitForTimeout(1_000);

      // Select analyzer type — Molecular
      const typeDropdown = page.locator(
        '[data-testid="analyzer-form-type-dropdown"]',
      );
      await typeDropdown.click();
      await page.waitForTimeout(500);

      const molecularOption = page
        .locator('[role="option"]')
        .filter({ hasText: /Molecular/i });
      await expect(molecularOption.first()).toBeVisible({ timeout: 3_000 });
      await molecularOption.first().click();
      await page.waitForTimeout(500);

      // Save the analyzer
      const saveButton = page.locator(
        '[data-testid="analyzer-form-save-button"]',
      );
      await saveButton.click();
      await expect(analyzerForm).toBeHidden({ timeout: 15_000 });
      console.log(`Created analyzer: ${createdAnalyzerName}`);
      await page.waitForTimeout(1_500);

      // ── Step 3: Find analyzer in the list ──────────────────────────
      await showStepCard(page, 3, "Verify Analyzer Created");

      const searchInput = page.locator('[data-testid="analyzer-search-input"]');
      await searchInput.fill(createdAnalyzerName);
      await page.waitForTimeout(1_500);

      const analyzerRow = page.locator("tbody tr", {
        hasText: new RegExp(
          createdAnalyzerName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"),
          "i",
        ),
      });
      await expect(analyzerRow.first()).toBeVisible({ timeout: 10_000 });
      await page.waitForTimeout(1_000);

      // ── Step 4: Configure file import ──────────────────────────────
      await showStepCard(page, 4, "Configure File Import");

      const overflowMenu = analyzerRow
        .first()
        .locator(".cds--overflow-menu")
        .first();
      await overflowMenu.click();
      await page.waitForTimeout(500);

      const fileImportAction = page
        .locator('[data-testid*="analyzer-action-file-import"]')
        .first();
      await expect(fileImportAction).toBeVisible({ timeout: 3_000 });
      await fileImportAction.click();

      const fileImportForm = page.locator(
        '[data-testid="file-import-configuration-form"]',
      );
      await expect(fileImportForm).toBeVisible({ timeout: 10_000 });
      await page.waitForTimeout(1_000);

      // Select file format — EXCEL
      const formatDropdown = page.locator(
        '[data-testid="file-import-configuration-file-format-dropdown"]',
      );
      await formatDropdown.click();
      await page.waitForTimeout(500);

      const excelOption = page
        .locator('[role="option"]')
        .filter({ hasText: /Excel/i });
      await expect(excelOption.first()).toBeVisible({ timeout: 3_000 });
      await excelOption.first().click();
      await page.waitForTimeout(500);

      // Set import directory
      const directoryInput = page.locator(
        '[data-testid="file-import-configuration-directory-input"]',
      );
      await directoryInput.fill(analyzer.importDir);
      await page.waitForTimeout(500);

      // Set file pattern
      const patternInput = page.locator(
        '[data-testid="file-import-configuration-pattern-input"]',
      );
      await patternInput.clear();
      await patternInput.fill(analyzer.filePattern);
      await page.waitForTimeout(500);

      // Set archive directory
      const archiveInput = page.locator(
        '[data-testid="file-import-configuration-archive-input"]',
      );
      await archiveInput.fill(analyzer.archiveDir);
      await page.waitForTimeout(500);

      // Set error directory
      const errorInput = page.locator(
        '[data-testid="file-import-configuration-error-input"]',
      );
      await errorInput.fill(analyzer.errorDir);
      await page.waitForTimeout(500);

      // Set column mappings
      const columnMappingsInput = page.locator(
        '[data-testid="file-import-configuration-column-mappings-input"]',
      );
      await columnMappingsInput.clear();
      await columnMappingsInput.fill(analyzer.columnMappings);
      await page.waitForTimeout(1_000);

      // Save file import configuration
      const fileImportSave = page.locator(
        '[data-testid="file-import-configuration-form-save-button"]',
      );
      await fileImportSave.click();
      await expect(fileImportForm).toBeHidden({ timeout: 15_000 });
      console.log("File import configuration saved");
      await page.waitForTimeout(1_500);

      // ── Step 5: Drop file into watched directory ───────────────────
      await showStepCard(
        page,
        5,
        `Drop ${fileExtension} file into watched directory`,
      );

      const timestamp = Date.now();
      const destFilename = `${analyzer.filePrefix}${timestamp}${fileExtension}`;
      const destPath = path.join(HOST_IMPORT_DIR, destFilename);

      fs.copyFileSync(FIXTURE_FILE, destPath);
      console.log(`Copied fixture to: ${destPath}`);
      expect(fs.existsSync(destPath)).toBeTruthy();
      await page.waitForTimeout(2_000);

      // ── Step 6: Wait for FileImportWatchService ────────────────────
      await showStepCard(
        page,
        6,
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

      // ── Step 7: View imported results ──────────────────────────────
      await showStepCard(page, 7, "View Imported Results");

      const apiResponsePromise = page
        .waitForResponse(
          (resp) => resp.url().includes("/rest/AnalyzerResults"),
          { timeout: 30_000 },
        )
        .catch(() => null);

      await page.goto(
        `AnalyzerResults?type=${encodeURIComponent(createdAnalyzerName)}`,
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
        } catch {
          // Non-JSON response
        }
      }
      await page.waitForTimeout(3_000);

      // ── Step 8: Verify results appear ──────────────────────────────
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

      // ── Navigate back to analyzer list ─────────────────────────────
      await page.goto("analyzers", { waitUntil: "domcontentloaded" });
      await expect(page.locator('[data-testid="analyzers-list"]')).toBeVisible({
        timeout: 15_000,
      });
      await page.waitForTimeout(2_000);
    });

    test.afterEach(async ({ page }) => {
      // Clean up dropped files
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

      // Clean up created analyzer (unless CLEANUP=false for video inspection)
      if (!CLEANUP || !createdAnalyzerName) return;

      try {
        await page.goto("analyzers", { waitUntil: "domcontentloaded" });
        const searchInput = page.locator(
          '[data-testid="analyzer-search-input"]',
        );
        await searchInput.fill(createdAnalyzerName);
        await page.waitForTimeout(1_000);

        const row = page.locator("tbody tr", {
          hasText: new RegExp(
            createdAnalyzerName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"),
            "i",
          ),
        });
        if (
          await row
            .first()
            .isVisible({ timeout: 3_000 })
            .catch(() => false)
        ) {
          const overflow = row.first().locator(".cds--overflow-menu").first();
          await overflow.click();
          await page.waitForTimeout(500);

          const deleteAction = page
            .locator('[data-testid*="analyzer-action-delete"]')
            .first();
          if (
            await deleteAction.isVisible({ timeout: 2_000 }).catch(() => false)
          ) {
            await deleteAction.click();
            const confirmButton = page
              .getByRole("button", { name: /delete|confirm/i })
              .last();
            if (
              await confirmButton
                .isVisible({ timeout: 3_000 })
                .catch(() => false)
            ) {
              await confirmButton.click();
              await page.waitForTimeout(1_000);
            }
          }
        }
      } catch {
        // Cleanup failure is not a test failure
      }
    });
  });
}
