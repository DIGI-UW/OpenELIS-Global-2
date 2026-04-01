import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { AnalyzerFormPage } from "../fixtures/analyzer-form";
import {
  ensureAnalyzerByName,
  GENEXPERT_DEFAULT_ANALYZER,
} from "../helpers/ensure-analyzer";
import { SHORT_TIMEOUT, UI_TIMEOUT, LONG_TIMEOUT } from "../helpers/timeouts";

/**
 * Analyzer Test Connection E2E
 *
 * Two test scenarios:
 * 1. Mock: fixture-loaded GeneXpert at 10.42.20.10:9600 (dedicated ASTM mock subnet)
 * 2. Real: dynamically-created analyzer pointing to a real device.
 *    Requires GENEXPERT_HOST and GENEXPERT_PORT env vars to be set.
 *    Skipped automatically when not configured or in CI.
 *
 * Both require the analyzer harness (bridge) to be running.
 */

const GENEXPERT_HOST = process.env.GENEXPERT_HOST;
const GENEXPERT_PORT = process.env.GENEXPERT_PORT || "1200";
test.describe("Analyzer Test Connection", () => {
  test.setTimeout(180_000);

  test("GeneXpert test-connection succeeds via ASTM mock", async ({ page }) => {
    const GENEXPERT_ID = await ensureAnalyzerByName(
      page.request,
      (a) => a.name?.includes("GeneXpert") && !a.name?.includes("E2E"),
      GENEXPERT_DEFAULT_ANALYZER,
    );

    const list = new AnalyzerListPage(page);

    await list.goto();
    await list.expectLoaded();

    const row = list.getRow(GENEXPERT_ID);
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });

    await list.openOverflowMenu(GENEXPERT_ID);
    await list.clickAction(GENEXPERT_ID, "test-connection");

    const modal = page.locator('[data-testid="test-connection-modal"]');
    await expect(modal).toBeVisible();

    const info = page.locator('[data-testid="test-connection-analyzer-info"]');
    await expect(info).toContainText("GeneXpert");

    const testButton = page.locator(
      '[data-testid="test-connection-test-button"]',
    );
    const successTag = page.locator('[data-testid="test-connection-success"]');
    const successText = page.getByText(/Connection Successful/i);
    const errorTag = page.locator('[data-testid="test-connection-error"]');
    const logsButton = page.getByRole("button", { name: /connection logs/i });
    const retryButton = page
      .locator(
        '[data-testid="test-connection-test-button"], button:has-text("Test Again")',
      )
      .first();

    // Test connection can be briefly flaky right after harness restarts.
    // Retry a few times, but fail with explicit UI error details if it never succeeds.
    // Click test and wait for result. The bridge ASTM round-trip takes ~2s,
    // so we use expect().toBeVisible() which auto-retries (unlike isVisible()
    // which returns immediately and ignores the timeout option).
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
        // Success tag didn't appear — check logs for details
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
      // Wait for "Test Again" button before retrying
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

/**
 * Real GeneXpert Dx VM Test Connection
 *
 * Creates an analyzer pointing to a real GeneXpert device and tests the
 * connection (which triggers ASTM line contention handling).
 *
 * Configure via environment variables:
 *   GENEXPERT_HOST=35.82.68.83  GENEXPERT_PORT=1200  npx playwright test analyzer-test-connection
 *
 * Skipped when GENEXPERT_HOST is not set or in CI.
 */
test.describe("Real GeneXpert Test Connection", () => {
  test.skip(
    !GENEXPERT_HOST || process.env.CI === "true",
    "Set GENEXPERT_HOST (and optionally GENEXPERT_PORT) to run real device tests",
  );
  test.describe.configure({ mode: "serial" });

  const uniqueSuffix = Date.now();
  const analyzerName = `E2E-GeneXpert-Real-${uniqueSuffix}`;
  let createdAnalyzerId: string;

  test.afterAll(async ({ request }) => {
    if (process.env.SKIP_CLEANUP) return;
    if (createdAnalyzerId) {
      try {
        await request.post(
          `/rest/analyzer/analyzers/${createdAnalyzerId}/delete`,
        );
      } catch {
        // Best effort cleanup
      }
    }
  });

  test("creates analyzer pointing to real GeneXpert VM", async ({ page }) => {
    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    await list.goto();
    await list.expectLoaded();
    await list.clickAdd();
    await form.expectOpen();

    // Fill form with real GeneXpert VM details
    await form.fillName(analyzerName);

    // Plugin Type loads async — wait for options before selecting
    await form.pluginTypeDropdown.click();
    const pluginOption = page.getByRole("option", { name: /Generic ASTM/ });
    await expect(pluginOption.first()).toBeVisible({ timeout: UI_TIMEOUT });
    await pluginOption.first().click();

    await form.selectType("Molecular");
    await form.fillIpAddress(GENEXPERT_HOST!);
    await form.fillPort(GENEXPERT_PORT);

    await form.save();
    await form.expectSuccessNotification();
    await expect(form.modal).not.toBeVisible();

    // Find the created analyzer and store its ID for cleanup
    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);

    const rows = page.locator("tbody tr");
    await expect(rows).toHaveCount(1);

    const row = rows.first();
    const testid = await row.getAttribute("data-testid");
    if (testid && testid.startsWith("analyzer-row-")) {
      createdAnalyzerId = testid.replace("analyzer-row-", "");
    }

    expect(createdAnalyzerId).toBeTruthy();

    // Verify no "Plugin Missing" warning on the created analyzer
    const pluginWarning = page.locator(
      `[data-testid="plugin-warning-${createdAnalyzerId}"]`,
    );
    await expect(pluginWarning).not.toBeVisible();
  });

  test("test-connection succeeds to real GeneXpert Dx VM", async ({ page }) => {
    test.skip(!createdAnalyzerId, "Requires analyzer from previous test");

    const list = new AnalyzerListPage(page);

    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);

    await list.openOverflowMenu(createdAnalyzerId);
    await list.clickAction(createdAnalyzerId, "test-connection");

    const modal = page.locator('[data-testid="test-connection-modal"]');
    await expect(modal).toBeVisible();

    // Verify analyzer info shows the real GeneXpert details
    const info = page.locator('[data-testid="test-connection-analyzer-info"]');
    await expect(info).toContainText(GENEXPERT_HOST!);
    await expect(info).toContainText(GENEXPERT_PORT);

    // Click Test — bridge will handle ASTM line contention with real GeneXpert
    const testButton = page.locator(
      '[data-testid="test-connection-test-button"]',
    );
    await testButton.click();

    // Real GeneXpert + contention handling may take longer than mock
    const successTag = page.locator('[data-testid="test-connection-success"]');
    await expect(successTag).toBeVisible({ timeout: LONG_TIMEOUT });

    const errorTag = page.locator('[data-testid="test-connection-error"]');
    await expect(errorTag).not.toBeVisible();

    const closeButton = page.locator(
      '[data-testid="test-connection-close-button"]',
    );
    await closeButton.click();
    await expect(modal).not.toBeVisible();
  });
});
