import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { AnalyzerFormPage } from "../fixtures/analyzer-form";

/**
 * Analyzer Test Connection E2E
 *
 * Two test scenarios:
 * 1. Mock: fixture-loaded GeneXpert at 172.20.1.100:9600 (ASTM mock in Docker)
 * 2. Real: dynamically-created analyzer at 35.82.68.83:1200 (GeneXpert Dx VM)
 *    Skipped in CI — requires the physical VM to be running.
 *
 * Both require the analyzer harness (bridge) to be running.
 */
test.describe("Analyzer Test Connection", () => {
  test("GeneXpert test-connection succeeds via ASTM mock", async ({ page }) => {
    const GENEXPERT_ID = "2013";
    const list = new AnalyzerListPage(page);

    await list.goto();
    await list.expectLoaded();

    const row = list.getRow(GENEXPERT_ID);
    await expect(row).toBeVisible({ timeout: 10_000 });

    await list.openOverflowMenu(GENEXPERT_ID);
    await list.clickAction(GENEXPERT_ID, "test-connection");

    const modal = page.locator('[data-testid="test-connection-modal"]');
    await expect(modal).toBeVisible();

    const info = page.locator('[data-testid="test-connection-analyzer-info"]');
    await expect(info).toContainText("GeneXpert");

    const testButton = page.locator(
      '[data-testid="test-connection-test-button"]',
    );
    await testButton.click();

    const successTag = page.locator('[data-testid="test-connection-success"]');
    await expect(successTag).toBeVisible({ timeout: 15_000 });

    const errorTag = page.locator('[data-testid="test-connection-error"]');
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
 * Creates an analyzer pointing to a real GeneXpert Dx v6.5 Windows VM,
 * tests the connection (which triggers ASTM line contention handling),
 * and cleans up. Requires the VM at 35.82.68.83:1200 to be running.
 *
 * Skipped in CI — the GeneXpert VM is only reachable from the dev server.
 */
test.describe("Real GeneXpert Test Connection", () => {
  test.skip(!!process.env.CI, "Real GeneXpert VM not available in CI");
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
    await expect(pluginOption.first()).toBeVisible({ timeout: 10_000 });
    await pluginOption.first().click();

    await form.selectType("Molecular");
    await form.fillIpAddress("35.82.68.83");
    await form.fillPort("1200");

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
    await expect(info).toContainText("35.82.68.83");
    await expect(info).toContainText("1200");

    // Click Test — bridge will handle ASTM line contention with real GeneXpert
    const testButton = page.locator(
      '[data-testid="test-connection-test-button"]',
    );
    await testButton.click();

    // Real GeneXpert + contention handling may take longer than mock
    const successTag = page.locator('[data-testid="test-connection-success"]');
    await expect(successTag).toBeVisible({ timeout: 30_000 });

    const errorTag = page.locator('[data-testid="test-connection-error"]');
    await expect(errorTag).not.toBeVisible();

    const closeButton = page.locator(
      '[data-testid="test-connection-close-button"]',
    );
    await closeButton.click();
    await expect(modal).not.toBeVisible();
  });
});
