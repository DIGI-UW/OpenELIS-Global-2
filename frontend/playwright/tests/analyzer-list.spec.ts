import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";

test.describe("Analyzer List Page", () => {
  test("loads with header, stats, and table", async ({ page }) => {
    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();

    // Table container should be present (even if empty)
    await expect(list.tableContainer).toBeVisible();
  });

  test("displays statistics cards", async ({ page }) => {
    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();

    // All three stat tiles should be visible
    await expect(page.locator('[data-testid="stat-total"]')).toBeVisible();
    await expect(page.locator('[data-testid="stat-active"]')).toBeVisible();
    await expect(page.locator('[data-testid="stat-inactive"]')).toBeVisible();
  });

  test("has Add Analyzer button", async ({ page }) => {
    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();

    await expect(list.addButton).toBeVisible();
  });

  test("has search input and status filter", async ({ page }) => {
    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();

    await expect(list.searchInput).toBeVisible();
    await expect(
      page.locator('[data-testid="analyzer-status-filter"]'),
    ).toBeVisible();
  });

  test("table renders with column headers", async ({ page }) => {
    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();

    // Table should have expected column headers
    const headers = list.table.locator("thead th");
    await expect(headers).toHaveCount(7);
  });
});
