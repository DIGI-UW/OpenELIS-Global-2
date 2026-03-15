import { test, expect } from "@playwright/test";
import { showTitleCard, showStepCard } from "../helpers/title-card";
import { videoPause } from "../helpers/video-pause";
import { acceptAndVerifyResults } from "../helpers/accept-results";

/**
 * GeneXpert ASTM Push → Results E2E Test
 *
 * Demonstrates the ASTM push-mode workflow for GeneXpert:
 *   1. Verify the fixture-loaded GeneXpert analyzer (ID 2013) exists
 *   2. Trigger the mock simulator to push an ASTM message via the bridge
 *   3. Navigate to Analyzer Results and verify imported data appears
 *
 * Unlike file-import tests, ASTM push uses network (TCP) delivery through the
 * analyzer bridge — no file watchers or import directories involved.
 *
 * Requires: analyzer harness stack (bridge + simulator + OE)
 *   CLEANUP=false TEST_USER=admin TEST_PASS='adminADMIN!' \
 *     npx playwright test astm-genexpert-results --project=demo-video
 */

const GENEXPERT_ANALYZER_NAME = "Cepheid GeneXpert (ASTM Mode)";
const SIMULATOR_URL = "http://localhost:8085";
const BRIDGE_DESTINATION = "tcp://openelis-analyzer-bridge:12001";

const EXPECTED_RESULTS = [
  { sampleId: "SPECIMEN-GX-001", testCode: "MTB-RIF", result: "NEGATIVE" },
  { sampleId: "SPECIMEN-GX-001", testCode: "RIF", result: "Sensitive" },
  { sampleId: "SPECIMEN-GX-001", testCode: "HIV-VL", result: "1250" },
  { sampleId: "SPECIMEN-GX-001", testCode: "COVID19", result: "NEGATIVE" },
];

test.describe("GeneXpert ASTM Push → Results", () => {
  test.setTimeout(360_000); // 6 min — slowMo=500ms adds up with step cards + accept + validate

  test("full flow: simulate ASTM push → verify results", async ({
    page,
  }, testInfo) => {
    // Capture errors for debugging
    const consoleErrors: string[] = [];
    page.on("console", (msg) => {
      if (msg.type() === "error") consoleErrors.push(msg.text());
    });

    // ── Title Card ─────────────────────────────────────────────────
    await showTitleCard(
      page,
      "GeneXpert — ASTM Push Mode",
      "Simulate → Bridge → Results",
      3000,
      testInfo,
    );

    // ── Step 1: Navigate to analyzer dashboard ─────────────────────
    await showStepCard(
      page,
      1,
      "Navigate to Analyzer Dashboard",
      2000,
      testInfo,
    );

    const analyzerApiPromise = page.waitForResponse(
      (resp) =>
        resp.url().includes("/rest/analyzer/analyzers") &&
        resp.status() === 200,
      { timeout: 30_000 },
    );
    await page.goto("analyzers", { waitUntil: "domcontentloaded" });
    await analyzerApiPromise;

    await expect(page.locator('[data-testid="analyzers-list"]')).toBeVisible({
      timeout: 30_000,
    });

    // Verify GeneXpert exists in the list (loaded by fixture SQL)
    const analyzerRow = page.locator("tbody tr", {
      hasText: new RegExp(
        GENEXPERT_ANALYZER_NAME.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"),
        "i",
      ),
    });
    await expect(analyzerRow.first()).toBeVisible({ timeout: 10_000 });
    console.log(`Found analyzer: ${GENEXPERT_ANALYZER_NAME}`);
    await videoPause(page, 1_500, testInfo);

    // ── Step 2: Trigger ASTM push via simulator ────────────────────
    await showStepCard(
      page,
      2,
      "Send ASTM Message via Simulator",
      2000,
      testInfo,
    );

    const simulatorRes = await page.request.post(
      `${SIMULATOR_URL}/simulate/astm/genexpert_astm`,
      {
        data: { destination: BRIDGE_DESTINATION, count: 1 },
      },
    );
    expect(simulatorRes.ok()).toBeTruthy();

    const simBody = await simulatorRes.json();
    console.log(
      `Simulator response: pushed=${simBody.pushed}, status=${simBody.status}`,
    );
    await videoPause(page, 2_000, testInfo);

    // ── Step 3: Wait for results to arrive ─────────────────────────
    await showStepCard(
      page,
      3,
      "Waiting for ASTM Results (near-instant via bridge)...",
      2000,
      testInfo,
    );

    // Poll until results appear in the staging table. Under concurrent load
    // (e.g. parallel file-import tests), OE may take longer to process the
    // bridge's HTTP forward. Polling is both faster (no wasted wait when
    // results arrive quickly) and more robust (no failure under load).
    let resultCount = 0;
    for (let attempt = 1; attempt <= 12; attempt++) {
      const resp = await page.request.get(
        `/api/OpenELIS-Global/rest/AnalyzerResults?type=${encodeURIComponent(GENEXPERT_ANALYZER_NAME)}`,
      );
      const data = await resp.json().catch(() => null);
      resultCount = data?.resultList?.length ?? 0;
      if (resultCount > 0) break;
      await page.waitForTimeout(5_000);
    }
    console.log(`Results available after polling: ${resultCount}`);
    await videoPause(page, 1_000, testInfo);

    // ── Step 4: View imported results ──────────────────────────────
    await showStepCard(page, 4, "View Imported Results", 2000, testInfo);

    const apiResponsePromise = page
      .waitForResponse((resp) => resp.url().includes("/rest/AnalyzerResults"), {
        timeout: 30_000,
      })
      .catch(() => null);

    await page.goto(
      `AnalyzerResults?type=${encodeURIComponent(GENEXPERT_ANALYZER_NAME)}`,
      { waitUntil: "domcontentloaded" },
    );

    const apiResponse = await apiResponsePromise;
    if (apiResponse) {
      const body = await apiResponse.text();
      console.log(
        `API Response: status=${apiResponse.status()}, length=${body.length}`,
      );
      try {
        const json = JSON.parse(body);
        console.log(`resultList length: ${json.resultList?.length ?? "N/A"}`);
      } catch {
        // Non-JSON response
      }
    }

    // Wait for results page to fully render
    await videoPause(page, 3_000, testInfo);

    // ── Step 5: Verify actual result values ────────────────────────
    await showStepCard(page, 5, "Verify Result Values", 2000, testInfo);

    const resultsTable = page.locator("table, .orderLegendBody");
    await expect(resultsTable.first()).toBeVisible({ timeout: 15_000 });

    // Hard assertion: verify EACH expected result value appears in the table.
    // Sample IDs render in <div data-testid="LabNo"> (plain text).
    // Result values render in <input> textboxes (editable) or plain text
    // (read-only). Use input[value] selector for editable results.
    for (const expected of EXPECTED_RESULTS) {
      await expect(
        page
          .locator('[data-testid="LabNo"]', { hasText: expected.sampleId })
          .first(),
      ).toBeVisible({ timeout: 15_000 });

      // Match result value in input fields (editable) — handles "1250" matching "1250.00"
      const resultPattern = expected.result.replace(
        /[.*+?^${}()|[\]\\]/g,
        "\\$&",
      );
      await expect(
        page.locator(`input[value*="${expected.result}"]`).first(),
      ).toBeVisible({ timeout: 5_000 });

      console.log(
        `  ✓ ${expected.sampleId} → ${expected.testCode}: ${expected.result}`,
      );
    }

    console.log(
      `All ${EXPECTED_RESULTS.length} result values verified for GeneXpert ASTM!`,
    );

    // ── Linger on staging results for the video ──────────────────
    await videoPause(page, 2_000, testInfo);

    // ── Steps 6-7: Accept results and verify in standard Results view
    await acceptAndVerifyResults(page, testInfo, 5);

    // ── Completion Card ───────────────────────────────────────────
    await showTitleCard(
      page,
      "Import Complete",
      `GeneXpert ASTM: ${EXPECTED_RESULTS.length} results accepted & validated`,
      3000,
      testInfo,
    );
  });
});
