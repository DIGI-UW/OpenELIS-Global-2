import { expect, Page, test } from "@playwright/test";
import { createDemoPresentation } from "../helpers/demo-presentation";
import type { DemoPresentation } from "../helpers/demo-presentation";
import {
  findAnalyzerRow,
  goToAnalyzerDashboard,
} from "../helpers/analyzer-dashboard";
import {
  accessionTextRegExp,
  openAnalyzerResultsAndWaitForText,
} from "../helpers/results-ui";
import { SHORT_TIMEOUT, UI_TIMEOUT, LONG_TIMEOUT } from "../helpers/timeouts";

const SIMULATOR_URL = "http://localhost:8085";
const MLLP_DESTINATION = "mllp://openelis-analyzer-bridge:2575";
const RESULTS_TIMEOUT = 90_000;

/**
 * Mindray HL7 analyzer definitions.
 *
 * Each analyzer pushes via MLLP → bridge → OE HL7 reader → AnalyzerResults.
 * The filler ID ("FILLER012") becomes the accession number in OE because the
 * HL7 generator puts testSample.id into the OBR placer field and "FILLER012"
 * as the filler order ID, and OE's HL7 parser extracts the filler from OBR-3.
 */
const ANALYZERS = [
  {
    name: "Mindray BC-5380",
    template: "mindray_bc5380",
    accession: "FILLER012",
    expectedTests: [
      { testName: "WBC", result: "7.5" },
      { testName: "RBC", result: "4.82" },
      { testName: "HGB", result: "14.2" },
      { testName: "HCT", result: "42" },
    ],
  },
  {
    name: "Mindray BS-200",
    template: "mindray_bs200",
    accession: "FILLER012",
    expectedTests: [
      { testName: "CREA", result: "1.1" },
      { testName: "ALT", result: "32" },
      { testName: "AST", result: "28" },
      { testName: "GLU", result: "92" },
    ],
  },
  {
    name: "Mindray BS-300",
    template: "mindray_bs300",
    accession: "FILLER012",
    expectedTests: [
      { testName: "CREA", result: "0.8" },
      { testName: "ALT", result: "19" },
      { testName: "AST", result: "24" },
      { testName: "GLU", result: "88" },
    ],
  },
];

async function pushHl7Message(
  page: Page,
  template: string,
  presentation: DemoPresentation,
) {
  const response = await page.request.post(
    `${SIMULATOR_URL}/simulate/hl7/${template}`,
    {
      data: { destination: MLLP_DESTINATION, count: 1 },
    },
  );
  expect(response.ok()).toBeTruthy();
  await presentation.pause(1_000);
}

async function verifyResults(
  page: Page,
  analyzerName: string,
  accession: string,
  expectedTests: { testName: string; result: string }[],
  presentation: DemoPresentation,
) {
  await openAnalyzerResultsAndWaitForText(page, analyzerName, accession, {
    timeoutMs: RESULTS_TIMEOUT,
    perAttemptTimeoutMs: LONG_TIMEOUT,
  });

  const resultsRegion = page.locator(".orderLegendBody, table").first();
  await expect(resultsRegion).toBeVisible({ timeout: UI_TIMEOUT });

  await expect(
    resultsRegion.getByText(accessionTextRegExp(accession)).first(),
  ).toBeVisible({ timeout: UI_TIMEOUT });

  for (const expected of expectedTests) {
    const inputResult = resultsRegion
      .locator(`input[value*="${expected.result}"]`)
      .first();
    let inputVisible = false;
    try {
      await expect(inputResult).toBeVisible({ timeout: SHORT_TIMEOUT });
      inputVisible = true;
    } catch {
      // Input field not found — try text match instead
    }
    if (!inputVisible) {
      await expect(
        resultsRegion.getByText(expected.result, { exact: false }).first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    }
  }

  await presentation.pause(2_000);
}

test.describe("Mindray HL7 MLLP demo story", () => {
  test.setTimeout(240_000);

  for (const analyzer of ANALYZERS) {
    test(`push and review HL7 results — ${analyzer.name}`, async ({
      page,
    }, testInfo) => {
      const presentation = createDemoPresentation(page, testInfo);

      await presentation.title(
        `${analyzer.name} HL7 Results`,
        "Push HL7 via MLLP, review staged results in OpenELIS.",
      );

      await presentation.step(1, `Find ${analyzer.name} on the dashboard`);
      await goToAnalyzerDashboard(page, testInfo);
      await findAnalyzerRow(page, analyzer.name, testInfo);

      await presentation.step(
        2,
        `Send HL7 message via MLLP → Bridge → OpenELIS`,
      );
      await pushHl7Message(page, analyzer.template, presentation);

      await presentation.step(3, "Review staged results");
      await verifyResults(
        page,
        analyzer.name,
        analyzer.accession,
        analyzer.expectedTests,
        presentation,
      );

      await presentation.title(
        "HL7 Results Verified",
        `${analyzer.name}: ${analyzer.expectedTests.length} test results staged successfully.`,
      );
    });
  }
});
