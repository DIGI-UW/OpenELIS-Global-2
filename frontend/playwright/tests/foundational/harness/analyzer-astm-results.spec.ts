import type { Locator, Page } from "@playwright/test";
import { expect, test } from "../../../helpers/test-base";
import { acceptAndVerifyResults } from "../../../helpers/accept-results";
import {
  findAnalyzerRow,
  goToAnalyzerDashboard,
} from "../../../helpers/analyzer-dashboard";
import { cleanupAnalyzersMatching } from "../../../helpers/cleanup-analyzer";
import {
  accessionTextRegExp,
  expectResultVisible,
  openAnalyzerResultsAndWaitForText,
} from "../../../helpers/results-ui";
import {
  SHORT_TIMEOUT,
  UI_TIMEOUT,
  LONG_TIMEOUT,
} from "../../../helpers/timeouts";

const SIMULATOR_URL = process.env.MOCK_SIMULATOR_URL || "http://localhost:8085";
// Use the bridge IP on the dedicated GeneXpert mock subnet so the simulator
// source IP is the registered GeneXpert mock IP (10.42.20.10).
const BRIDGE_DESTINATION = "tcp://10.42.20.2:12001";
const PRELOADED_NAME = "Cepheid GeneXpert (ASTM Mode)";
const FIXTURE_SAMPLE_ID = "DEV01261000000000001";
const RESULTS_TIMEOUT = 90_000;

const EXPECTED_RESULT = "NEGATIVE";

async function testConnection(page: Page, analyzerRow: Locator) {
  const overflow = analyzerRow
    .first()
    .locator('[data-testid^="analyzer-row-overflow-"]')
    .first();
  await overflow.click();

  const testConnectionAction = page
    .locator('[data-testid*="analyzer-action-test-connection"]')
    .first();
  await expect(testConnectionAction).toBeVisible({ timeout: SHORT_TIMEOUT });
  await testConnectionAction.click();

  const connectionModal = page.locator('[data-testid="test-connection-modal"]');
  await expect(connectionModal).toBeVisible({ timeout: UI_TIMEOUT });

  const triggerButton = connectionModal
    .locator(
      '[data-testid="test-connection-test-button"], button:has-text("Test Again")',
    )
    .first();
  const successTag = page.locator('[data-testid="test-connection-success"]');
  const errorTag = page.locator('[data-testid="test-connection-error"]');

  let connected = false;
  for (let attempt = 1; attempt <= 3; attempt++) {
    await expect(triggerButton).toBeVisible({ timeout: SHORT_TIMEOUT });
    await triggerButton.click();
    try {
      await expect(successTag).toBeVisible({ timeout: LONG_TIMEOUT });
      connected = true;
      break;
    } catch {
      if (attempt < 3) {
        await expect(successTag.or(errorTag)).toBeVisible({
          timeout: SHORT_TIMEOUT,
        });
      }
    }
  }
  expect(connected).toBeTruthy();

  await connectionModal
    .locator('[data-testid="test-connection-close-button"]')
    .click();
  await expect(connectionModal).toBeHidden({ timeout: UI_TIMEOUT });
}

async function pushAstmMessage(page: Page): Promise<string> {
  const response = await page.request.post(
    `${SIMULATOR_URL}/simulate/astm/genexpert_astm`,
    {
      data: {
        destination: BRIDGE_DESTINATION,
        count: 1,
        sample_id: FIXTURE_SAMPLE_ID,
      },
    },
  );
  const body = await response.json();
  const sampleId = body?.results?.[0]?.sample_id;
  if (!sampleId) throw new Error("Push returned no sample_id");
  return sampleId;
}

async function verifyResults(
  page: Page,
  analyzerName: string,
  sampleId: string,
) {
  await openAnalyzerResultsAndWaitForText(page, analyzerName, sampleId, {
    timeoutMs: RESULTS_TIMEOUT,
    perAttemptTimeoutMs: LONG_TIMEOUT,
  });

  const resultsRegion = page.locator(".orderLegendBody, table").first();
  await expect(resultsRegion).toBeVisible({ timeout: UI_TIMEOUT });

  await expect(
    resultsRegion.getByText(accessionTextRegExp(sampleId)).first(),
  ).toBeVisible({ timeout: UI_TIMEOUT });
  await expectResultVisible(resultsRegion, EXPECTED_RESULT);
}

test.describe("GeneXpert ASTM harness integration", () => {
  test.setTimeout(180_000);

  test("review and accept staged ASTM results", async ({ page }, testInfo) => {
    await goToAnalyzerDashboard(page, testInfo);

    await cleanupAnalyzersMatching(
      page,
      /Cepheid GeneXpert \(ASTM Mode\) E2E/i,
    );

    const analyzerRow = await findAnalyzerRow(page, PRELOADED_NAME, testInfo);

    await testConnection(page, analyzerRow);

    const sampleId = await pushAstmMessage(page);

    await verifyResults(page, PRELOADED_NAME, sampleId);

    await acceptAndVerifyResults(page, sampleId);
  });
});
