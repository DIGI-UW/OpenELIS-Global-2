import { test, expect } from "@playwright/test";
import * as fs from "fs";
import * as path from "path";
import { showTitleCard, showStepCard } from "../helpers/title-card";
import { isVideoProject, videoPause } from "../helpers/video-pause";
import { acceptAndVerifyResults } from "../helpers/accept-results";

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
    importSubdir: "demo-qs5",
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
    expectedResults: [
      { sampleId: "E2E001", result: "1520.5" },
      { sampleId: "E2E002", result: "45200" },
      { sampleId: "E2E005", result: "3200.8" },
    ],
    headerMarker: "Sample Name", // should NOT appear in results (regression check)
  },
  {
    name: "QuantStudio 7",
    profileText: "QuantStudio",
    fixture: "quantstudio-e2e-results.xlsx",
    importSubdir: "demo-qs7",
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
    expectedResults: [
      { sampleId: "E2E001", result: "1520.5" },
      { sampleId: "E2E002", result: "45200" },
      { sampleId: "E2E005", result: "3200.8" },
    ],
    headerMarker: "Sample Name",
  },
  {
    name: "FluoroCycler XT",
    profileText: "FluoroCycler",
    fixture: "fluorocycler-e2e-results.xlsx",
    importSubdir: "demo-fluorocycler",
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
    expectedResults: [
      { sampleId: "E2E-FC001", result: "28.5" },
      { sampleId: "E2E-FC002", result: "31.2" },
      { sampleId: "E2E-FC003", result: "Negative" },
    ],
    headerMarker: "SampleID",
  },
];

for (const analyzer of ANALYZERS) {
  const HOST_IMPORTS_BASE = path.join(
    REPO_ROOT,
    "projects/analyzer-harness/volume/analyzer-imports",
  );
  const FIXTURE_FILE = path.join(FIXTURES_DIR, analyzer.fixture);
  const fileExtension = path.extname(analyzer.fixture);

  test.describe(`${analyzer.name} File Import → Results`, () => {
    test.setTimeout(300_000); // 5 min — create + config + 60s poll + results

    let createdAnalyzerName: string;
    // Unique per-run directory to avoid overlapping config conflicts with
    // stale E2E analyzers from previous runs (findOverlappingConfigs check).
    let HOST_IMPORT_DIR: string;

    test(`full flow: create → configure → import → results (${fileExtension})`, async ({
      page,
    }, testInfo) => {
      // Skip if analyzer harness bind-mount base doesn't exist (e.g. in CI)
      test.skip(
        !fs.existsSync(HOST_IMPORTS_BASE),
        "Requires analyzer harness bind-mount (analyzer-imports not found)",
      );

      // Use a unique name and directory per run to avoid collisions
      const runId = Date.now();
      createdAnalyzerName = `${analyzer.name} E2E ${runId}`;
      const dirSlug = analyzer.importSubdir;
      HOST_IMPORT_DIR = path.join(
        HOST_IMPORTS_BASE,
        `${dirSlug}-${runId}`,
        "incoming",
      );

      // Compute unique container-side directories matching the host path
      const containerDirBase = `/data/analyzer-imports/${dirSlug}-${runId}`;
      const containerImportDir = `${containerDirBase}/incoming`;
      const containerArchiveDir = `${containerDirBase}/processed`;
      const containerErrorDir = `${containerDirBase}/errors`;

      // Create host-side subdirectory
      if (!fs.existsSync(HOST_IMPORT_DIR)) {
        fs.mkdirSync(HOST_IMPORT_DIR, { recursive: true });
      }

      // Verify fixture exists
      expect(fs.existsSync(FIXTURE_FILE)).toBeTruthy();

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
        3000,
        testInfo,
      );

      // ── Step 1: Navigate to analyzer dashboard ─────────────────────
      await showStepCard(
        page,
        1,
        "Navigate to Analyzer Dashboard",
        2000,
        testInfo,
      );

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
      await videoPause(page, 1_500, testInfo);

      // ── Step 2: Create analyzer ────────────────────────────────────
      await showStepCard(
        page,
        2,
        `Create ${analyzer.name} Analyzer`,
        2000,
        testInfo,
      );

      const addButton = page.locator('[data-testid="add-analyzer-button"]');
      await expect(addButton).toBeVisible({ timeout: 5_000 });
      await addButton.click();

      const analyzerForm = page.locator('[data-testid="analyzer-form"]');
      await expect(analyzerForm).toBeVisible({ timeout: 10_000 });
      await videoPause(page, 1_000, testInfo);

      // Fill name
      const nameInput = page.locator(
        '[data-testid="analyzer-form-name-input"]',
      );
      await nameInput.fill(createdAnalyzerName);
      await videoPause(page, 500, testInfo);

      // Select plugin type — Generic File (FILE)
      // Carbon places data-testid on the wrapper div; click the inner trigger button.
      const pluginTypeDropdown = page.locator(
        '[data-testid="analyzer-form-plugin-type-dropdown"]',
      );
      const pluginTypeTrigger = pluginTypeDropdown.locator(
        'button[role="combobox"], .cds--list-box__field',
      );
      await expect(pluginTypeTrigger).toBeEnabled({ timeout: 10_000 });
      await pluginTypeTrigger.click();
      await videoPause(page, 500, testInfo);

      const filePluginOption = page
        .locator('[role="option"]')
        .filter({ hasText: /Generic File.*FILE|FILE.*Generic File/i });
      await expect(filePluginOption.first()).toBeVisible({ timeout: 3_000 });
      await filePluginOption.first().click();
      await videoPause(page, 1_000, testInfo);

      // Select default config profile (QuantStudio or FluoroCycler)
      const defaultConfigDropdown = page.locator(
        '[data-testid="analyzer-form-default-config-dropdown"]',
      );
      const defaultConfigTrigger = defaultConfigDropdown.locator(
        'button[role="combobox"], .cds--list-box__field',
      );
      await expect(defaultConfigTrigger).toBeEnabled({ timeout: 10_000 });
      await defaultConfigTrigger.click();
      await videoPause(page, 500, testInfo);

      const profileOption = page
        .locator('[role="option"]')
        .filter({ hasText: new RegExp(analyzer.profileText, "i") });
      await expect(profileOption.first()).toBeVisible({ timeout: 3_000 });
      await profileOption.first().click();
      await videoPause(page, 1_000, testInfo);

      // Select analyzer type — Molecular
      const typeDropdown = page.locator(
        '[data-testid="analyzer-form-type-dropdown"]',
      );
      const typeTrigger = typeDropdown.locator(
        'button[role="combobox"], .cds--list-box__field',
      );
      await expect(typeTrigger).toBeEnabled({ timeout: 10_000 });
      await typeTrigger.click();
      await videoPause(page, 500, testInfo);

      const molecularOption = page
        .locator('[role="option"]')
        .filter({ hasText: /Molecular/i });
      await expect(molecularOption.first()).toBeVisible({ timeout: 3_000 });
      await molecularOption.first().click();
      await videoPause(page, 500, testInfo);

      // Save the analyzer
      const saveButton = page.locator(
        '[data-testid="analyzer-form-save-button"]',
      );
      await saveButton.click();
      await expect(analyzerForm).toBeHidden({ timeout: 15_000 });
      console.log(`Created analyzer: ${createdAnalyzerName}`);
      await videoPause(page, 1_500, testInfo);

      // ── Step 3: Find analyzer in the list ──────────────────────────
      await showStepCard(page, 3, "Verify Analyzer Created", 2000, testInfo);

      const searchInput = page.locator('[data-testid="analyzer-search-input"]');
      await searchInput.fill(createdAnalyzerName);
      await videoPause(page, 1_500, testInfo);

      const analyzerRow = page.locator("tbody tr", {
        hasText: new RegExp(
          createdAnalyzerName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"),
          "i",
        ),
      });
      await expect(analyzerRow.first()).toBeVisible({ timeout: 10_000 });
      await videoPause(page, 1_000, testInfo);

      // ── Step 4: Configure file import ──────────────────────────────
      await showStepCard(page, 4, "Configure File Import", 2000, testInfo);

      const overflowMenu = analyzerRow
        .first()
        .locator(".cds--overflow-menu")
        .first();
      await overflowMenu.click();
      await videoPause(page, 500, testInfo);

      const fileImportAction = page
        .locator('[data-testid*="analyzer-action-file-import"]')
        .first();
      await expect(fileImportAction).toBeVisible({ timeout: 3_000 });
      await fileImportAction.click();

      const fileImportForm = page.locator(
        '[data-testid="file-import-configuration-form"]',
      );
      await expect(fileImportForm).toBeVisible({ timeout: 10_000 });
      await videoPause(page, 1_000, testInfo);

      // Wait for the auto-created config to load into the form.
      // autoCreateFromProfile pre-populates the directory field; we must wait
      // for it before filling, or a subsequent re-render overwrites our values.
      const directoryInput = page.locator(
        '[data-testid="file-import-configuration-directory-input"]',
      );
      await expect(directoryInput).not.toHaveValue("", { timeout: 10_000 });

      // Select file format — EXCEL
      const formatDropdown = page.locator(
        '[data-testid="file-import-configuration-file-format-dropdown"]',
      );
      const formatTrigger = formatDropdown.locator(
        'button[role="combobox"], .cds--list-box__field',
      );
      await expect(formatTrigger).toBeEnabled({ timeout: 10_000 });
      await formatTrigger.click();
      await videoPause(page, 500, testInfo);

      const excelOption = page
        .locator('[role="option"]')
        .filter({ hasText: /Excel/i });
      await expect(excelOption.first()).toBeVisible({ timeout: 3_000 });
      await excelOption.first().click();
      await videoPause(page, 500, testInfo);

      // Set import directory (clear auto-created value first)
      await directoryInput.clear();
      await directoryInput.fill(containerImportDir);
      await videoPause(page, 500, testInfo);

      // Set file pattern
      const patternInput = page.locator(
        '[data-testid="file-import-configuration-pattern-input"]',
      );
      await patternInput.clear();
      await patternInput.fill(analyzer.filePattern);
      await videoPause(page, 500, testInfo);

      // Set archive directory
      const archiveInput = page.locator(
        '[data-testid="file-import-configuration-archive-input"]',
      );
      await archiveInput.clear();
      await archiveInput.fill(containerArchiveDir);
      await videoPause(page, 500, testInfo);

      // Set error directory
      const errorInput = page.locator(
        '[data-testid="file-import-configuration-error-input"]',
      );
      await errorInput.clear();
      await errorInput.fill(containerErrorDir);
      await videoPause(page, 500, testInfo);

      // Set column mappings
      const columnMappingsInput = page.locator(
        '[data-testid="file-import-configuration-column-mappings-input"]',
      );
      await columnMappingsInput.clear();
      await columnMappingsInput.fill(analyzer.columnMappings);
      await videoPause(page, 1_000, testInfo);

      // Save file import configuration
      const fileImportSave = page.locator(
        '[data-testid="file-import-configuration-form-save-button"]',
      );
      await fileImportSave.click();
      await expect(fileImportForm).toBeHidden({ timeout: 15_000 });
      console.log("File import configuration saved");
      await videoPause(page, 1_500, testInfo);

      // ── Step 5: Drop file into watched directory ───────────────────
      await showStepCard(
        page,
        5,
        `Drop ${fileExtension} file into watched directory`,
        2000,
        testInfo,
      );

      const timestamp = Date.now();
      const destFilename = `${analyzer.filePrefix}${timestamp}${fileExtension}`;
      const destPath = path.join(HOST_IMPORT_DIR, destFilename);

      fs.copyFileSync(FIXTURE_FILE, destPath);
      console.log(`Copied fixture to: ${destPath}`);
      expect(fs.existsSync(destPath)).toBeTruthy();
      await videoPause(page, 2_000, testInfo);

      // ── Step 6: Wait for FileImportWatchService ────────────────────
      await showStepCard(
        page,
        6,
        "Waiting for FileImportWatchService (polls every 60s)...",
        2000,
        testInfo,
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
        await page.waitForTimeout(pollIntervalMs); // Legitimate poll wait
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

      await videoPause(page, 2_000, testInfo);

      // ── Step 7: View imported results ──────────────────────────────
      await showStepCard(page, 7, "View Imported Results", 2000, testInfo);

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

      // Wait for results page to fully render
      await videoPause(page, 3_000, testInfo);

      // ── Step 8: Verify actual result values ────────────────────────
      const resultsTable = page.locator("table, .orderLegendBody");
      await expect(resultsTable.first()).toBeVisible({ timeout: 15_000 });

      // Regression check: header row should NOT be imported as data
      const headerMarkerLocator = page
        .locator("td")
        .filter({ hasText: new RegExp(`^${analyzer.headerMarker}$`) });
      await expect(headerMarkerLocator).toHaveCount(0, { timeout: 5_000 });
      console.log(
        `Verified: no header row in results (no '${analyzer.headerMarker}' in table)`,
      );

      // Hard assertion: verify EACH expected result value
      for (const expected of analyzer.expectedResults) {
        // Find the sample ID on the page (getByText works with the
        // AnalyzerResults component; tr hasText does not due to nested elements)
        const sampleText = page.getByText(expected.sampleId);
        await expect(sampleText.first()).toBeVisible({ timeout: 15_000 });

        // Verify the result value also appears on the page
        const resultText = page.getByText(expected.result, { exact: false });
        await expect(resultText.first()).toBeVisible({ timeout: 5_000 });

        console.log(`  ✓ ${expected.sampleId} → ${expected.result}`);
      }

      console.log(
        `All ${analyzer.expectedResults.length} result values verified for ${analyzer.name}!`,
      );
      if (isVideoProject(testInfo)) {
        await page.screenshot({
          path: `test-results/${analyzer.name.replace(/\s+/g, "-")}-staging-results.png`,
          fullPage: true,
        });
      }

      // ── Linger on staging results for the video ──────────────────
      await videoPause(page, 2_000, testInfo);

      // ── Steps 9-11: Accept results and verify in AccessionResults
      await acceptAndVerifyResults(page, testInfo, 8, analyzer.sampleIds[0]);

      // ── Completion Card (video ends here) ─────────────────────────
      await showTitleCard(
        page,
        "Import Complete",
        `${analyzer.name}: ${analyzer.expectedResults.length} results accepted & validated`,
        3000,
        testInfo,
      );
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
        await page.waitForTimeout(1_000); // Cleanup stability

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
          await page.waitForTimeout(500); // Cleanup stability

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
              await page.waitForTimeout(1_000); // Cleanup stability
            }
          }
        }
      } catch {
        // Cleanup failure is not a test failure
      }
    });
  });
}
