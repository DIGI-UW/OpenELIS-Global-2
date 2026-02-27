import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";

/**
 * Analyzer Test Connection E2E
 *
 * Verifies the "Test Connection" flow for a fixture-loaded GeneXpert analyzer.
 * Requires the analyzer harness (mock server + bridge) to be running.
 *
 * Fixture: analyzer ID 2013 — Cepheid GeneXpert ASTM at 172.20.1.100:9600
 */
test.describe("Analyzer Test Connection", () => {
  const GENEXPERT_ID = "2013";

  test("GeneXpert test-connection succeeds via ASTM mock", async ({ page }) => {
    const list = new AnalyzerListPage(page);

    // Navigate to the analyzers list
    await list.goto();
    await list.expectLoaded();

    // Find the GeneXpert row and open its action menu
    const row = list.getRow(GENEXPERT_ID);
    await expect(row).toBeVisible({ timeout: 10_000 });

    await list.openOverflowMenu(GENEXPERT_ID);
    await list.clickAction(GENEXPERT_ID, "test-connection");

    // Modal should open with analyzer info
    const modal = page.locator('[data-testid="test-connection-modal"]');
    await expect(modal).toBeVisible();

    const info = page.locator('[data-testid="test-connection-analyzer-info"]');
    await expect(info).toContainText("GeneXpert");

    // Click the Test button to initiate the connection test
    const testButton = page.locator(
      '[data-testid="test-connection-test-button"]',
    );
    await testButton.click();

    // Wait for the success tag — the mock should respond within a few seconds
    const successTag = page.locator('[data-testid="test-connection-success"]');
    await expect(successTag).toBeVisible({ timeout: 15_000 });

    // Verify no error tag is shown
    const errorTag = page.locator('[data-testid="test-connection-error"]');
    await expect(errorTag).not.toBeVisible();

    // Close the modal
    const closeButton = page.locator(
      '[data-testid="test-connection-close-button"]',
    );
    await closeButton.click();
    await expect(modal).not.toBeVisible();
  });
});
