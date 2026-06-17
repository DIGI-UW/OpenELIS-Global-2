/**
 * Unified Madagascar Analyzer Demo Flows
 *
 * Each test exercises the full E2E lifecycle:
 *   1. Create analyzer from profile via dashboard UI
 *   2. Test connection (TCP analyzers only)
 *   3. Push a result via mock server (ASTM, HL7, or FILE)
 *   4. Verify results appear on the AnalyzerResults page
 *   5. Accept results and verify on AccessionResults page
 *   6. Delete analyzer (teardown)
 *
 * The mock server is the single source of truth for all analyzer interactions.
 * It owns the fixture files, delivers results, and returns metadata.
 * Tests never hardcode expected values — they come from the mock response.
 */

import { expect, test } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { findAnalyzerRow } from "../../../helpers/analyzer-dashboard";
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
import type {
  AnalyzerTestConfig,
  PushResult,
} from "../../../helpers/analyzer-test-config";

const SIMULATOR_URL = "http://localhost:8085";
const RESULTS_TIMEOUT = 90_000;

// ── Analyzer Configurations ──────────────────────────────────────
//
// Every config creates from scratch via UI and tears down after.
// Names use "Demo:" prefix to coexist with pre-seeded analyzers.
//
// No hardcoded expectedResults — the mock server returns them.

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
      destination: "tcp://placeholder:12001",
    },
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
  },
  // ── FILE Analyzers ─────────────────────────────────────────────
  {
    name: "Demo: QuantStudio 7",
    displayName: "QuantStudio 7 (FILE/Excel)",
    analyzerType: "MOLECULAR",
    pluginType: "Generic File",
    profileName: "QuantStudio QS5/QS7",
    protocol: "FILE",
    push: {
      protocol: "FILE",
      simulatorUrl: SIMULATOR_URL,
      template: "quantstudio7",
      targetDir: "/data/analyzer-imports/demo--quantstudio-7/incoming",
    },
  },
  {
    name: "Demo: QuantStudio 5",
    displayName: "QuantStudio 5 (FILE/Excel)",
    analyzerType: "MOLECULAR",
    pluginType: "Generic File",
    profileName: "QuantStudio QS5/QS7",
    protocol: "FILE",
    push: {
      protocol: "FILE",
      simulatorUrl: SIMULATOR_URL,
      template: "quantstudio5",
      targetDir: "/data/analyzer-imports/demo--quantstudio-5/incoming",
    },
  },
  {
    name: "Demo: FluoroCycler XT",
    displayName: "FluoroCycler XT (FILE/Excel)",
    analyzerType: "MOLECULAR",
    pluginType: "Generic File",
    profileName: "Bruker FluoroCycler XT",
    protocol: "FILE",
    push: {
      protocol: "FILE",
      simulatorUrl: SIMULATOR_URL,
      template: "hain_fluorocycler",
      // FluoroCycler real files carry no per-row test-code column — in
      // production the lab tech declares VIH-1 in the bridge upload UI.
      // Mirror that here instead of hacking a TestCode column into the
      // fixture.
      uploadViaBridge: true,
      analyzerName: "Demo: FluoroCycler XT",
      testCode: "VIH-1",
    },
  },
  // ── Madagascar Sprint: 3 New FILE Analyzers ────────────────────
  {
    name: "Demo: Wondfo Finecare FS-205",
    displayName: "Wondfo Finecare FS-205 (FILE/CSV — POCT)",
    analyzerType: "IMMUNOLOGY",
    pluginType: "Generic File",
    profileName: "Wondfo Finecare FS-205 (CSV)",
    protocol: "FILE",
    push: {
      protocol: "FILE",
      simulatorUrl: SIMULATOR_URL,
      template: "wondfo_finecare",
      targetDir: "/data/analyzer-imports/demo--wondfo-finecare-fs-205/incoming",
    },
  },
  {
    name: "Demo: Tecan Infinite F50",
    displayName: "Tecan Infinite F50 (FILE/CSV — ELISA)",
    analyzerType: "IMMUNOLOGY",
    pluginType: "Generic File",
    profileName: "Tecan Infinite F50",
    protocol: "FILE",
    push: {
      protocol: "FILE",
      simulatorUrl: SIMULATOR_URL,
      template: "tecan_f50",
      targetDir: "/data/analyzer-imports/demo--tecan-infinite-f50/incoming",
    },
  },
  {
    name: "Demo: Thermo Multiskan FC",
    displayName: "Thermo Multiskan FC (FILE/CSV — ELISA)",
    analyzerType: "IMMUNOLOGY",
    pluginType: "Generic File",
    profileName: "Thermo Multiskan FC",
    protocol: "FILE",
    push: {
      protocol: "FILE",
      simulatorUrl: SIMULATOR_URL,
      template: "multiskan_fc",
      targetDir: "/data/analyzer-imports/demo--thermo-multiskan-fc/incoming",
    },
  },
];

// ── Unified Test Flow ────────────────────────────────────────────

async function verifyResults(
  page: import("@playwright/test").Page,
  config: AnalyzerTestConfig,
  pushResults: PushResult[],
  primarySampleId: string,
  presentation: import("../../../helpers/demo-presentation").DemoPresentation,
) {
  const allAccessions = pushResults
    .map((r) => r.sampleId || primarySampleId)
    .filter((v, i, a) => a.indexOf(v) === i);

  await openAnalyzerResultsAndWaitForText(page, config.name, primarySampleId, {
    timeoutMs: RESULTS_TIMEOUT,
    perAttemptTimeoutMs: LONG_TIMEOUT,
    allExpectedAccessions: allAccessions,
  });

  const resultsRegion = page.locator(".orderLegendBody, table").first();
  await expect(resultsRegion).toBeVisible({ timeout: UI_TIMEOUT });

  for (const expected of pushResults) {
    const expectedSampleId = expected.sampleId || primarySampleId;
    await expect(
      resultsRegion.getByText(accessionTextRegExp(expectedSampleId)).first(),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    if (expected.result) {
      await expectResultVisible(resultsRegion, expected.result);
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

      await presentation.title(
        config.displayName,
        `${config.protocol} → Bridge → OpenELIS → Review → Accept`,
      );

      // Step 1: Create analyzer from profile via dashboard UI
      await presentation.step(
        1,
        `Create ${config.name} from profile via dashboard`,
      );
      const dynamicIp = await createAnalyzerFromProfile(
        page,
        config,
        presentation,
      );
      await findAnalyzerRow(page, config.name, testInfo);

      // Step 2: Test connection (skip for FILE — no TCP)
      if (config.protocol !== "FILE") {
        await presentation.step(2, "Test analyzer connection");
        const analyzerRow = await findAnalyzerRow(page, config.name, testInfo);
        await testAnalyzerConnection(page, analyzerRow, presentation);
      }

      const hasTestConnection = config.protocol !== "FILE";
      let step = hasTestConnection ? 3 : 2;

      // Override push destination with dynamic bridge IP for TCP analyzers
      let pushConfig = config.push;
      if (dynamicIp && config.protocol !== "FILE") {
        const bridgeIp = dynamicIp.replace(/\.\d+$/, ".2");
        const port = config.protocol === "ASTM" ? 12001 : 2575;
        const scheme = config.protocol === "ASTM" ? "tcp" : "mllp";
        pushConfig = {
          ...pushConfig,
          destination: `${scheme}://${bridgeIp}:${port}`,
          // Address /simulate by the provisioned instance so the push sources
          // from the analyzer's own IP (the bridge identifies it by source IP).
          mockAnalyzerName: config.mockAnalyzerName,
        };
      }

      // Step 3: Push result via mock server
      await presentation.step(
        step,
        `Send ${config.protocol} result → Bridge → OpenELIS`,
      );
      const pushResults = await pushAnalyzerResult(
        page,
        pushConfig,
        presentation,
      );

      expect(
        pushResults.length,
        `Mock should return at least 1 result for ${config.name}`,
      ).toBeGreaterThan(0);

      const primarySampleId = pushResults[0].sampleId;
      expect(
        primarySampleId,
        `Mock should return a sampleId for ${config.name}`,
      ).toBeTruthy();

      // Step 4: Wait for results from bridge
      step++;
      await presentation.step(
        step,
        "Waiting for results from analyzer bridge...",
      );
      await verifyResults(
        page,
        config,
        pushResults,
        primarySampleId,
        presentation,
      );

      await presentation.step(step, "Results staged — ready to accept");
      await presentation.pause(3_000);

      // Step 5: Accept results
      await acceptAndVerifyResults(page, presentation, step, primarySampleId);

      await presentation.title(
        "Flow Complete",
        `${config.displayName}: ${pushResults.length} results accepted.`,
      );

      // Teardown: delete analyzer + remove mock network
      await teardownAnalyzer(page, config);
    });
  }
});
