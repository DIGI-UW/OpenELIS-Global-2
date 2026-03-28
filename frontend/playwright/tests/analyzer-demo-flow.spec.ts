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
import * as path from "path";
import { createDemoPresentation } from "../helpers/demo-presentation";
import {
  findAnalyzerRow,
  goToAnalyzerDashboard,
} from "../helpers/analyzer-dashboard";
import {
  createAnalyzerFromProfile,
  teardownAnalyzer,
} from "../helpers/create-analyzer-from-profile";
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
const RESULTS_TIMEOUT = 90_000;

// Per-analyzer bridge IPs on stable mock networks (see analyzer_network_manager.py FIXED_SUBNETS).
// The mock pushes FROM its per-analyzer IP so the bridge can identify the source.
// Bridge gets .2 on each subnet; mock gets .10.
const BRIDGE_IP = {
  genexpert: "10.42.20.2",
  bc5380: "10.42.21.2",
  bs200: "10.42.22.2",
  bs300: "10.42.23.2",
};

const REPO_ROOT = path.resolve(__dirname, "../../..");
const HOST_IMPORTS_BASE = path.join(
  REPO_ROOT,
  "projects/analyzer-harness/volume/analyzer-imports",
);

// ── Analyzer Configurations ──────────────────────────────────────

const CONFIGS: AnalyzerTestConfig[] = [
  {
    name: "Cepheid GeneXpert (ASTM Mode)",
    displayName: "GeneXpert ASTM",
    analyzerType: "MOLECULAR",
    pluginType: "Generic ASTM",
    profileName: "Cepheid GeneXpert (ASTM Mode)",
    protocol: "ASTM",
    preSeeded: false,
    mockAnalyzerName: "demo-genexpert",
    port: 9600,
    push: {
      protocol: "ASTM",
      simulatorUrl: SIMULATOR_URL,
      template: "genexpert_astm",
      destination: `tcp://${BRIDGE_IP.genexpert}:12001`,
    },
    expectedResults: [{ result: "NEGATIVE" }],
  },
  {
    name: "Mindray BC-5380",
    displayName: "Mindray BC-5380 (HL7 Hematology)",
    analyzerType: "HEMATOLOGY",
    pluginType: "Generic HL7",
    profileName: "Mindray BC-5380",
    protocol: "HL7",
    preSeeded: false,
    mockAnalyzerName: "demo-bc5380",
    port: 5380,
    push: {
      protocol: "HL7",
      simulatorUrl: SIMULATOR_URL,
      template: "mindray_bc5380",
      destination: `mllp://${BRIDGE_IP.bc5380}:2575`,
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
    profileName: "Mindray BS-200",
    protocol: "HL7",
    preSeeded: false,
    mockAnalyzerName: "demo-bs200",
    port: 6001,
    push: {
      protocol: "HL7",
      simulatorUrl: SIMULATOR_URL,
      template: "mindray_bs200",
      destination: `mllp://${BRIDGE_IP.bs200}:2575`,
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
    profileName: "Mindray BS-300",
    protocol: "HL7",
    preSeeded: false,
    mockAnalyzerName: "demo-bs300",
    port: 6002,
    push: {
      protocol: "HL7",
      simulatorUrl: SIMULATOR_URL,
      template: "mindray_bs300",
      destination: `mllp://${BRIDGE_IP.bs300}:2575`,
    },
    expectedResults: [
      { result: "0.8", testName: "CREA" },
      { result: "19", testName: "ALT" },
      { result: "24", testName: "AST" },
      { result: "88", testName: "GLU" },
    ],
  },
  // ── FILE Analyzers ─────────────────────────────────────────────
  {
    name: "QuantStudio 7",
    displayName: "QuantStudio 7 (FILE/Excel)",
    analyzerType: "MOLECULAR",
    pluginType: "Generic File",
    profileName: "QuantStudio QS5/QS7",
    protocol: "FILE",
    preSeeded: false,
    fileSampleId: "HARN-QS7-2026-00001",
    push: {
      protocol: "FILE",
      fixtureFile: "quantstudio-e2e-results.xlsx",
      importDir: path.join(HOST_IMPORTS_BASE, "quantstudio-7/incoming"),
      filePrefix: "qs7-e2e-",
    },
    expectedResults: [{ result: "1520.5" }],
  },
  {
    name: "QuantStudio 5",
    displayName: "QuantStudio 5 (FILE/Excel)",
    analyzerType: "MOLECULAR",
    pluginType: "Generic File",
    profileName: "QuantStudio QS5/QS7",
    protocol: "FILE",
    preSeeded: false,
    fileSampleId: "HARN-QS5-2026-00001",
    push: {
      protocol: "FILE",
      fixtureFile: "quantstudio-e2e-results-qs5.xls",
      importDir: path.join(HOST_IMPORTS_BASE, "quantstudio-5/incoming"),
      filePrefix: "qs5-e2e-",
    },
    expectedResults: [{ result: "1520.5" }],
  },
  {
    name: "FluoroCycler XT",
    displayName: "FluoroCycler XT (FILE/Excel)",
    analyzerType: "MOLECULAR",
    pluginType: "Generic File",
    profileName: "Bruker FluoroCycler XT",
    protocol: "FILE",
    preSeeded: false,
    fileSampleId: "HARN-FC-2026-00001",
    push: {
      protocol: "FILE",
      fixtureFile: "fluorocycler-e2e-results.xlsx",
      importDir: path.join(HOST_IMPORTS_BASE, "fluorocycler-xt/incoming"),
      filePrefix: "fc-e2e-",
    },
    expectedResults: [{ result: "28.5" }],
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

      // Step 2: Create or find analyzer on dashboard
      let analyzerRow;
      let dynamicIp: string | null = null;
      if (config.preSeeded) {
        await presentation.step(1, `Find ${config.name} on the dashboard`);
        await goToAnalyzerDashboard(page, testInfo);
        analyzerRow = await findAnalyzerRow(page, config.name, testInfo);
      } else {
        await presentation.step(
          1,
          `Create ${config.name} from profile via dashboard`,
        );
        dynamicIp = await createAnalyzerFromProfile(page, config, presentation);
        analyzerRow = await findAnalyzerRow(page, config.name, testInfo);
      }

      // Step 3: Test connection (skip for FILE — no TCP)
      if (config.protocol !== "FILE") {
        await presentation.step(2, "Test analyzer connection");
        await testAnalyzerConnection(page, analyzerRow, presentation);
      }

      // Step numbering: TCP analyzers get test-connection (step 2), FILE skips it
      const hasTestConnection = config.protocol !== "FILE";
      let step = hasTestConnection ? 3 : 2;

      // For dynamically created TCP analyzers, push via the per-analyzer network.
      // Each analyzer gets a unique IP — the bridge identifies source by IP,
      // simulating production where each physical instrument has its own address.
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

      // Push result — mock generates unique sample_id per push
      await presentation.step(
        step,
        `Send ${config.protocol} result → Bridge → OpenELIS`,
      );
      const sampleId = await pushAnalyzerResult(page, pushConfig, presentation);

      // ASTM/HL7 return dynamic sample_id; FILE fixtures use known IDs
      if (config.protocol !== "FILE") {
        expect(sampleId).toBeTruthy();
      }

      // For FILE, use the known accession from the fixture
      const verifyId = sampleId || config.fileSampleId || config.name;

      // Verify results using the sample_id from the push (or known fixture ID)
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

      // Teardown: clean up created analyzer + mock network (self-contained test)
      if (!config.preSeeded) {
        await teardownAnalyzer(page, config);
      }
    });
  }
});
