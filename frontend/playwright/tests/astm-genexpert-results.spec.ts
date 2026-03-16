import { test, expect } from "@playwright/test";
import { showTitleCard, showStepCard } from "../helpers/title-card";
import { isVideoProject, videoPause } from "../helpers/video-pause";
import { acceptAndVerifyResults } from "../helpers/accept-results";

/**
 * GeneXpert ASTM Push → Results E2E Tests
 *
 * Two complementary tests run in every project (harness, demo-video, etc.):
 *
 *   1. **Pre-loaded validation** — Finds the GeneXpert analyzer seeded by
 *      seed-analyzers.sh, tests its connection, pushes ASTM, verifies results.
 *      Validates that the profile-based seeding pipeline works end-to-end.
 *
 *   2. **Full create flow** — Creates a new GeneXpert analyzer via the UI,
 *      pushes ASTM, verifies results, then cleans up. Validates the full
 *      user-facing workflow from analyzer creation to result acceptance.
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

/** Name used by seed-analyzers.sh — must match exactly */
const PRELOADED_NAME = "Cepheid GeneXpert (ASTM Mode)";

const EXPECTED_RESULTS = [
  { sampleId: "SPECIMEN-GX-001", testCode: "MTB-RIF", result: "NEGATIVE" },
  { sampleId: "SPECIMEN-GX-001", testCode: "RIF", result: "Sensitive" },
  { sampleId: "SPECIMEN-GX-001", testCode: "HIV-VL", result: "1250" },
  { sampleId: "SPECIMEN-GX-001", testCode: "COVID19", result: "NEGATIVE" },
];

// ── Shared helpers ─────────────────────────────────────────────────────

/** Navigate to analyzer dashboard and wait for API load */
async function goToAnalyzerDashboard(page: any) {
  const apiPromise = page.waitForResponse(
    (resp: any) =>
      resp.url().includes("/rest/analyzer/analyzers") && resp.status() === 200,
    { timeout: 30_000 },
  );
  await page.goto("analyzers", { waitUntil: "domcontentloaded" });
  await apiPromise;
  await expect(page.locator('[data-testid="analyzers-list"]')).toBeVisible({
    timeout: 30_000,
  });
}

/** Find an analyzer row by name in the dashboard table */
async function findAnalyzerRow(page: any, name: string, testInfo: any) {
  const searchInput = page.locator('[data-testid="analyzer-search-input"]');
  await searchInput.fill(name);
  await videoPause(page, 1_500, testInfo);

  const row = page.locator("tbody tr", {
    hasText: new RegExp(name.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"), "i"),
  });
  await expect(row.first()).toBeVisible({ timeout: 10_000 });
  console.log(`Found analyzer: ${name}`);
  return row;
}

/** Open overflow menu → Test Connection → verify success → close modal */
async function testConnection(page: any, analyzerRow: any, testInfo: any) {
  const overflow = analyzerRow.first().locator(".cds--overflow-menu").first();
  await overflow.click();
  await videoPause(page, 500, testInfo);

  const testConnectionAction = page
    .locator('[data-testid*="analyzer-action-test-connection"]')
    .first();
  await expect(testConnectionAction).toBeVisible({ timeout: 3_000 });
  await testConnectionAction.click();

  const connectionModal = page.locator('[data-testid="test-connection-modal"]');
  await expect(connectionModal).toBeVisible({ timeout: 10_000 });

  const testButton = page.locator(
    '[data-testid="test-connection-test-button"]',
  );
  await testButton.click();

  const successTag = page.locator('[data-testid="test-connection-success"]');
  await expect(successTag).toBeVisible({ timeout: 15_000 });
  console.log("  ✓ Test connection succeeded");

  if (isVideoProject(testInfo)) {
    await page.screenshot({
      path: `test-results/gx-test-connection.png`,
      fullPage: true,
    });
  }
  await videoPause(page, 2_000, testInfo);

  const closeButton = connectionModal
    .locator('button:has-text("Close"), [aria-label="Close"]')
    .first();
  if (await closeButton.isVisible({ timeout: 2_000 }).catch(() => false)) {
    await closeButton.click();
  }
  await videoPause(page, 500, testInfo);
}

/** Trigger ASTM push via simulator and poll until results arrive */
async function pushAstmAndWaitForResults(
  page: any,
  analyzerName: string,
  testInfo: any,
) {
  const simulatorRes = await page.request.post(
    `${SIMULATOR_URL}/simulate/astm/genexpert_astm`,
    { data: { destination: BRIDGE_DESTINATION, count: 1 } },
  );
  expect(simulatorRes.ok()).toBeTruthy();

  const simBody = await simulatorRes.json();
  console.log(
    `Simulator response: pushed=${simBody.pushed}, status=${simBody.status}`,
  );
  await videoPause(page, 2_000, testInfo);

  // Poll until results appear in the staging table
  let resultCount = 0;
  for (let attempt = 1; attempt <= 8; attempt++) {
    const resp = await page.request.get(
      `/api/OpenELIS-Global/rest/AnalyzerResults?type=${encodeURIComponent(analyzerName)}`,
    );
    const data = await resp.json().catch(() => null);
    resultCount = data?.resultList?.length ?? 0;
    if (resultCount > 0) break;
    await page.waitForTimeout(3_000);
  }
  console.log(`Results available after polling: ${resultCount}`);
  await videoPause(page, 1_000, testInfo);
}

/** Navigate to AnalyzerResults page and verify all expected values */
async function verifyResults(page: any, analyzerName: string, testInfo: any) {
  const apiResponsePromise = page
    .waitForResponse(
      (resp: any) => resp.url().includes("/rest/AnalyzerResults"),
      { timeout: 30_000 },
    )
    .catch(() => null);

  await page.goto(`AnalyzerResults?type=${encodeURIComponent(analyzerName)}`, {
    waitUntil: "domcontentloaded",
  });

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

  await videoPause(page, 3_000, testInfo);

  const resultsTable = page.locator("table, .orderLegendBody");
  await expect(resultsTable.first()).toBeVisible({ timeout: 15_000 });

  await expect(
    page
      .locator('[data-testid="LabNo"]', {
        hasText: EXPECTED_RESULTS[0].sampleId,
      })
      .first(),
  ).toBeVisible({ timeout: 10_000 });

  for (const expected of EXPECTED_RESULTS) {
    const inputCount = await page
      .locator(`input[value*="${expected.result}"]`)
      .count();

    if (inputCount > 0) {
      await expect(
        page.locator(`input[value*="${expected.result}"]`).first(),
      ).toBeVisible({ timeout: 5_000 });
    } else {
      await expect(
        page.getByText(expected.result, { exact: false }).first(),
      ).toBeVisible({ timeout: 5_000 });
    }

    console.log(
      `  ✓ ${expected.sampleId} → ${expected.testCode}: ${expected.result}`,
    );
  }

  console.log(
    `All ${EXPECTED_RESULTS.length} result values verified for GeneXpert ASTM!`,
  );

  if (isVideoProject(testInfo)) {
    await page.screenshot({
      path: `test-results/gx-staging-results.png`,
      fullPage: true,
    });
  }
  await videoPause(page, 2_000, testInfo);
}

// ═══════════════════════════════════════════════════════════════════════
// Test 1: Validate pre-loaded GeneXpert (seeded by seed-analyzers.sh)
// ═══════════════════════════════════════════════════════════════════════

test.describe("GeneXpert ASTM — Pre-loaded Validation", () => {
  test.setTimeout(300_000);

  test("pre-loaded: test connection → ASTM push → verify results", async ({
    page,
  }, testInfo) => {
    // ── Title Card ─────────────────────────────────────────────────
    await showTitleCard(
      page,
      "GeneXpert — Validate Pre-loaded",
      "Connection → Simulate → Results",
      3000,
      testInfo,
    );

    // ── Step 1: Navigate to dashboard ──────────────────────────────
    await showStepCard(
      page,
      1,
      "Navigate to Analyzer Dashboard",
      2000,
      testInfo,
    );
    await goToAnalyzerDashboard(page);
    await videoPause(page, 1_500, testInfo);

    // ── Step 2: Find pre-loaded analyzer ───────────────────────────
    await showStepCard(page, 2, "Find Pre-loaded GeneXpert", 2000, testInfo);
    const preloadedListResp = await page.request.get(
      "/api/OpenELIS-Global/rest/analyzer/analyzers",
    );
    const preloadedListJson = await preloadedListResp.json().catch(() => ({}));
    const allAnalyzers = preloadedListJson?.analyzers ?? [];
    const preloadedMatches = allAnalyzers.filter((a: any) =>
      (a?.name ?? "").includes(PRELOADED_NAME),
    );
    const analyzerRow = await findAnalyzerRow(page, PRELOADED_NAME, testInfo);

    // ── Step 3: Test Connection ────────────────────────────────────
    await showStepCard(page, 3, "Test Analyzer Connection", 2000, testInfo);
    await testConnection(page, analyzerRow, testInfo);

    // ── Step 4: Trigger ASTM push ──────────────────────────────────
    await showStepCard(
      page,
      4,
      "Send ASTM Message via Simulator",
      2000,
      testInfo,
    );
    await pushAstmAndWaitForResults(page, PRELOADED_NAME, testInfo);

    // ── Step 5: Verify results ─────────────────────────────────────
    await showStepCard(page, 5, "Verify Imported Results", 2000, testInfo);
    await verifyResults(page, PRELOADED_NAME, testInfo);

    // ── Step 6: Accept results ─────────────────────────────────────
    await acceptAndVerifyResults(page, testInfo, 6, "SPECIMEN-GX-001");

    // ── Completion Card ────────────────────────────────────────────
    await showTitleCard(
      page,
      "Validation Complete",
      `Pre-loaded GeneXpert: ${EXPECTED_RESULTS.length} results accepted`,
      3000,
      testInfo,
    );
  });
});

// ═══════════════════════════════════════════════════════════════════════
// Test 2: Full create flow (creates its own analyzer, cleans up after)
// ═══════════════════════════════════════════════════════════════════════

test.describe("GeneXpert ASTM — Full Create Flow", () => {
  test.setTimeout(300_000);

  let createdAnalyzerName: string;

  test("create analyzer → ASTM push → verify results", async ({
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
    await goToAnalyzerDashboard(page);

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

    const saveButton = page.locator(
      '[data-testid="analyzer-form-save-button"]',
    );
    await saveButton.click();
    await expect(analyzerForm).toBeHidden({ timeout: 15_000 });
    console.log(`Created analyzer: ${createdAnalyzerName}`);
    await videoPause(page, 1_500, testInfo);

    // ── Step 3: Verify analyzer in list ──────────────────────────
    await showStepCard(page, 3, "Verify Analyzer Created", 2000, testInfo);

    const analyzerRow = await findAnalyzerRow(
      page,
      createdAnalyzerName,
      testInfo,
    );

    if (isVideoProject(testInfo)) {
      await page.screenshot({
        path: `test-results/gx-01-analyzer-created.png`,
        fullPage: true,
      });
    }
    await videoPause(page, 1_000, testInfo);

    // ── Step 4: Test Connection ────────────────────────────────────
    await showStepCard(page, 4, "Test Analyzer Connection", 2000, testInfo);
    await testConnection(page, analyzerRow, testInfo);

    // ── Step 5: Trigger ASTM push via simulator ────────────────────
    await showStepCard(
      page,
      5,
      "Send ASTM Message via Simulator",
      2000,
      testInfo,
    );
    await pushAstmAndWaitForResults(page, createdAnalyzerName, testInfo);

    // ── Step 6: Wait for and verify results ────────────────────────
    await showStepCard(page, 6, "Verify Imported Results", 2000, testInfo);
    await verifyResults(page, createdAnalyzerName, testInfo);

    // ── Steps 7-9: Accept results and verify in AccessionResults ──
    await acceptAndVerifyResults(page, testInfo, 7, "SPECIMEN-GX-001");

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
