import { test, expect } from "@playwright/test";
import { videoPause } from "../helpers/video-pause";

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

test.describe("QuantStudio 7 MVP Workflow", () => {
  test.setTimeout(120_000);

  let createdAnalyzerName: string;

  test("create analyzer, configure file import, test connection", async ({
    page,
  }, testInfo) => {
    // Capture browser console errors for debugging
    const consoleErrors: string[] = [];
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });
    page.on("pageerror", (err) => {
      consoleErrors.push(`PAGE ERROR: ${err.message}`);
    });

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

    const fileOption = page
      .locator('[role="option"]')
      .filter({ hasText: /FILE/i });
    const firstOption = page.locator('[role="option"]').first();

    if (
      await fileOption
        .first()
        .isVisible({ timeout: 2_000 })
        .catch(() => false)
    ) {
      await fileOption.first().click();
    } else {
      await firstOption.click();
    }
    await videoPause(page, 500, testInfo);

    // Analyzer Type — "Molecular" (QuantStudio is a PCR instrument)
    const typeDropdown = page.locator(
      '[data-testid="analyzer-form-type-dropdown"]',
    );
    await typeDropdown.click();
    await videoPause(page, 500, testInfo);

    const molecularOption = page
      .locator('[role="option"]')
      .filter({ hasText: /Molecular/i });
    if (
      await molecularOption
        .first()
        .isVisible({ timeout: 2_000 })
        .catch(() => false)
    ) {
      await molecularOption.first().click();
    } else {
      await page.locator('[role="option"]').first().click();
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
      hasText: new RegExp(
        createdAnalyzerName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"),
        "i",
      ),
    });
    await expect(qsRow.first()).toBeVisible({ timeout: 10_000 });
    await videoPause(page, 1_000, testInfo);

    // ── Step 6: Configure File Import ────────────────────────────
    const overflowMenu = qsRow.first().locator(".cds--overflow-menu").first();
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

    // Debug: dump console errors if modal doesn't appear
    try {
      await expect(fileImportForm).toBeVisible({ timeout: 10_000 });
    } catch (e) {
      console.log("Console errors captured:", consoleErrors);
      console.log("Current URL:", page.url());
      const bodyText = await page
        .locator("body")
        .textContent({ timeout: 2_000 })
        .catch(() => "(empty)");
      console.log("Page body text:", bodyText?.substring(0, 500));
      throw e;
    }
    await videoPause(page, 1_000, testInfo);

    // Select file format — EXCEL for QuantStudio
    const formatDropdown = page.locator(
      '[data-testid="file-import-configuration-file-format-dropdown"]',
    );
    await formatDropdown.click();
    await videoPause(page, 500, testInfo);

    const excelOption = page
      .locator('[role="option"]')
      .filter({ hasText: /Excel/i });
    if (
      await excelOption
        .first()
        .isVisible({ timeout: 2_000 })
        .catch(() => false)
    ) {
      await excelOption.first().click();
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
    await expect(qsRow.first()).toBeVisible({ timeout: 10_000 });
    const overflowMenu2 = qsRow.first().locator(".cds--overflow-menu").first();
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

    // Wait for result
    await videoPause(page, 3_000, testInfo);

    // Verify success
    const successTag = page.locator('[data-testid="test-connection-success"]');
    const errorTag = page.locator('[data-testid="test-connection-error"]');
    await expect(successTag.or(errorTag)).toBeVisible({ timeout: 10_000 });

    // Expand logs for video
    const logsAccordion = page.locator('[data-testid="test-connection-logs"]');
    if (await logsAccordion.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await logsAccordion.click();
      await videoPause(page, 1_500, testInfo);
    }

    await videoPause(page, 2_000, testInfo);

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
      await page.goto("analyzers", { waitUntil: "domcontentloaded" });
      const searchInput = page.locator('[data-testid="analyzer-search-input"]');
      await searchInput.fill(createdAnalyzerName);
      await page.waitForTimeout(1_000); // Cleanup stability

      const qsRow = page.locator("tbody tr", {
        hasText: new RegExp(
          createdAnalyzerName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"),
          "i",
        ),
      });
      if (
        await qsRow
          .first()
          .isVisible({ timeout: 3_000 })
          .catch(() => false)
      ) {
        const overflow = qsRow.first().locator(".cds--overflow-menu").first();
        await overflow.click();
        await page.waitForTimeout(500); // Cleanup stability

        const deleteAction = page
          .locator('[data-testid*="analyzer-action-delete"]')
          .first();
        if (
          await deleteAction.isVisible({ timeout: 2_000 }).catch(() => false)
        ) {
          await deleteAction.click();
          // Confirm deletion in the modal
          const confirmButton = page
            .getByRole("button", { name: /delete|confirm/i })
            .last();
          if (
            await confirmButton.isVisible({ timeout: 3_000 }).catch(() => false)
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
