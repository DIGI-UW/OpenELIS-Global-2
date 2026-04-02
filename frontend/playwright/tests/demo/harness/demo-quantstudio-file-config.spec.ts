import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerFormPage } from "../../../fixtures/analyzer-form";
import { cleanupAnalyzerByName } from "../../../helpers/cleanup-analyzer";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import {
  SHORT_TIMEOUT,
  UI_TIMEOUT,
  LONG_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * Demo: QuantStudio 7 — Generic File profile walkthrough.
 *
 * This is intentionally UI-only story proof:
 *   1. Open the analyzer form
 *   2. Select the Generic File plugin
 *   3. Apply the QuantStudio profile
 *   4. Save the analyzer
 *   5. Re-open the visible file-import form and review the defaults
 *
 * Run with:
 *   Normal:  npx playwright test demo-quantstudio --project=harness-demo
 *   Video:   npx playwright test demo-quantstudio --project=harness-demo-video
 */

test.describe("Demo: QuantStudio 7 Generic File Config", () => {
  let createdAnalyzerName = "";

  test.afterEach(async ({ page }) => {
    if (!createdAnalyzerName) {
      return;
    }

    try {
      await cleanupAnalyzerByName(page, createdAnalyzerName);
    } catch {
      // Best-effort cleanup for demo-created analyzers.
    }

    createdAnalyzerName = "";
  });

  test("create QuantStudio 7 analyzer via Generic File plugin + profile", async ({
    page,
  }, testInfo) => {
    createdAnalyzerName = `QuantStudio-7-Demo-${Date.now()}`;

    const presentation = createDemoPresentation(page, testInfo);
    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    await presentation.title(
      "QuantStudio Generic File Profile",
      "Create the analyzer from the UI and review the visible file-import defaults.",
    );

    // Step 1: Navigate to Analyzers list
    await list.goto();
    await list.expectLoaded();
    await presentation.pause(1_000);

    // Step 2: Open Add Analyzer form
    await presentation.step(1, "Open the analyzer form");
    await list.clickAdd();
    await form.expectOpen();
    await presentation.pause(500);

    // Step 3: Select "Generic File" plugin type
    await presentation.step(2, "Select the Generic File plugin");
    await form.selectPluginType("Generic File");
    await presentation.pause(500);

    // Step 4: Verify FILE-specific form behavior
    // Protocol version dropdown should NOT be visible
    await expect(form.protocolVersionDropdown).not.toBeVisible();
    await expect(form.connectionFields).not.toBeVisible();
    await expect(form.fileProtocolInfo).toBeVisible();
    await presentation.pause(500);

    // Step 5: Select "QuantStudio QS5/QS7" default profile
    await presentation.step(3, "Apply the QuantStudio profile");
    await form.selectDefaultConfig("QuantStudio");
    await presentation.pause(1_000);

    // Step 6: Verify prefilled fields from QuantStudio profile
    await expect(form.typeDropdown).toContainText(/Molecular/i);

    // Step 7: Fill name
    await form.fillName(createdAnalyzerName);
    await presentation.pause(500);

    // Step 8: Save and return to the list
    await presentation.step(4, "Save the analyzer");
    await form.save();
    await form.expectSuccessNotification();
    await expect(form.modal).not.toBeVisible({ timeout: LONG_TIMEOUT });
    await presentation.pause(1_000);

    // Step 9: Verify analyzer appears in list
    await presentation.step(5, "Find the new analyzer in the list");
    await list.expectLoaded();
    await list.search(createdAnalyzerName);
    await presentation.pause(500);

    const row = page
      .locator("tbody tr")
      .filter({ hasText: createdAnalyzerName })
      .first();
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(row).toContainText(createdAnalyzerName);
    await expect(row).toContainText("MOLECULAR");
    await presentation.pause(1_000);

    // Step 10: Re-open the visible file-import form and review defaults
    await presentation.step(6, "Review the visible file-import defaults");
    await row
      .locator('[data-testid^="analyzer-row-overflow-"]')
      .first()
      .click();

    const fileImportAction = page
      .locator('[data-testid*="analyzer-action-file-import"]')
      .first();
    await expect(fileImportAction).toBeVisible({ timeout: SHORT_TIMEOUT });
    await fileImportAction.click();

    const fileImportForm = page.locator(
      '[data-testid="file-import-configuration-form"]',
    );
    await expect(fileImportForm).toBeVisible({ timeout: UI_TIMEOUT });

    const fileFormatDropdown = page.locator(
      '[data-testid="file-import-configuration-file-format-dropdown"]',
    );
    const directoryInput = page.locator(
      '[data-testid="file-import-configuration-directory-input"]',
    );
    const patternInput = page.locator(
      '[data-testid="file-import-configuration-pattern-input"]',
    );
    const archiveInput = page.locator(
      '[data-testid="file-import-configuration-archive-input"]',
    );
    const errorInput = page.locator(
      '[data-testid="file-import-configuration-error-input"]',
    );

    await expect(fileFormatDropdown).toContainText(/Excel/i);
    await expect(directoryInput).not.toHaveValue("", { timeout: UI_TIMEOUT });
    await expect(patternInput).not.toHaveValue("", { timeout: UI_TIMEOUT });
    await expect(archiveInput).not.toHaveValue("", { timeout: UI_TIMEOUT });
    await expect(errorInput).not.toHaveValue("", { timeout: UI_TIMEOUT });

    await presentation.title(
      "Story Complete",
      "The QuantStudio profile is proven by visible UI state only.",
    );
  });
});
