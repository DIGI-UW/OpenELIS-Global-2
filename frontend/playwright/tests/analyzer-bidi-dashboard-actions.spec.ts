import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { GENEXPERT_FIXTURE_ID } from "../fixtures/analyzer-constants";

/**
 * Dashboard presence of bidirectional actions (Send Order, Query Results).
 * Does not require the analyzer harness; only that the analyzer list loads
 * with at least the minimal GeneXpert fixture (2013).
 */
test.describe("Analyzer bidirectional dashboard actions", () => {
  test.skip(
    process.env.CI === "true",
    "Requires analyzer list with fixture data (not available in default CI)",
  );

  test("overflow menu shows Send Order and Query Results for fixture analyzer", async ({
    page,
  }) => {
    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();

    const row = list.getRow(GENEXPERT_FIXTURE_ID);
    await expect(row).toBeVisible({ timeout: 10_000 });

    await list.openOverflowMenu(GENEXPERT_FIXTURE_ID);

    const sendOrderAction = page.locator(
      `[data-testid="analyzer-action-send-order-${GENEXPERT_FIXTURE_ID}"]`,
    );
    const queryResultsAction = page.locator(
      `[data-testid="analyzer-action-query-results-${GENEXPERT_FIXTURE_ID}"]`,
    );

    await expect(sendOrderAction).toBeVisible();
    await expect(queryResultsAction).toBeVisible();
  });

  test("Send Order action opens Send Order modal", async ({ page }) => {
    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();

    await list.openOverflowMenu(GENEXPERT_FIXTURE_ID);
    await list.clickAction(GENEXPERT_FIXTURE_ID, "send-order");

    const modal = page.locator('[data-testid="send-order-modal"]');
    await expect(modal).toBeVisible({ timeout: 5_000 });
    await expect(
      page.locator('[data-testid="send-order-accession-input"]'),
    ).toBeVisible();

    await page.locator('[data-testid="send-order-close-button"]').click();
    await expect(modal).not.toBeVisible();
  });

  test("Query Results action opens Query Results modal", async ({ page }) => {
    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();

    await list.openOverflowMenu(GENEXPERT_FIXTURE_ID);
    await list.clickAction(GENEXPERT_FIXTURE_ID, "query-results");

    const modal = page.locator('[data-testid="query-results-modal"]');
    await expect(modal).toBeVisible({ timeout: 5_000 });
    await expect(
      page.locator('[data-testid="query-results-accession-input"]'),
    ).toBeVisible();

    await page.locator('[data-testid="query-results-close-button"]').click();
    await expect(modal).not.toBeVisible();
  });
});
