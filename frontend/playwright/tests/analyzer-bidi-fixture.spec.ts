import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { GENEXPERT_FIXTURE_ID } from "../fixtures/analyzer-constants";
import {
  isMockSimulateAvailable,
  triggerMockResultsPush,
} from "../utils/harness-preflight";

/**
 * Fixture-backed bidirectional specs using analyzer 2013 (minimal GeneXpert).
 * Requires analyzer harness: mock simulator and OpenELIS running.
 */
test.describe("Analyzer bidirectional fixture-backed", () => {
  test.skip(
    process.env.CI === "true",
    "Requires analyzer harness with fixture data (not available in CI)",
  );

  test.beforeEach(async () => {
    const available = await isMockSimulateAvailable();
    if (!available) {
      test.skip(
        true,
        "Mock simulator not reachable at MOCK_SIMULATE_URL (default http://localhost:8085). Start analyzer harness.",
      );
    }
  });

  test("fixture Send Order modal submit shows success or error", async ({
    page,
  }) => {
    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();

    await list.openOverflowMenu(GENEXPERT_FIXTURE_ID);
    await list.clickAction(GENEXPERT_FIXTURE_ID, "send-order");

    const modal = page.locator('[data-testid="send-order-modal"]');
    await expect(modal).toBeVisible({ timeout: 5_000 });

    await page
      .locator('[data-testid="send-order-accession-input"]')
      .fill("E2E-ACC-001");
    await page.locator('[data-testid="send-order-submit-button"]').click();

    await expect(
      page.locator(
        '[data-testid="send-order-success"], [data-testid="send-order-error"]',
      ),
    ).toBeVisible({ timeout: 15_000 });
  });

  test("fixture Query Results modal submit shows success or error", async ({
    page,
  }) => {
    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();

    await list.openOverflowMenu(GENEXPERT_FIXTURE_ID);
    await list.clickAction(GENEXPERT_FIXTURE_ID, "query-results");

    const modal = page.locator('[data-testid="query-results-modal"]');
    await expect(modal).toBeVisible({ timeout: 5_000 });

    await page
      .locator('[data-testid="query-results-accession-input"]')
      .fill("E2E-ACC-002");
    await page.locator('[data-testid="query-results-submit-button"]').click();

    await expect(
      page.locator(
        '[data-testid="query-results-success"], [data-testid="query-results-error"]',
      ),
    ).toBeVisible({ timeout: 15_000 });
  });

  test("fixture results-push: mock push then result visible in Analyzer Results UI", async ({
    page,
  }) => {
    const baseURL = process.env.BASE_URL || "https://localhost";
    const res = await triggerMockResultsPush(1, baseURL);
    if (!res.ok) {
      test.skip(
        true,
        `Mock push failed (status ${res.status}). Ensure harness and OE are running.`,
      );
    }

    await page.goto("/AnalyzerResults", { waitUntil: "domcontentloaded" });
    await page.waitForLoadState("networkidle").catch(() => {});

    const sampleInfoCells = page.locator('[data-testid="sampleInfo"]');
    await expect(sampleInfoCells.first()).toBeVisible({ timeout: 20_000 });
  });
});
