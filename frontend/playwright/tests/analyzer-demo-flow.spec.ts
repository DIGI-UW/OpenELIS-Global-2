/**
 * Unified Madagascar Analyzer Demo Flows
 *
 * Single test file that runs the full E2E flow for each supported analyzer:
 *   1. Find (or create) the analyzer on the dashboard
 *   2. Test connection (TCP analyzers only)
 *   3. Push a result (ASTM, HL7 MLLP, or file drop)
 *   4. Verify results appear on the AnalyzerResults page
 *   5. Accept results and verify on AccessionResults page
 *
 * Run all:
 *   npx playwright test analyzer-demo-flow --project=harness-demo
 *
 * Run with video:
 *   npx playwright test analyzer-demo-flow --project=harness-demo-video
 *
 * Run one analyzer:
 *   npx playwright test analyzer-demo-flow --project=harness-demo --grep="BC-5380"
 */

import { expect, test } from "@playwright/test";
import { createDemoPresentation } from "../helpers/demo-presentation";
import {
  findAnalyzerRow,
  goToAnalyzerDashboard,
} from "../helpers/analyzer-dashboard";
import { testAnalyzerConnection } from "../helpers/test-analyzer-connection";
import { pushAnalyzerResult } from "../helpers/push-analyzer-result";
import { acceptAndVerifyResults } from "../helpers/accept-results";
import {
  accessionTextRegExp,
  openAnalyzerResultsAndWaitForText,
} from "../helpers/results-ui";
import { SHORT_TIMEOUT, UI_TIMEOUT, LONG_TIMEOUT } from "../helpers/timeouts";
import type { AnalyzerTestConfig } from "../helpers/analyzer-test-config";

const SIMULATOR_URL = "http://localhost:8085";
const ASTM_DESTINATION = "tcp://openelis-analyzer-bridge:12001";
const MLLP_DESTINATION = "mllp://openelis-analyzer-bridge:2575";
const RESULTS_TIMEOUT = 90_000;

// ── Analyzer Configurations ──────────────────────────────────────

const CONFIGS: AnalyzerTestConfig[] = [
  {
    name: "Cepheid GeneXpert (ASTM Mode)",
    displayName: "GeneXpert ASTM",
    analyzerType: "MOLECULAR",
    pluginType: "Generic ASTM",
    protocol: "ASTM",
    preSeeded: true,
    push: {
      protocol: "ASTM",
      simulatorUrl: SIMULATOR_URL,
      template: "genexpert_astm",
      destination: ASTM_DESTINATION,
    },
    expectedResults: [{ result: "NEGATIVE" }],
  },
  {
    name: "Mindray BC-5380",
    displayName: "Mindray BC-5380 (HL7 Hematology)",
    analyzerType: "HEMATOLOGY",
    pluginType: "Generic HL7",
    protocol: "HL7",
    preSeeded: true,
    push: {
      protocol: "HL7",
      simulatorUrl: SIMULATOR_URL,
      template: "mindray_bc5380",
      destination: MLLP_DESTINATION,
    },
    expectedResults: [
      { result: "7.5", testName: "WBC" },
      { result: "4.82", testName: "RBC" },
      { result: "14.2", testName: "HGB" },
      { result: "42", testName: "HCT" },
    ],
  },
  {
    name: "Mindray BS-200",
    displayName: "Mindray BS-200 (HL7 Chemistry)",
    analyzerType: "CHEMISTRY",
    pluginType: "Generic HL7",
    protocol: "HL7",
    preSeeded: true,
    push: {
      protocol: "HL7",
      simulatorUrl: SIMULATOR_URL,
      template: "mindray_bs200",
      destination: MLLP_DESTINATION,
    },
    expectedResults: [
      { result: "1.1", testName: "CREA" },
      { result: "32", testName: "ALT" },
      { result: "28", testName: "AST" },
      { result: "92", testName: "GLU" },
    ],
  },
  {
    name: "Mindray BS-300",
    displayName: "Mindray BS-300 (HL7 Chemistry)",
    analyzerType: "CHEMISTRY",
    pluginType: "Generic HL7",
    protocol: "HL7",
    preSeeded: true,
    push: {
      protocol: "HL7",
      simulatorUrl: SIMULATOR_URL,
      template: "mindray_bs300",
      destination: MLLP_DESTINATION,
    },
    expectedResults: [
      { result: "0.8", testName: "CREA" },
      { result: "19", testName: "ALT" },
      { result: "24", testName: "AST" },
      { result: "88", testName: "GLU" },
    ],
  },
  // FILE analyzers — uncomment when harness bind-mount + fixtures are ready:
  // {
  //   name: "QuantStudio 7",
  //   displayName: "QuantStudio 7 (FILE/Excel)",
  //   analyzerType: "MOLECULAR",
  //   pluginType: "Generic File",
  //   protocol: "FILE",
  //   preSeeded: true,
  //   push: {
  //     protocol: "FILE",
  //     fixtureFile: "quantstudio7-results.xls",
  //     importDir: "/data/analyzer-imports/quantstudio-7/incoming",
  //     filePrefix: "qs7-e2e-",
  //   },
  //   expectedResults: [
  //     { sampleId: "HARN-QS7-2026-00001", result: "1520.5" },
  //   ],
  // },
];

// ── Unified Test Flow ────────────────────────────────────────────

async function verifyResults(
  page: import("@playwright/test").Page,
  config: AnalyzerTestConfig,
  sampleId: string,
  presentation: import("../helpers/demo-presentation").DemoPresentation,
) {
  await openAnalyzerResultsAndWaitForText(page, config.name, sampleId, {
    timeoutMs: RESULTS_TIMEOUT,
    perAttemptTimeoutMs: LONG_TIMEOUT,
  });

  const resultsRegion = page.locator(".orderLegendBody, table").first();
  await expect(resultsRegion).toBeVisible({ timeout: UI_TIMEOUT });

  // Verify accession number
  await expect(
    resultsRegion.getByText(accessionTextRegExp(sampleId)).first(),
  ).toBeVisible({ timeout: UI_TIMEOUT });

  // Verify each expected result value
  for (const expected of config.expectedResults) {
    const inputResult = resultsRegion
      .locator(`input[value*="${expected.result}"]`)
      .first();
    let inputVisible = false;
    try {
      await expect(inputResult).toBeVisible({ timeout: SHORT_TIMEOUT });
      inputVisible = true;
    } catch {
      // Input not found — try text match
    }
    if (!inputVisible) {
      await expect(
        resultsRegion.getByText(expected.result, { exact: false }).first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    }
  }

  await presentation.pause(2_000);
}

// ── Test Suite ───────────────────────────────────────────────────

test.describe("Madagascar analyzer demo flows", () => {
  test.setTimeout(240_000);

  for (const config of CONFIGS) {
    test(`${config.displayName}: full E2E flow`, async ({ page }, testInfo) => {
      const presentation = createDemoPresentation(page, testInfo);

      // Step 1: Title
      await presentation.title(
        config.displayName,
        `${config.protocol} → Bridge → OpenELIS → Review → Accept`,
      );

      // Step 2: Find analyzer on dashboard
      await presentation.step(1, `Find ${config.name} on the dashboard`);
      await goToAnalyzerDashboard(page, testInfo);
      const analyzerRow = await findAnalyzerRow(page, config.name, testInfo);

      // Step 3: Test connection (skip for FILE — no TCP)
      if (config.protocol !== "FILE") {
        await presentation.step(2, "Test analyzer connection");
        await testAnalyzerConnection(page, analyzerRow, presentation);
      }

      // Step numbering: ASTM gets test-connection (step 2), others skip it
      const hasTestConnection = config.protocol === "ASTM";
      let step = hasTestConnection ? 3 : 2;

      // Push result — mock generates unique sample_id per push
      await presentation.step(
        step,
        `Send ${config.protocol} result → Bridge → OpenELIS`,
      );
      const sampleId = await pushAnalyzerResult(
        page,
        config.push,
        presentation,
      );
      expect(sampleId).toBeTruthy();

      // Verify results using the dynamic sample_id from the push
      step++;
      await presentation.step(step, "Review staged results");
      await verifyResults(page, config, sampleId!, presentation);

      // Accept results
      await acceptAndVerifyResults(page, presentation, step, sampleId!);

      // Done
      await presentation.title(
        "Flow Complete",
        `${config.displayName}: ${config.expectedResults.length} results accepted.`,
      );
    });
  }
});
