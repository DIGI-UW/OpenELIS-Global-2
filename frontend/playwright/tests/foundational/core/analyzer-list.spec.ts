import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";

test.describe("Analyzer List Page", () => {
  let list: AnalyzerListPage;

  test.beforeEach(async ({ page }) => {
    list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();
  });

  test("loads with header, stats, and table", async () => {
    await expect(list.tableContainer).toBeVisible();
  });

  test("displays statistics cards", async ({ page }) => {
    await expect(page.locator('[data-testid="stat-total"]')).toBeVisible();
    await expect(page.locator('[data-testid="stat-active"]')).toBeVisible();
    await expect(page.locator('[data-testid="stat-inactive"]')).toBeVisible();
  });

  test("has Add Analyzer button", async () => {
    await expect(list.addButton).toBeVisible();
  });

  test("has search input and status filter", async ({ page }) => {
    await expect(list.searchInput).toBeVisible();
    await expect(
      page.locator('[data-testid="analyzer-status-filter"]'),
    ).toBeVisible();
  });

  test("table renders with column headers", async () => {
    const headers = list.table.locator("thead th");
    await expect(headers).toHaveCount(7);
  });
});
