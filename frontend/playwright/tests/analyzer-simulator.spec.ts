import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";

/**
 * Analyzer Simulator E2E
 *
 * Validates the revised M2 simulator path for fixture-loaded GeneXpert analyzer:
 * open mappings -> open test mapping modal -> preview sample ASTM payload.
 */
test.describe("Analyzer Simulator", () => {
  test.skip(
    !!process.env.CI,
    "Requires analyzer harness with fixture data (not available in CI)",
  );

  test("GeneXpert preview-mapping shows v1.2 simulator payload", async ({
    page,
  }) => {
    const GENEXPERT_ID = "2013";
    const list = new AnalyzerListPage(page);

    await list.goto();
    await list.expectLoaded();
    await list.openOverflowMenu(GENEXPERT_ID);
    await list.clickAction(GENEXPERT_ID, "mappings");

    await expect(page).toHaveURL(new RegExp(`/analyzers/${GENEXPERT_ID}/mappings`));
    await expect(page.locator('[data-testid="field-mapping"]')).toBeVisible();

    await page
      .locator('[data-testid="field-mapping-test-button"]')
      .click({ timeout: 10_000 });
    await expect(page.locator('[data-testid="test-mapping-modal"]')).toBeVisible();

    await page
      .locator('[data-testid="test-mapping-message-input"]')
      .fill(
        "H|\\^&|||PSM^Micro^2.0|...\nP|1||...\nO|1||...\nR|1|^GLUCOSE^...|123|mg/dL|...",
      );
    await page.locator('[data-testid="test-mapping-preview-button"]').click();

    await expect(
      page.locator('[data-testid="test-mapping-plugin-config-snapshot"]'),
    ).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('[data-testid="test-mapping-results"]')).toBeVisible(
      { timeout: 15_000 },
    );
  });
});
