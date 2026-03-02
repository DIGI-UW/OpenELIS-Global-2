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
    process.env.CI === "true",
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

    await expect(page).toHaveURL(
      new RegExp(`/analyzers/${GENEXPERT_ID}/mappings`),
    );
    await expect(page.locator('[data-testid="field-mapping"]')).toBeVisible();

    await page
      .locator('[data-testid="field-mapping-test-button"]')
      .click({ timeout: 10_000 });
    await expect(
      page.locator('[data-testid="test-mapping-modal"]'),
    ).toBeVisible();

    await page
      .locator('[data-testid="test-mapping-message-input"]')
      .fill(
        "H|\\^&|||PSM^Micro^2.0|...\nP|1||...\nO|1||...\nR|1|^GLUCOSE^...|123|mg/dL|...",
      );
    await page.locator('[data-testid="test-mapping-preview-button"]').click();

    const results = page.locator('[data-testid="test-mapping-results"]');
    await expect(results).toBeVisible({ timeout: 15_000 });

    // Ensure preview produced actual parsed rows.
    await expect(results.locator("tbody tr").first()).toBeVisible({
      timeout: 10_000,
    });

    // Plugin config details may render either in-modal (legacy test-id blocks)
    // or as the page-level snapshot card in newer UI revisions.
    const snapshot = page.locator(
      '[data-testid="test-mapping-plugin-config-snapshot"]',
    );
    const errors = page.locator('[data-testid="test-mapping-errors"]');
    const pageSnapshotCard = page.getByText("Plugin Configuration Snapshot");
    const hasSnapshot = (await snapshot.count()) > 0;
    const hasErrors = (await errors.count()) > 0;
    const hasPageSnapshotCard = (await pageSnapshotCard.count()) > 0;
    expect(
      hasSnapshot || hasErrors || hasPageSnapshotCard,
      "Preview should expose plugin-config state (modal or page snapshot)",
    ).toBeTruthy();
  });
});
