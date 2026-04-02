import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import {
  ensureAnalyzerByName,
  GENEXPERT_DEFAULT_ANALYZER,
} from "../../../helpers/ensure-analyzer";
import {
  SHORT_TIMEOUT,
  UI_TIMEOUT,
  LONG_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * Harness foundational connectivity check using the seeded ASTM mock analyzer.
 */
test.describe("Analyzer Test Connection (Harness Foundational)", () => {
  test.setTimeout(180_000);

  test("GeneXpert test-connection succeeds via ASTM mock", async ({ page }) => {
    const analyzerId = await ensureAnalyzerByName(
      page.request,
      (a) => a.name?.includes("GeneXpert") && !a.name?.includes("E2E"),
      GENEXPERT_DEFAULT_ANALYZER,
    );

    const list = new AnalyzerListPage(page);

    await list.goto();
    await list.expectLoaded();

    const row = list.getRow(analyzerId);
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });

    await list.openOverflowMenu(analyzerId);
    await list.clickAction(analyzerId, "test-connection");

    const modal = page.locator('[data-testid="test-connection-modal"]');
    await expect(modal).toBeVisible();

    const info = page.locator('[data-testid="test-connection-analyzer-info"]');
    await expect(info).toContainText("GeneXpert");

    const testButton = page.locator(
      '[data-testid="test-connection-test-button"]',
    );
    const successTag = page.locator('[data-testid="test-connection-success"]');
    const errorTag = page.locator('[data-testid="test-connection-error"]');
    const logsButton = page.getByRole("button", { name: /connection logs/i });
    const retryButton = page
      .locator(
        '[data-testid="test-connection-test-button"], button:has-text("Test Again")',
      )
      .first();

    // Bridge startup can briefly race with the first connectivity attempt.
    let connected = false;
    let lastError = "";
    const appendVisibleLogs = async () => {
      if (page.isClosed()) {
        return;
      }
      try {
        if (!(await logsButton.isVisible())) {
          return;
        }
        await logsButton.click();
        const logs = page.locator(
          '[data-testid="test-connection-logs"], [data-testid="test-connection-log-content"], pre',
        );
        if (await logs.first().isVisible()) {
          const logText = ((await logs.first().textContent()) || "").trim();
          if (logText.length > 0) {
            lastError = `${lastError}\n${logText}`.trim();
          }
        }
      } catch {
        // Best effort only — modal can close while timeout teardown is occurring.
      }
    };

    for (let attempt = 1; attempt <= 3; attempt++) {
      await testButton.click();
      try {
        await expect(successTag.first()).toBeVisible({ timeout: LONG_TIMEOUT });
        connected = true;
        break;
      } catch {
        // Success tag didn't appear — check logs for details.
      }

      await appendVisibleLogs();
      try {
        if (await errorTag.isVisible()) {
          lastError =
            (await errorTag.textContent())?.trim() || "Connection failed";
        }
      } catch {
        // Best effort only when page/modal lifecycle is changing.
      }
      if (attempt < 3) {
        try {
          await expect(retryButton).toBeVisible({ timeout: SHORT_TIMEOUT });
          await retryButton.click();
        } catch {
          await expect(successTag.or(errorTag)).toBeVisible({
            timeout: SHORT_TIMEOUT,
          });
        }
      }
    }

    if (!connected) {
      await appendVisibleLogs();
    }

    expect(
      connected,
      `Mock GeneXpert test-connection should succeed. Last error: ${lastError}`,
    ).toBeTruthy();
    await expect(errorTag).not.toBeVisible();

    const closeButton = page.locator(
      '[data-testid="test-connection-close-button"]',
    );
    await closeButton.click();
    await expect(modal).not.toBeVisible();
  });
});
