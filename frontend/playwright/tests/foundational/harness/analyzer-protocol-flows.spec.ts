/**
 * M1 priority Analyzer Profile transport flows
 *
 * Each test exercises the M1 profile publication gate:
 *   1. Create analyzer from profile via dashboard UI
 *   2. Test connection (TCP analyzers only)
 *   3. Push a result via mock server (ASTM, HL7, or FILE)
 *   4. Verify results appear on the AnalyzerResults page
 *   5. Delete analyzer (teardown)
 *
 * The mock server is the single source of truth for all analyzer interactions.
 * It owns the fixture files, delivers results, and returns metadata.
 * Tests never hardcode expected values — they come from the mock response.
 * Clinical posting is intentionally outside this gate: the M4 story creates a
 * real order through the UI before accepting and verifying a patient result.
 */

import { expect, test } from "../../../helpers/test-base";
import { findAnalyzerRow } from "../../../helpers/analyzer-dashboard";
import {
  createAnalyzerFromProfile,
  teardownAnalyzer,
} from "../../../helpers/create-analyzer-from-profile";
import { testAnalyzerConnection } from "../../../helpers/test-analyzer-connection";
import { pushAnalyzerResult } from "../../../helpers/push-analyzer-result";
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
import { createRunScopedAnalyzerConfig } from "../../../helpers/analyzer-test-config";
import { resolveMockSimulatorUrl } from "../../../helpers/analyzer-test-config";

const SIMULATOR_URL = resolveMockSimulatorUrl();
const RESULTS_TIMEOUT = 90_000;

const CONFIGS: AnalyzerTestConfig[] = [
  {
    name: "Demo: GeneXpert ASTM",
    displayName: "GeneXpert ASTM",
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
    name: "Demo: QuantStudio 7",
    displayName: "QuantStudio 7 (FILE/Excel)",
    profileName: "Thermo Fisher QuantStudio QS5/QS7",
    protocol: "FILE",
    push: {
      protocol: "FILE",
      simulatorUrl: SIMULATOR_URL,
      template: "quantstudio7",
      targetDir: "/data/analyzer-imports/demo--quantstudio-7/incoming",
    },
  },
  {
    name: "Demo: FluoroCycler XT",
    displayName: "FluoroCycler XT (FILE/Excel)",
    profileName: "Bruker FluoroCycler XT",
    protocol: "FILE",
    push: {
      protocol: "FILE",
      simulatorUrl: SIMULATOR_URL,
      template: "hain_fluorocycler",
      targetDir: "/data/analyzer-imports/demo--fluorocycler-xt/incoming",
    },
  },
];

// ── Unified Test Flow ────────────────────────────────────────────

async function verifyResults(
  page: import("@playwright/test").Page,
  config: AnalyzerTestConfig,
  pushResults: PushResult[],
  primarySampleId: string,
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
}

// ── Test Suite ───────────────────────────────────────────────────

test.describe("M1 priority profile transport integrations", () => {
  test.setTimeout(240_000);

  for (const baseConfig of CONFIGS) {
    test(`${baseConfig.displayName}: transport integration flow`, async ({
      page,
    }, testInfo) => {
      const runId = `${Date.now().toString(36)}-${testInfo.retry}`;
      const config = createRunScopedAnalyzerConfig(baseConfig, runId);

      // Step 1: Create analyzer from profile via dashboard UI
      const dynamicIp = await createAnalyzerFromProfile(page, config);
      const analyzerRow = await findAnalyzerRow(page, config.name, testInfo);

      // Step 2: Test connection (skip for FILE — no TCP)
      if (config.protocol !== "FILE") {
        await testAnalyzerConnection(page, analyzerRow);
      }

      // Override push destination with dynamic bridge IP for TCP analyzers
      let pushConfig = { ...config.push };
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
      const pushResults = await pushAnalyzerResult(pushConfig);

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
      await verifyResults(page, config, pushResults, primarySampleId);

      // Teardown: delete analyzer + remove mock network
      await teardownAnalyzer(page, config);
    });
  }
});
