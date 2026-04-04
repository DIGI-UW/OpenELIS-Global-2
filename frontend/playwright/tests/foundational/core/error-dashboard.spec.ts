import { test, expect } from "@playwright/test";
import { ErrorDashboardPage } from "../../../fixtures/error-dashboard";

test.describe("Error Dashboard Page", () => {
  let dashboard: ErrorDashboardPage;

  test.beforeEach(async ({ page }) => {
    dashboard = new ErrorDashboardPage(page);
    await dashboard.goto();
    await dashboard.expectLoaded();
  });

  test("loads with header, stats, and table", async () => {
    await expect(dashboard.tableContainer).toBeVisible();
  });

  test("displays four statistics cards", async ({ page }) => {
    await expect(page.locator('[data-testid="stat-total"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="stat-unacknowledged"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="stat-critical"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="stat-last24hours"]'),
    ).toBeVisible();
  });

  test("has Acknowledge All button", async () => {
    await expect(dashboard.acknowledgeAllButton).toBeVisible();
  });

  test("has filter bar with search and dropdowns", async ({ page }) => {
    await expect(dashboard.filtersSection).toBeVisible();
    await expect(dashboard.searchInput).toBeVisible();
    await expect(
      page.locator('[data-testid="error-type-filter"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="severity-filter"]')).toBeVisible();
  });

  test("table renders with column headers", async () => {
    const headers = dashboard.table.locator("thead th");
    await expect(headers).toHaveCount(7);
  });
});
