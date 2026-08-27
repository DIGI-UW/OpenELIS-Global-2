import { expect, Locator, Page, test } from "../../../helpers/test-base";
import { acceptAndVerifyResults } from "../../../helpers/accept-results";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import type { DemoPresentation } from "../../../helpers/demo-presentation";
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

const SIMULATOR_URL = "http://localhost:8085";
// Use the bridge IP on the dedicated GeneXpert mock subnet so the simulator
// source IP is the registered GeneXpert mock IP (10.42.20.10).
const BRIDGE_DESTINATION = "tcp://10.42.20.2:12001";
const PRELOADED_NAME = "Cepheid GeneXpert (ASTM Mode)";
const FIXTURE_SAMPLE_ID = "DEV01261000000000001";
const RESULTS_TIMEOUT = 90_000;

const EXPECTED_RESULT = "NEGATIVE";

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
  await presentation.pause(1_500);

  await connectionModal
    .locator('[data-testid="test-connection-close-button"]')
    .click();
  await expect(connectionModal).toBeHidden({ timeout: UI_TIMEOUT });
}

// The GeneXpert fixture carries an accented patient name on purpose, so this
// lane exercises the non-ASCII path over ASTM TCP. A receiver that mis-decodes
// those bytes computes the frame checksum over the wrong ones and NAKs the
// frame, which is how MG-97 reached production: the lane was ASCII-only, so it
// passed against a bridge that failed at a French-language site. Asserting the
// fixture is still non-ASCII keeps that coverage from silently going away.
async function assertFixtureCarriesNonAscii(page: Page) {
  const response = await page.request.get(
    `${SIMULATOR_URL}/simulate/astm/genexpert_astm`,
  );
  const message: string = (await response.json())?.message ?? "";
  expect(
    message,
    "GeneXpert fixture must contain non-ASCII text so this lane covers the Latin-1 path (MG-97)",
  ).toMatch(/[^\x00-\x7F]/);
}

async function pushAstmMessage(
  page: Page,
  presentation: DemoPresentation,
): Promise<string> {
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
  const result = body?.results?.[0];
  // The simulator reports a sample_id even when the transport rejected the
  // message, so the push flag has to be checked separately. Without this a
  // NAKed frame surfaced as a 90s timeout waiting for results that were never
  // sent, hiding the actual transport error the simulator had already reported.
  if (!result?.pushed) {
    throw new Error(
      `ASTM push was not accepted over ${BRIDGE_DESTINATION}: ${
        result?.error ?? "no error reported"
      }`,
    );
  }
  const sampleId = result.sample_id;
  if (!sampleId) throw new Error("Push returned no sample_id");
  await presentation.pause(1_000);
  return sampleId;
}

async function verifyResults(
  page: Page,
  analyzerName: string,
  sampleId: string,
  presentation: DemoPresentation,
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
    await assertFixtureCarriesNonAscii(page);
    const sampleId = await pushAstmMessage(page, presentation);

    await presentation.step(4, "Review the staged results");
    await verifyResults(page, PRELOADED_NAME, sampleId, presentation);

    await acceptAndVerifyResults(page, presentation, 4, sampleId);

    await presentation.title(
      "Story Complete",
      "The GeneXpert workflow stayed UI-only in both demo modes.",
    );
  });
});
