import { expect, Locator, Page, test } from "@playwright/test";
import { acceptAndVerifyResults } from "../helpers/accept-results";
import { createDemoPresentation } from "../helpers/demo-presentation";
import type { DemoPresentation } from "../helpers/demo-presentation";
import {
  findAnalyzerRow,
  goToAnalyzerDashboard,
} from "../helpers/analyzer-dashboard";
import { cleanupAnalyzersMatching } from "../helpers/cleanup-analyzer";
import { openAnalyzerResultsAndWaitForText } from "../helpers/results-ui";
import { UI_TIMEOUT } from "../helpers/timeouts";

const SIMULATOR_URL = "http://localhost:8085";
const BRIDGE_DESTINATION = "tcp://openelis-analyzer-bridge:12001";
const PRELOADED_NAME = "Cepheid GeneXpert (ASTM Mode)";
const RESULTS_TIMEOUT = 90_000;

const EXPECTED_RESULTS = [
  { sampleId: "E2E001", result: "NEGATIVE" },
  { sampleId: "E2E001", result: "Sensitive" },
  { sampleId: "E2E001", result: "1250" },
];

async function testConnection(
  page: Page,
  analyzerRow: Locator,
  presentation: DemoPresentation,
) {
  const overflow = analyzerRow
    .first()
    .locator('[data-testid^="analyzer-row-overflow-"]')
    .first();
  await overflow.click();
  await presentation.pause(500);

  const testConnectionAction = page
    .locator('[data-testid*="analyzer-action-test-connection"]')
    .first();
  await expect(testConnectionAction).toBeVisible({ timeout: 3_000 });
  await testConnectionAction.click();

  const connectionModal = page.locator('[data-testid="test-connection-modal"]');
  await expect(connectionModal).toBeVisible({ timeout: 10_000 });

  const testButton = page.locator(
    '[data-testid="test-connection-test-button"]',
  );
  await testButton.click();

  const successTag = page.locator('[data-testid="test-connection-success"]');
  await expect(successTag).toBeVisible({ timeout: 15_000 });
  await presentation.pause(1_500);

  await connectionModal
    .locator('[data-testid="test-connection-close-button"]')
    .click();
  await expect(connectionModal).toBeHidden({ timeout: 10_000 });
}

async function pushAstmMessage(page: Page, presentation: DemoPresentation) {
  await page.request.post(`${SIMULATOR_URL}/simulate/astm/genexpert_astm`, {
    data: { destination: BRIDGE_DESTINATION, count: 1 },
  });
  await presentation.pause(1_000);
}

async function verifyResults(
  page: Page,
  analyzerName: string,
  presentation: DemoPresentation,
) {
  await openAnalyzerResultsAndWaitForText(
    page,
    analyzerName,
    EXPECTED_RESULTS[0].sampleId,
    { timeoutMs: RESULTS_TIMEOUT, perAttemptTimeoutMs: 15_000 },
  );

  const resultsRegion = page.locator(".orderLegendBody, table").first();
  await expect(resultsRegion).toBeVisible({ timeout: UI_TIMEOUT });

  for (const expected of EXPECTED_RESULTS) {
    await expect(
      resultsRegion.getByText(expected.sampleId, { exact: false }).first(),
    ).toBeVisible({ timeout: UI_TIMEOUT });

    const inputResult = resultsRegion
      .locator(`input[value*="${expected.result}"]`)
      .first();
    if (await inputResult.isVisible().catch(() => false)) {
      await expect(inputResult).toBeVisible({ timeout: UI_TIMEOUT });
    } else {
      await expect(
        resultsRegion.getByText(expected.result, { exact: false }).first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    }
  }

  await presentation.pause(2_000);
}

test.describe("GeneXpert ASTM demo story", () => {
  test.setTimeout(180_000);

  test("review and accept staged ASTM results", async ({ page }, testInfo) => {
    const presentation = createDemoPresentation(page, testInfo);

    await presentation.title(
      "GeneXpert ASTM Results",
      "Find analyzer, review staged results, and accept them.",
    );

    await goToAnalyzerDashboard(page, testInfo);

    await cleanupAnalyzersMatching(
      page,
      /Cepheid GeneXpert \(ASTM Mode\) E2E/i,
    );

    await presentation.step(1, "Find the pre-loaded GeneXpert analyzer");
    const analyzerRow = await findAnalyzerRow(page, PRELOADED_NAME, testInfo);

    await presentation.step(2, "Confirm the analyzer connection");
    await testConnection(page, analyzerRow, presentation);

    await presentation.step(3, "Send a GeneXpert ASTM message");
    await pushAstmMessage(page, presentation);

    await presentation.step(4, "Review the staged results");
    await verifyResults(page, PRELOADED_NAME, presentation);

    await acceptAndVerifyResults(
      page,
      presentation,
      4,
      EXPECTED_RESULTS[0].sampleId,
    );

    await presentation.title(
      "Story Complete",
      "The GeneXpert workflow stayed UI-only in both demo modes.",
    );
  });
});
