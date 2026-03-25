import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { ASTM_FIXTURE_IDS } from "../fixtures/analyzer-constants";
import { isMockSimulateAvailable } from "../utils/harness-preflight";

/**
 * Matrix test: Test Connection (and optionally Query Results) for each ASTM
 * fixture when harness is running with multi-port mock (--analyzers=astm-full).
 * One mock port per fixture; template selected by port.
 */
test.describe("Analyzer bidirectional matrix (ASTM fixtures)", () => {
  test.skip(
    process.env.CI === "true",
    "Requires analyzer harness with fixture data (not available in CI)",
  );

  test.beforeEach(async () => {
    const available = await isMockSimulateAvailable();
    if (!available) {
      test.skip(
        true,
        "Mock simulator not reachable. Start analyzer harness (multi-port mock).",
      );
    }
  });

  for (const analyzerId of ASTM_FIXTURE_IDS) {
    test(`fixture ${analyzerId}: Test Connection shows success or error`, async ({
      page,
    }) => {
      const list = new AnalyzerListPage(page);
      await list.goto();
      await list.expectLoaded();

      const row = list.getRow(analyzerId);
      await expect(row).toBeVisible({ timeout: 10_000 });

      await list.openOverflowMenu(analyzerId);
      await list.clickAction(analyzerId, "test-connection");

      const modal = page.locator('[data-testid="test-connection-modal"]');
      await expect(modal).toBeVisible({ timeout: 5_000 });

      const testButton = page.locator(
        '[data-testid="test-connection-test-button"]',
      );
      await testButton.click();

      await expect(
        page.locator(
          '[data-testid="test-connection-success"], [data-testid="test-connection-error"]',
        ),
      ).toBeVisible({ timeout: 15_000 });
    });
  }
});
