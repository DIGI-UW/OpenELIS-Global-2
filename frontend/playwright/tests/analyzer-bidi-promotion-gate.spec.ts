import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { AnalyzerFormPage } from "../fixtures/analyzer-form";
import {
  HARNESS_MOCK_HOST,
  HARNESS_MOCK_PORT,
} from "../fixtures/analyzer-constants";
import { isMockSimulateAvailable } from "../utils/harness-preflight";

/**
 * Promotion gate: prove a newly created analyzer (Generic ASTM, harness mock)
 * can pass test-connection. Run after fixture-backed bidi lane is green.
 * Requires analyzer harness with mock at HARNESS_MOCK_HOST:HARNESS_MOCK_PORT.
 */
test.describe("Analyzer bidirectional promotion gate (new analyzer)", () => {
  test.describe.configure({ mode: "serial" });

  test.skip(
    process.env.CI === "true",
    "Requires analyzer harness (not available in CI)",
  );

  const uniqueSuffix = Date.now();
  const analyzerName = `E2E-Bidi-Promotion-${uniqueSuffix}`;
  let createdAnalyzerId: string | undefined;

  test.beforeEach(async () => {
    const available = await isMockSimulateAvailable();
    if (!available) {
      test.skip(
        true,
        "Mock simulator not reachable. Start analyzer harness for promotion-gate tests.",
      );
    }
  });

  test.afterAll(async ({ request }) => {
    if (process.env.SKIP_CLEANUP === "true" || !createdAnalyzerId) return;
    try {
      await request.post(
        `/rest/analyzer/analyzers/${createdAnalyzerId}/delete`,
      );
    } catch {
      // Best effort cleanup
    }
  });

  test("creates Generic ASTM analyzer pointing at harness mock", async ({
    page,
  }) => {
    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    await list.goto();
    await list.expectLoaded();
    await list.clickAdd();
    await form.expectOpen();

    await form.fillName(analyzerName);

    await form.pluginTypeDropdown.click();
    const pluginOption = page.getByRole("option", { name: /Generic ASTM/ });
    await expect(pluginOption.first()).toBeVisible({ timeout: 10_000 });
    await pluginOption.first().click();

    await form.selectType("Molecular");
    await form.fillIpAddress(HARNESS_MOCK_HOST);
    await form.fillPort(HARNESS_MOCK_PORT);

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
    if (testid?.startsWith("analyzer-row-")) {
      createdAnalyzerId = testid.replace("analyzer-row-", "");
    }
    expect(createdAnalyzerId).toBeTruthy();

    const pluginWarning = page.locator(
      `[data-testid="plugin-warning-${createdAnalyzerId}"]`,
    );
    await expect(pluginWarning).not.toBeVisible();
  });

  test("new analyzer test-connection succeeds against harness mock", async ({
    page,
  }) => {
    test.skip(!createdAnalyzerId, "Requires analyzer from previous test");

    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);

    await list.openOverflowMenu(createdAnalyzerId!);
    await list.clickAction(createdAnalyzerId!, "test-connection");

    const modal = page.locator('[data-testid="test-connection-modal"]');
    await expect(modal).toBeVisible();

    const testButton = page.locator(
      '[data-testid="test-connection-test-button"]',
    );
    await testButton.click();

    const successTag = page.locator('[data-testid="test-connection-success"]');
    await expect(successTag).toBeVisible({ timeout: 15_000 });

    const errorTag = page.locator('[data-testid="test-connection-error"]');
    await expect(errorTag).not.toBeVisible();
  });
});
