import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerFormPage } from "../../../fixtures/analyzer-form";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

const GENEXPERT_HOST = process.env.GENEXPERT_HOST;
const GENEXPERT_PORT = process.env.GENEXPERT_PORT || "1200";

/**
 * Manual-only real-device connectivity coverage.
 *
 * This spec is intentionally excluded from ordinary PR CI. It is for explicit
 * operator-driven runs against real hardware.
 */
test.describe("Real GeneXpert Test Connection (Manual Only)", () => {
  test.skip(
    !GENEXPERT_HOST || process.env.CI === "true",
    "Set GENEXPERT_HOST (and optionally GENEXPERT_PORT) and run outside CI",
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
        // Best effort cleanup.
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

    await form.fillName(analyzerName);

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

    const info = page.locator('[data-testid="test-connection-analyzer-info"]');
    await expect(info).toContainText(GENEXPERT_HOST!);
    await expect(info).toContainText(GENEXPERT_PORT);

    const testButton = page.locator(
      '[data-testid="test-connection-test-button"]',
    );
    await testButton.click();

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
