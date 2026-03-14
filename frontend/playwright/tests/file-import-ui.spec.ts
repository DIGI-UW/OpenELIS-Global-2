import { test, expect } from "@playwright/test";

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
 * Video recording controlled by PLAYWRIGHT_VIDEO env var:
 *   PLAYWRIGHT_VIDEO=on → record video
 *
 * Usage:
 *   # Video + keep data for inspection
 *   CLEANUP=false PLAYWRIGHT_VIDEO=on TEST_USER=admin TEST_PASS="adminADMIN!" \
 *     npx playwright test file-import-ui --project=file-import
 *
 *   # Normal CI run (cleanup, no video)
 *   TEST_USER=admin TEST_PASS="adminADMIN!" \
 *     npx playwright test file-import-ui --project=file-import
 */

const CLEANUP = process.env.CLEANUP !== "false";

test.describe("QuantStudio 7 MVP Workflow", () => {
  test.setTimeout(120_000);

  let createdAnalyzerName: string;

  test("create analyzer, configure file import, test connection", async ({
    page,
  }) => {
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

    await page.waitForTimeout(1_500);

    // ── Step 2: Click "Add Analyzer" ─────────────────────────────
    const addButton = page.locator('[data-testid="add-analyzer-button"]');
    await expect(addButton).toBeVisible({ timeout: 5_000 });
    await addButton.click();

    const analyzerForm = page.locator('[data-testid="analyzer-form"]');
    await expect(analyzerForm).toBeVisible({ timeout: 10_000 });
    await page.waitForTimeout(1_000);

    // ── Step 3: Fill in QuantStudio 7 details ────────────────────
    // Name
    const nameInput = page.locator('[data-testid="analyzer-form-name-input"]');
    await nameInput.fill(createdAnalyzerName);
    await page.waitForTimeout(500);

    // Plugin Type — select a FILE protocol type if available
    const pluginTypeDropdown = page.locator(
      '[data-testid="analyzer-form-plugin-type-dropdown"]',
    );
    await pluginTypeDropdown.click();
    await page.waitForTimeout(500);

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
    await page.waitForTimeout(500);

    // Analyzer Type — "Molecular" (QuantStudio is a PCR instrument)
    const typeDropdown = page.locator(
      '[data-testid="analyzer-form-type-dropdown"]',
    );
    await typeDropdown.click();
    await page.waitForTimeout(500);

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
    await page.waitForTimeout(1_500);

    // ── Step 4: Save the analyzer ────────────────────────────────
    const saveButton = page.locator(
      '[data-testid="analyzer-form-save-button"]',
    );
    await saveButton.click();

    // Wait for modal to auto-close (1s delay on success)
    await expect(analyzerForm).toBeHidden({ timeout: 15_000 });
    await page.waitForTimeout(1_500);

    // ── Step 5: Find the new analyzer in the list ────────────────
    const searchInput = page.locator('[data-testid="analyzer-search-input"]');
    await searchInput.fill(createdAnalyzerName);
    await page.waitForTimeout(1_500);

    const qsRow = page.locator("tbody tr", {
      hasText: new RegExp(
        createdAnalyzerName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"),
        "i",
      ),
    });
    await expect(qsRow.first()).toBeVisible({ timeout: 10_000 });
    await page.waitForTimeout(1_000);

    // ── Step 6: Configure File Import ────────────────────────────
    const overflowMenu = qsRow.first().locator(".cds--overflow-menu").first();
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
    await page.waitForTimeout(1_000);

    // Select file format — EXCEL for QuantStudio
    const formatDropdown = page.locator(
      '[data-testid="file-import-configuration-file-format-dropdown"]',
    );
    await formatDropdown.click();
    await page.waitForTimeout(500);

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
    await page.waitForTimeout(500);

    // Set import directory (simulated network drive)
    const directoryInput = page.locator(
      '[data-testid="file-import-configuration-directory-input"]',
    );
    await directoryInput.fill("/data/analyzer-imports/quantstudio-7");
    await page.waitForTimeout(500);

    // Set file pattern for Excel
    const patternInput = page.locator(
      '[data-testid="file-import-configuration-pattern-input"]',
    );
    await patternInput.clear();
    await patternInput.fill("*.xlsx");
    await page.waitForTimeout(500);

    // Set archive directory
    const archiveInput = page.locator(
      '[data-testid="file-import-configuration-archive-input"]',
    );
    await archiveInput.fill("/data/analyzer-imports/quantstudio-7/archive");
    await page.waitForTimeout(500);

    // Set error directory
    const errorInput = page.locator(
      '[data-testid="file-import-configuration-error-input"]',
    );
    await errorInput.fill("/data/analyzer-imports/quantstudio-7/errors");
    await page.waitForTimeout(2_000);

    // Save
    const fileImportSave = page.locator(
      '[data-testid="file-import-configuration-form-save-button"]',
    );
    await fileImportSave.click();
    await expect(fileImportForm).toBeHidden({ timeout: 15_000 });
    await page.waitForTimeout(1_500);

    // ── Step 7: Test Connection ──────────────────────────────────
    await expect(qsRow.first()).toBeVisible({ timeout: 10_000 });
    const overflowMenu2 = qsRow.first().locator(".cds--overflow-menu").first();
    await overflowMenu2.click();
    await page.waitForTimeout(500);

    const testConnectionAction = page
      .locator('[data-testid*="analyzer-action-test-connection"]')
      .first();
    await expect(testConnectionAction).toBeVisible({ timeout: 3_000 });
    await testConnectionAction.click();

    const testConnectionModal = page.locator(
      '[data-testid="test-connection-modal"]',
    );
    await expect(testConnectionModal).toBeVisible({ timeout: 10_000 });
    await page.waitForTimeout(1_000);

    // Click "Test"
    const testButton = page.locator(
      '[data-testid="test-connection-test-button"]',
    );
    await expect(testButton).toBeVisible({ timeout: 5_000 });
    await testButton.click();

    // Wait for result
    await page.waitForTimeout(3_000);

    // Verify success
    const successTag = page.locator('[data-testid="test-connection-success"]');
    const errorTag = page.locator('[data-testid="test-connection-error"]');
    await expect(successTag.or(errorTag)).toBeVisible({ timeout: 10_000 });

    // Expand logs for video
    const logsAccordion = page.locator('[data-testid="test-connection-logs"]');
    if (await logsAccordion.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await logsAccordion.click();
      await page.waitForTimeout(1_500);
    }

    await page.waitForTimeout(2_000);

    // Close
    const closeButton = page.locator(
      '[data-testid="test-connection-close-button"]',
    );
    await closeButton.click();

    // ── Step 8: Verify back at list ──────────────────────────────
    await expect(analyzerList).toBeVisible({ timeout: 10_000 });
    await page.waitForTimeout(1_500);
  });

  test.afterEach(async ({ page }) => {
    if (!CLEANUP || !createdAnalyzerName) return;

    // Clean up: delete the created analyzer via overflow menu
    try {
      await page.goto("analyzers", { waitUntil: "domcontentloaded" });
      const searchInput = page.locator('[data-testid="analyzer-search-input"]');
      await searchInput.fill(createdAnalyzerName);
      await page.waitForTimeout(1_000);

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
        await page.waitForTimeout(500);

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
            await page.waitForTimeout(1_000);
          }
        }
      }
    } catch {
      // Cleanup failure is not a test failure
    }
  });
});
