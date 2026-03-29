import { test, expect } from "@playwright/test";
import { ErrorDashboardPage } from "../fixtures/error-dashboard";

test.describe("Error Dashboard Page", () => {
  test("loads with header, stats, and table", async ({ page }) => {
    const dashboard = new ErrorDashboardPage(page);
    await dashboard.goto();
    await dashboard.expectLoaded();

    // Table container should be present (even if empty)
    await expect(dashboard.tableContainer).toBeVisible();
  });

  test("displays four statistics cards", async ({ page }) => {
    const dashboard = new ErrorDashboardPage(page);
    await dashboard.goto();
    await dashboard.expectLoaded();

    await expect(page.locator('[data-testid="stat-total"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="stat-unacknowledged"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="stat-critical"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="stat-last24hours"]'),
    ).toBeVisible();
  });

  test("has Acknowledge All button", async ({ page }) => {
    const dashboard = new ErrorDashboardPage(page);
    await dashboard.goto();
    await dashboard.expectLoaded();

    await expect(dashboard.acknowledgeAllButton).toBeVisible();
  });

  test("has filter bar with search and dropdowns", async ({ page }) => {
    const dashboard = new ErrorDashboardPage(page);
    await dashboard.goto();
    await dashboard.expectLoaded();

    await expect(dashboard.filtersSection).toBeVisible();
    await expect(dashboard.searchInput).toBeVisible();
    await expect(
      page.locator('[data-testid="error-type-filter"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="severity-filter"]')).toBeVisible();
  });

  test("table renders with column headers", async ({ page }) => {
    const dashboard = new ErrorDashboardPage(page);
    await dashboard.goto();
    await dashboard.expectLoaded();

    const headers = dashboard.table.locator("thead th");
    await expect(headers).toHaveCount(7);
  });
});
