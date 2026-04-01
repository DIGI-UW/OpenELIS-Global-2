/**
 * Unified Madagascar Analyzer Demo Flows
 *
 * Each test exercises the full E2E lifecycle:
 *   1. Create analyzer from profile via dashboard UI
 *   2. Test connection (TCP analyzers only)
 *   3. Push a result (ASTM, HL7 MLLP, or file drop)
 *   4. Verify results appear on the AnalyzerResults page
 *   5. Accept results and verify on AccessionResults page
 *   6. Delete analyzer (teardown)
 *
 * Tests use "Demo:" prefixed names to coexist with pre-seeded analyzers.
 * Same test runs in both harness-demo (fast) and harness-demo-video (slowMo+video).
 */

import { expect, test } from "@playwright/test";
import * as path from "path";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import {
  findAnalyzerRow,
  goToAnalyzerDashboard,
} from "../../../helpers/analyzer-dashboard";
import {
  createAnalyzerFromProfile,
  teardownAnalyzer,
} from "../../../helpers/create-analyzer-from-profile";
import { testAnalyzerConnection } from "../../../helpers/test-analyzer-connection";
import { pushAnalyzerResult } from "../../../helpers/push-analyzer-result";
import { acceptAndVerifyResults } from "../../../helpers/accept-results";
import {
  accessionTextRegExp,
  expectResultVisible,
  openAnalyzerResultsAndWaitForText,
} from "../../../helpers/results-ui";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { resolveHarnessImportsDir } from "../../../helpers/workspace-paths";
import type { AnalyzerTestConfig } from "../../../helpers/analyzer-test-config";

const SIMULATOR_URL = "http://localhost:8085";
const RESULTS_TIMEOUT = 90_000;

const HOST_IMPORTS_BASE = resolveHarnessImportsDir(__dirname);

// ── Analyzer Configurations ──────────────────────────────────────
//
// Every config creates from scratch via UI and tears down after.
// Names use "Demo:" prefix to coexist with pre-seeded analyzers.

const CONFIGS: AnalyzerTestConfig[] = [
  {
    name: "Demo: GeneXpert ASTM",
    displayName: "GeneXpert ASTM",
    analyzerType: "MOLECULAR",
    pluginType: "Generic ASTM",
    profileName: "Cepheid GeneXpert (ASTM Mode)",
    protocol: "ASTM",
    mockAnalyzerName: "demo-genexpert",
    port: 9600,
    push: {
      protocol: "ASTM",
      simulatorUrl: SIMULATOR_URL,
      template: "genexpert_astm",
      destination: "tcp://placeholder:12001", // overridden by dynamic IP
    },
    expectedResults: [{ result: "NEGATIVE" }],
  },
  {
    name: "Demo: Mindray BC-5380",
    displayName: "Mindray BC-5380 (HL7 Hematology)",
    analyzerType: "HEMATOLOGY",
    pluginType: "Generic HL7",
    profileName: "Mindray BC-5380",
    protocol: "HL7",
    mockAnalyzerName: "demo-bc5380",
    port: 5380,
    push: {
      protocol: "HL7",
      simulatorUrl: SIMULATOR_URL,
      template: "mindray_bc5380",
      destination: "mllp://placeholder:2575",
    },
    expectedResults: [
      { result: "7.5", testName: "White Blood Cells" },
      { result: "4.82", testName: "Red Blood Cells" },
      { result: "14.2", testName: "Hemoglobin" },
      { result: "42", testName: "Hematocrit" },
    ],
  },
  {
    name: "Demo: Mindray BS-200",
    displayName: "Mindray BS-200 (HL7 Chemistry)",
    analyzerType: "CHEMISTRY",
    pluginType: "Generic HL7",
    profileName: "Mindray BS-200",
    protocol: "HL7",
    mockAnalyzerName: "demo-bs200",
    port: 6001,
    push: {
      protocol: "HL7",
      simulatorUrl: SIMULATOR_URL,
      template: "mindray_bs200",
      destination: "mllp://placeholder:2575",
    },
    expectedResults: [
      { result: "1.1", testName: "Creatinine" },
      { result: "32", testName: "ALT" },
      { result: "28", testName: "AST" },
      { result: "92", testName: "Glucose" },
    ],
  },
  {
    name: "Demo: Mindray BS-300",
    displayName: "Mindray BS-300 (HL7 Chemistry)",
    analyzerType: "CHEMISTRY",
    pluginType: "Generic HL7",
    profileName: "Mindray BS-300",
    protocol: "HL7",
    mockAnalyzerName: "demo-bs300",
    port: 6002,
    push: {
      protocol: "HL7",
      simulatorUrl: SIMULATOR_URL,
      template: "mindray_bs300",
      destination: "mllp://placeholder:2575",
    },
    expectedResults: [
      { result: "0.8", testName: "Creatinine" },
      { result: "19", testName: "ALT" },
      { result: "24", testName: "AST" },
      { result: "88", testName: "Glucose" },
    ],
  },
  // ── FILE Analyzers ─────────────────────────────────────────────
  {
    name: "Demo: QuantStudio 7",
    displayName: "QuantStudio 7 (FILE/Excel)",
    analyzerType: "MOLECULAR",
    pluginType: "Generic File",
    profileName: "QuantStudio QS5/QS7",
    protocol: "FILE",
    fileSampleId: "HARN-QS7-2026-00001",
    push: {
      protocol: "FILE",
      fixtureFile: "quantstudio-e2e-results.xlsx",
      importDir: path.join(HOST_IMPORTS_BASE, "demo--quantstudio-7/incoming"),
      filePrefix: "qs7-e2e-",
    },
    expectedResults: [
      { sampleId: "HARN-QS7-2026-00001", result: "1520.5" },
      { sampleId: "HARN-QS7-2026-00002", result: "45200" },
      { sampleId: "HARN-QS7-2026-00005", result: "3200.8" },
    ],
  },
  {
    name: "Demo: QuantStudio 5",
    displayName: "QuantStudio 5 (FILE/Excel)",
    analyzerType: "MOLECULAR",
    pluginType: "Generic File",
    profileName: "QuantStudio QS5/QS7",
    protocol: "FILE",
    fileSampleId: "HARN-QS5-2026-00001",
    push: {
      protocol: "FILE",
      fixtureFile: "quantstudio-e2e-results-qs5.xls",
      importDir: path.join(HOST_IMPORTS_BASE, "demo--quantstudio-5/incoming"),
      filePrefix: "qs5-e2e-",
    },
    expectedResults: [
      { sampleId: "HARN-QS5-2026-00001", result: "1520.5" },
      { sampleId: "HARN-QS5-2026-00002", result: "45200" },
      { sampleId: "HARN-QS5-2026-00005", result: "3200.8" },
    ],
  },
  {
    name: "Demo: FluoroCycler XT",
    displayName: "FluoroCycler XT (FILE/Excel)",
    analyzerType: "MOLECULAR",
    pluginType: "Generic File",
    profileName: "Bruker FluoroCycler XT",
    protocol: "FILE",
    fileSampleId: "HARN-FC-2026-00001",
    push: {
      protocol: "FILE",
      fixtureFile: "fluorocycler-e2e-results.xlsx",
      importDir: path.join(HOST_IMPORTS_BASE, "demo--fluorocycler-xt/incoming"),
      filePrefix: "fc-e2e-",
    },
    expectedResults: [
      { sampleId: "HARN-FC-2026-00001", result: "28.5" },
      { sampleId: "HARN-FC-2026-00002", result: "31.2" },
      { sampleId: "HARN-FC-2026-00003", result: "Negative" },
    ],
  },
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

  // Verify each expected result value
  for (const expected of config.expectedResults) {
    if (expected.testName) {
      await expect(
        page.getByText(expected.testName, { exact: false }).first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(
        page.locator(`input[value*="${expected.result}"]`).first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      continue;
    }

    const expectedSampleId = expected.sampleId || sampleId;
    const accessionRow = resultsRegion
      .getByRole("row", { name: accessionTextRegExp(expectedSampleId) })
      .first();
    await expect(accessionRow).toBeVisible({ timeout: UI_TIMEOUT });
    await expectResultVisible(accessionRow, expected.result);
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

      // Step 2: Create analyzer from profile via dashboard UI
      await presentation.step(
        1,
        `Create ${config.name} from profile via dashboard`,
      );
      const dynamicIp = await createAnalyzerFromProfile(
        page,
        config,
        presentation,
      );
      const analyzerRow = await findAnalyzerRow(page, config.name, testInfo);

      // Step 3: Test connection (skip for FILE — no TCP)
      if (config.protocol !== "FILE") {
        await presentation.step(2, "Test analyzer connection");
        await testAnalyzerConnection(page, analyzerRow, presentation);
      }

      const hasTestConnection = config.protocol !== "FILE";
      let step = hasTestConnection ? 3 : 2;

      // For TCP analyzers, override push destination with dynamic bridge IP
      let pushConfig = config.push;
      if (dynamicIp && config.protocol !== "FILE") {
        const bridgeIp = dynamicIp.replace(/\.\d+$/, ".2");
        const port = config.protocol === "ASTM" ? 12001 : 2575;
        const scheme = config.protocol === "ASTM" ? "tcp" : "mllp";
        pushConfig = {
          ...pushConfig,
          destination: `${scheme}://${bridgeIp}:${port}`,
        } as typeof pushConfig;
      }

      // Push result
      await presentation.step(
        step,
        `Send ${config.protocol} result → Bridge → OpenELIS`,
      );
      const sampleId = await pushAnalyzerResult(page, pushConfig, presentation);

      if (config.protocol !== "FILE") {
        expect(sampleId).toBeTruthy();
      }

      const verifyId = sampleId || config.fileSampleId || config.name;

      // Verify results on staging page
      step++;
      await presentation.step(step, "Review staged results");
      await verifyResults(page, config, verifyId, presentation);

      // Accept results
      await acceptAndVerifyResults(page, presentation, step, verifyId);

      // Done
      await presentation.title(
        "Flow Complete",
        `${config.displayName}: ${config.expectedResults.length} results accepted.`,
      );

      // Teardown: delete analyzer + remove mock network
      await teardownAnalyzer(page, config);
    });
  }
});
