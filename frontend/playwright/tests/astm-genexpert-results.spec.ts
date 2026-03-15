import { test, expect } from "@playwright/test";
import { showTitleCard, showStepCard } from "../helpers/title-card";
import { isVideoProject, videoPause } from "../helpers/video-pause";
import { acceptAndVerifyResults } from "../helpers/accept-results";

/**
 * GeneXpert ASTM Push → Results E2E Test
 *
 * Demonstrates the full ASTM push-mode workflow for GeneXpert:
 *   1. Create a new GeneXpert analyzer via the UI (selects ASTM profile)
 *   2. Trigger the mock simulator to push an ASTM message via the bridge
 *   3. Navigate to Analyzer Results and verify imported data appears
 *   4. Accept results and verify in AccessionResults with real test names
 *
 * The analyzer profile's default_test_mappings auto-creates analyzer_test_map
 * entries by looking up tests via LOINC codes against the test catalog.
 *
 * Requires: analyzer harness stack (bridge + simulator + OE)
 *   CLEANUP=false TEST_USER=admin TEST_PASS='adminADMIN!' \
 *     npx playwright test astm-genexpert-results --project=demo-video
 */

const CLEANUP = process.env.CLEANUP !== "false";
const SIMULATOR_URL = "http://localhost:8085";
const BRIDGE_DESTINATION = "tcp://openelis-analyzer-bridge:12001";

const EXPECTED_RESULTS = [
  { sampleId: "SPECIMEN-GX-001", testCode: "MTB-RIF", result: "NEGATIVE" },
  { sampleId: "SPECIMEN-GX-001", testCode: "RIF", result: "Sensitive" },
  { sampleId: "SPECIMEN-GX-001", testCode: "HIV-VL", result: "1250" },
  { sampleId: "SPECIMEN-GX-001", testCode: "COVID19", result: "NEGATIVE" },
];

test.describe("GeneXpert ASTM Push → Results", () => {
  test.setTimeout(420_000); // 7 min — create + ASTM push + poll + accept + AccessionResults

  let createdAnalyzerName: string;

  test("full flow: create analyzer → ASTM push → verify results", async ({
    page,
  }, testInfo) => {
    const runId = Date.now();
    createdAnalyzerName = `Cepheid GeneXpert (ASTM Mode) E2E ${runId}`;

    // Capture errors for debugging
    const consoleErrors: string[] = [];
    page.on("console", (msg) => {
      if (msg.type() === "error") consoleErrors.push(msg.text());
    });

    // ── Title Card ─────────────────────────────────────────────────
    await showTitleCard(
      page,
      "GeneXpert — ASTM Push Mode",
      "Create → Configure → Simulate → Results",
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

    // Clean up stale E2E GeneXpert analyzers from previous runs.
    // Prevents routing ambiguity (multiple analyzers matching same identifier_pattern).
    const listResp = await page.request.get(
      "/api/OpenELIS-Global/rest/analyzer/analyzers",
    );
    if (listResp.ok()) {
      const data = await listResp.json();
      const analyzers = data.analyzers ?? [];
      for (const a of analyzers) {
        if (a.name?.includes("GeneXpert") && a.name?.includes("E2E")) {
          await page.request.delete(
            `/api/OpenELIS-Global/rest/analyzer/analyzers/${a.id}`,
          );
          console.log(`Cleaned stale analyzer: ${a.name} (id=${a.id})`);
        }
      }
    }

    await videoPause(page, 1_500, testInfo);

    // ── Step 2: Create GeneXpert analyzer ──────────────────────────
    await showStepCard(
      page,
      2,
      "Create GeneXpert ASTM Analyzer",
      2000,
      testInfo,
    );

    const addButton = page.locator('[data-testid="add-analyzer-button"]');
    await expect(addButton).toBeVisible({ timeout: 5_000 });
    await addButton.click();

    const analyzerForm = page.locator('[data-testid="analyzer-form"]');
    await expect(analyzerForm).toBeVisible({ timeout: 10_000 });
    await videoPause(page, 1_000, testInfo);

    // Fill name
    const nameInput = page.locator('[data-testid="analyzer-form-name-input"]');
    await nameInput.fill(createdAnalyzerName);
    await videoPause(page, 500, testInfo);

    // Select plugin type — Generic ASTM
    const pluginTypeDropdown = page.locator(
      '[data-testid="analyzer-form-plugin-type-dropdown"]',
    );
    const pluginTypeTrigger = pluginTypeDropdown.locator(
      'button[role="combobox"], .cds--list-box__field',
    );
    await expect(pluginTypeTrigger).toBeEnabled({ timeout: 10_000 });
    await pluginTypeTrigger.click();
    await videoPause(page, 500, testInfo);

    const astmPluginOption = page
      .locator('[role="option"]')
      .filter({ hasText: /Generic ASTM.*ASTM|ASTM.*Generic ASTM/i });
    await expect(astmPluginOption.first()).toBeVisible({ timeout: 3_000 });
    await astmPluginOption.first().click();
    await videoPause(page, 1_000, testInfo);

    // Select default config profile (GeneXpert ASTM)
    const defaultConfigDropdown = page.locator(
      '[data-testid="analyzer-form-default-config-dropdown"]',
    );
    const defaultConfigTrigger = defaultConfigDropdown.locator(
      'button[role="combobox"], .cds--list-box__field',
    );
    await expect(defaultConfigTrigger).toBeEnabled({ timeout: 10_000 });
    await defaultConfigTrigger.click();
    await videoPause(page, 500, testInfo);

    const profileOption = page
      .locator('[role="option"]')
      .filter({ hasText: /GeneXpert/i });
    await expect(profileOption.first()).toBeVisible({ timeout: 3_000 });
    await profileOption.first().click();
    await videoPause(page, 1_000, testInfo);

    // Select analyzer type — Molecular
    const typeDropdown = page.locator(
      '[data-testid="analyzer-form-type-dropdown"]',
    );
    const typeTrigger = typeDropdown.locator(
      'button[role="combobox"], .cds--list-box__field',
    );
    await expect(typeTrigger).toBeEnabled({ timeout: 10_000 });
    await typeTrigger.click();
    await videoPause(page, 500, testInfo);

    const molecularOption = page
      .locator('[role="option"]')
      .filter({ hasText: /Molecular/i });
    await expect(molecularOption.first()).toBeVisible({ timeout: 3_000 });
    await molecularOption.first().click();
    await videoPause(page, 500, testInfo);

    // Fill identifier pattern (for ASTM routing fallback)
    const identifierInput = page.locator(
      '[data-testid="analyzer-form-identifier-pattern-input"]',
    );
    if (
      await identifierInput.isVisible({ timeout: 2_000 }).catch(() => false)
    ) {
      await identifierInput.fill("GENEXPERT|CEPHEID");
      await videoPause(page, 500, testInfo);
    }

    // Fill IP address (ASTM simulator on harness network) and port
    const ipInput = page.locator('[data-testid="analyzer-form-ip-input"]');
    if (await ipInput.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await ipInput.fill("172.21.1.100");
      await videoPause(page, 500, testInfo);
    }
    const portInput = page.locator('[data-testid="analyzer-form-port-input"]');
    if (await portInput.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await portInput.clear();
      await portInput.fill("9600");
      await videoPause(page, 500, testInfo);
    }

    // Save the analyzer — triggers autoCreateTestMappings() + bridge registration
    const saveButton = page.locator(
      '[data-testid="analyzer-form-save-button"]',
    );
    await saveButton.click();
    await expect(analyzerForm).toBeHidden({ timeout: 15_000 });
    console.log(`Created analyzer: ${createdAnalyzerName}`);
    await videoPause(page, 1_500, testInfo);

    // ── Step 3: Verify analyzer in list ──────────────────────────
    await showStepCard(page, 3, "Verify Analyzer Created", 2000, testInfo);

    const searchInput = page.locator('[data-testid="analyzer-search-input"]');
    await searchInput.fill(createdAnalyzerName);
    await videoPause(page, 1_500, testInfo);

    const analyzerRow = page.locator("tbody tr", {
      hasText: new RegExp(
        createdAnalyzerName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"),
        "i",
      ),
    });
    await expect(analyzerRow.first()).toBeVisible({ timeout: 10_000 });
    console.log(`Found analyzer: ${createdAnalyzerName}`);
    if (isVideoProject(testInfo)) {
      await page.screenshot({
        path: `test-results/gx-01-analyzer-created.png`,
        fullPage: true,
      });
    }
    await videoPause(page, 1_000, testInfo);

    // ── Step 4: Test Connection ────────────────────────────────────
    await showStepCard(page, 4, "Test Analyzer Connection", 2000, testInfo);

    const overflow = analyzerRow.first().locator(".cds--overflow-menu").first();
    await overflow.click();
    await videoPause(page, 500, testInfo);

    const testConnectionAction = page
      .locator('[data-testid*="analyzer-action-test-connection"]')
      .first();
    await expect(testConnectionAction).toBeVisible({ timeout: 3_000 });
    await testConnectionAction.click();

    const connectionModal = page.locator(
      '[data-testid="test-connection-modal"]',
    );
    await expect(connectionModal).toBeVisible({ timeout: 10_000 });

    // Click "Test" and wait for result
    const testButton = page.locator(
      '[data-testid="test-connection-test-button"]',
    );
    await testButton.click();

    // Wait for success or error (bridge ASTM round-trip ~2s)
    const successTag = page.locator('[data-testid="test-connection-success"]');
    await expect(successTag).toBeVisible({ timeout: 15_000 });
    console.log("  ✓ Test connection succeeded");

    if (isVideoProject(testInfo)) {
      await page.screenshot({
        path: `test-results/gx-01b-test-connection.png`,
        fullPage: true,
      });
    }
    await videoPause(page, 2_000, testInfo);

    // Close the modal
    const closeButton = connectionModal
      .locator('button:has-text("Close"), [aria-label="Close"]')
      .first();
    if (await closeButton.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await closeButton.click();
    }
    await videoPause(page, 500, testInfo);

    // ── Step 5: Trigger ASTM push via simulator ────────────────────
    await showStepCard(
      page,
      5,
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

    // ── Step 6: Wait for results to arrive ─────────────────────────
    await showStepCard(
      page,
      6,
      "Waiting for ASTM Results (near-instant via bridge)...",
      2000,
      testInfo,
    );

    // Poll until results appear in the staging table
    let resultCount = 0;
    for (let attempt = 1; attempt <= 12; attempt++) {
      const resp = await page.request.get(
        `/api/OpenELIS-Global/rest/AnalyzerResults?type=${encodeURIComponent(createdAnalyzerName)}`,
      );
      const data = await resp.json().catch(() => null);
      resultCount = data?.resultList?.length ?? 0;
      if (resultCount > 0) break;
      await page.waitForTimeout(5_000);
    }
    console.log(`Results available after polling: ${resultCount}`);
    await videoPause(page, 1_000, testInfo);

    // ── Step 7: View imported results ──────────────────────────────
    await showStepCard(page, 7, "View Imported Results", 2000, testInfo);

    const apiResponsePromise = page
      .waitForResponse((resp) => resp.url().includes("/rest/AnalyzerResults"), {
        timeout: 30_000,
      })
      .catch(() => null);

    await page.goto(
      `AnalyzerResults?type=${encodeURIComponent(createdAnalyzerName)}`,
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

    // ── Step 8: Verify actual result values ────────────────────────
    await showStepCard(page, 8, "Verify Result Values", 2000, testInfo);

    const resultsTable = page.locator("table, .orderLegendBody");
    await expect(resultsTable.first()).toBeVisible({ timeout: 15_000 });

    // Verify EACH expected result value appears on the page.
    // AnalyserResults uses react-data-table-component (div rows, not <tr>).
    // The sample ID renders in [data-testid="LabNo"] only on the first row
    // of each sample group.
    await expect(
      page
        .locator('[data-testid="LabNo"]', {
          hasText: EXPECTED_RESULTS[0].sampleId,
        })
        .first(),
    ).toBeVisible({ timeout: 15_000 });

    for (const expected of EXPECTED_RESULTS) {
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
    if (isVideoProject(testInfo)) {
      await page.screenshot({
        path: `test-results/gx-02-staging-results.png`,
        fullPage: true,
      });
    }

    // ── Linger on staging results for the video ──────────────────
    await videoPause(page, 2_000, testInfo);

    // ── Steps 8-10: Accept results and verify in AccessionResults
    await acceptAndVerifyResults(page, testInfo, 8, "SPECIMEN-GX-001");

    // ── Completion Card ───────────────────────────────────────────
    await showTitleCard(
      page,
      "Import Complete",
      `GeneXpert ASTM: ${EXPECTED_RESULTS.length} results accepted & validated`,
      3000,
      testInfo,
    );
  });

  test.afterEach(async ({ page }) => {
    // Clean up created analyzer (unless CLEANUP=false for video inspection)
    if (!CLEANUP || !createdAnalyzerName) return;

    try {
      await page.goto("analyzers", { waitUntil: "domcontentloaded" });
      const searchInput = page.locator('[data-testid="analyzer-search-input"]');
      await searchInput.fill(createdAnalyzerName);
      await page.waitForTimeout(1_000);

      const row = page.locator("tbody tr", {
        hasText: new RegExp(
          createdAnalyzerName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"),
          "i",
        ),
      });
      if (
        await row
          .first()
          .isVisible({ timeout: 3_000 })
          .catch(() => false)
      ) {
        const overflow = row.first().locator(".cds--overflow-menu").first();
        await overflow.click();
        await page.waitForTimeout(500);

        const deleteAction = page
          .locator('[data-testid*="analyzer-action-delete"]')
          .first();
        if (
          await deleteAction.isVisible({ timeout: 2_000 }).catch(() => false)
        ) {
          await deleteAction.click();
          const confirmButton = page
            .getByRole("button", { name: /delete|confirm/i })
            .last();
          if (
            await confirmButton.isVisible({ timeout: 3_000 }).catch(() => false)
          ) {
            await confirmButton.click();
            await page.waitForTimeout(1_000);
          }
        }
      }
    } catch {
      // Cleanup failure is not a test failure
    }
  });
});
