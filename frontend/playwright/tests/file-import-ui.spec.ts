import { test, expect } from "@playwright/test";
import { videoPause } from "../helpers/video-pause";
import { cleanupAnalyzerByName } from "../helpers/cleanup-analyzer";

/**
 * QuantStudio 7 MVP Workflow E2E
 *
 * Demonstrates the first stage of the MVP workflow:
 *   1. Create a new QuantStudio 7 analyzer from scratch
 *   2. Configure file import pointing to a network drive location
 *   3. Test the directory connection → verify success
 *
 * Cleanup behavior controlled by CLEANUP env var:
 *   CLEANUP=false  → leave test data (for video/debug)
 *   CLEANUP unset  → delete created analyzer after test (default)
 *
 * Usage:
 *   # Video recording:
 *   CLEANUP=false TEST_USER=admin TEST_PASS="adminADMIN!" \
 *     npx playwright test file-import-ui --project=demo-video
 *
 *   # Normal run (cleanup, no video):
 *   TEST_USER=admin TEST_PASS="adminADMIN!" \
 *     npx playwright test file-import-ui --project=demo
 */

const CLEANUP = process.env.CLEANUP !== "false";

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

test.describe("QuantStudio 7 MVP Workflow", () => {
  test.setTimeout(120_000);

  let createdAnalyzerName: string;

  test("create analyzer, configure file import, test connection", async ({
    page,
  }, testInfo) => {
    // Use a unique name to avoid collisions with existing data
    createdAnalyzerName = `QuantStudio 7 Pro E2E ${Date.now()}`;

    // ── Step 1: Navigate to analyzer management ──────────────────
    await page.goto("analyzers", { waitUntil: "domcontentloaded" });

    const analyzerList = page.locator('[data-testid="analyzers-list"]');
    await expect(analyzerList).toBeVisible({ timeout: 30_000 });
    await expect(
      page.locator('[data-testid="analyzers-list-stats"]'),
    ).toBeVisible({ timeout: 15_000 });

    await videoPause(page, 1_500, testInfo);

    // ── Step 2: Click "Add Analyzer" ─────────────────────────────
    const addButton = page.locator('[data-testid="add-analyzer-button"]');
    await expect(addButton).toBeVisible({ timeout: 5_000 });
    await addButton.click();

    const analyzerForm = page.locator('[data-testid="analyzer-form"]');
    await expect(analyzerForm).toBeVisible({ timeout: 10_000 });
    await videoPause(page, 1_000, testInfo);

    // ── Step 3: Fill in QuantStudio 7 details ────────────────────
    // Name
    const nameInput = page.locator('[data-testid="analyzer-form-name-input"]');
    await nameInput.fill(createdAnalyzerName);
    await videoPause(page, 500, testInfo);

    // Plugin Type — select a FILE protocol type if available
    const pluginTypeDropdown = page.locator(
      '[data-testid="analyzer-form-plugin-type-dropdown"]',
    );
    await pluginTypeDropdown.click();
    await videoPause(page, 500, testInfo);

    const dropdownOptions = page.locator(
      '[role="listbox"]:visible [role="option"]',
    );
    const fileOption = dropdownOptions.filter({ hasText: /FILE/i }).first();
    if (await fileOption.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await fileOption.click();
    } else {
      await expect(dropdownOptions.first()).toBeVisible({ timeout: 2_000 });
      await dropdownOptions.first().click();
    }
    await videoPause(page, 500, testInfo);

    // Analyzer Type — "Molecular" (QuantStudio is a PCR instrument)
    const typeDropdown = page.locator(
      '[data-testid="analyzer-form-type-dropdown"]',
    );
    await typeDropdown.click();
    await videoPause(page, 500, testInfo);

    const molecularOption = dropdownOptions
      .filter({ hasText: /Molecular/i })
      .first();
    if (
      await molecularOption.isVisible({ timeout: 2_000 }).catch(() => false)
    ) {
      await molecularOption.click();
    } else {
      await expect(dropdownOptions.first()).toBeVisible({ timeout: 2_000 });
      await dropdownOptions.first().click();
    }
    await videoPause(page, 1_500, testInfo);

    // ── Step 4: Save the analyzer ────────────────────────────────
    const saveButton = page.locator(
      '[data-testid="analyzer-form-save-button"]',
    );
    await saveButton.click();

    // Wait for modal to auto-close (1s delay on success)
    await expect(analyzerForm).toBeHidden({ timeout: 15_000 });
    await videoPause(page, 1_500, testInfo);

    // ── Step 5: Find the new analyzer in the list ────────────────
    const searchInput = page.locator('[data-testid="analyzer-search-input"]');
    await searchInput.fill(createdAnalyzerName);
    await videoPause(page, 1_500, testInfo);

    const qsRow = page.locator("tbody tr", {
      hasText: new RegExp(escapeRegExp(createdAnalyzerName), "i"),
    });
    const analyzerRow = qsRow.first();
    await expect(analyzerRow).toBeVisible({ timeout: 10_000 });
    await videoPause(page, 1_000, testInfo);

    // ── Step 6: Configure File Import ────────────────────────────
    const overflowMenu = analyzerRow
      .locator('[data-testid^="analyzer-row-overflow-"]')
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

    // Select file format — EXCEL for QuantStudio
    const formatDropdown = page.locator(
      '[data-testid="file-import-configuration-file-format-dropdown"]',
    );
    await formatDropdown.click();
    await videoPause(page, 500, testInfo);

    const excelOption = dropdownOptions.filter({ hasText: /Excel/i }).first();
    if (await excelOption.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await excelOption.click();
    } else {
      await expect(dropdownOptions.first()).toBeVisible({ timeout: 2_000 });
      await dropdownOptions.first().click();
    }
    await videoPause(page, 500, testInfo);

    // Set import directory (simulated network drive)
    const directoryInput = page.locator(
      '[data-testid="file-import-configuration-directory-input"]',
    );
    await directoryInput.fill("/data/analyzer-imports/quantstudio-7");
    await videoPause(page, 500, testInfo);

    // Set file pattern for Excel
    const patternInput = page.locator(
      '[data-testid="file-import-configuration-pattern-input"]',
    );
    await patternInput.clear();
    await patternInput.fill("*.xlsx");
    await videoPause(page, 500, testInfo);

    // Set archive directory
    const archiveInput = page.locator(
      '[data-testid="file-import-configuration-archive-input"]',
    );
    await archiveInput.fill("/data/analyzer-imports/quantstudio-7/archive");
    await videoPause(page, 500, testInfo);

    // Set error directory
    const errorInput = page.locator(
      '[data-testid="file-import-configuration-error-input"]',
    );
    await errorInput.fill("/data/analyzer-imports/quantstudio-7/errors");
    await videoPause(page, 2_000, testInfo);

    // Save
    const fileImportSave = page.locator(
      '[data-testid="file-import-configuration-form-save-button"]',
    );
    await fileImportSave.click();
    await expect(fileImportForm).toBeHidden({ timeout: 15_000 });
    await videoPause(page, 1_500, testInfo);

    // ── Step 7: Test Connection ──────────────────────────────────
    await expect(analyzerRow).toBeVisible({ timeout: 10_000 });
    const overflowMenu2 = analyzerRow
      .locator('[data-testid^="analyzer-row-overflow-"]')
      .first();
    await overflowMenu2.click();
    await videoPause(page, 500, testInfo);

    const testConnectionAction = page
      .locator('[data-testid*="analyzer-action-test-connection"]')
      .first();
    await expect(testConnectionAction).toBeVisible({ timeout: 3_000 });
    await testConnectionAction.click();

    const testConnectionModal = page.locator(
      '[data-testid="test-connection-modal"]',
    );
    await expect(testConnectionModal).toBeVisible({ timeout: 10_000 });
    await videoPause(page, 1_000, testInfo);

    // Click "Test"
    const testButton = page.locator(
      '[data-testid="test-connection-test-button"]',
    );
    await expect(testButton).toBeVisible({ timeout: 5_000 });
    await testButton.click();

    // Verify success
    const successTag = page.locator('[data-testid="test-connection-success"]');
    await expect(successTag).toBeVisible({ timeout: 10_000 });
    await videoPause(page, 1_500, testInfo);

    // Close
    const closeButton = page.locator(
      '[data-testid="test-connection-close-button"]',
    );
    await closeButton.click();

    // ── Step 8: Verify back at list ──────────────────────────────
    await expect(analyzerList).toBeVisible({ timeout: 10_000 });
    await videoPause(page, 1_500, testInfo);
  });

  test.afterEach(async ({ page }) => {
    if (!CLEANUP || !createdAnalyzerName) return;

    // Clean up: delete the created analyzer via overflow menu
    try {
      await cleanupAnalyzerByName(page, createdAnalyzerName);
    } catch (error) {
      console.warn("Cleanup failure for created analyzer:", error);
    }
  });
});
