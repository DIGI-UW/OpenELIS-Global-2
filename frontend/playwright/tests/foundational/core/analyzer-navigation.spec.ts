import { test, expect } from "../../../helpers/test-base";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";

test.describe("Analyzer Pages Navigation", () => {
  test("navigates to analyzer list page", async ({ page }) => {
    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();

    await expect(page).toHaveURL(/\/analyzers$/);
  });
});
