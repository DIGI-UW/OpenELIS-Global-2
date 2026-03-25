import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { ErrorDashboardPage } from "../fixtures/error-dashboard";

test.describe("Analyzer Pages Navigation", () => {
  test("navigates to analyzer list page", async ({ page }) => {
    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();

    await expect(page).toHaveURL(/\/analyzers$/);
  });

  test("navigates to error dashboard page", async ({ page }) => {
    const dashboard = new ErrorDashboardPage(page);
    await dashboard.goto();
    await dashboard.expectLoaded();

    await expect(page).toHaveURL(/\/analyzers\/errors/);
  });

  test("navigates between analyzer list and error dashboard", async ({
    page,
  }) => {
    // Start at analyzer list
    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();

    // Navigate to error dashboard
    const dashboard = new ErrorDashboardPage(page);
    await dashboard.goto();
    await dashboard.expectLoaded();

    // Navigate back to list
    await list.goto();
    await list.expectLoaded();
  });
});
