import { test, expect, Page } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { createAndCompleteAccessions } from "../../../helpers/seed-tat-data";
import { seedHolidays } from "../../../helpers/seed-calendar-data";

/**
 * Number of fresh accessions the spec seeds in beforeAll. Test assertions
 * only require ≥1 populated row; 3 makes the histogram + breakdown in the
 * demo video look populated while keeping beforeAll under ~15s.
 */
const SEEDED_ACCESSION_COUNT = 3;

/**
 * Guard assertion: fail loudly when the TAT Report is empty.
 *
 * Rationale: the Summary tab renders "Insufficient data" tiles when no
 * rows match the filter (TATSummaryTab.js:100-101). If the seed failed
 * and this text is on the page, the test must fail — not record an
 * empty video like the previous iteration did.
 */
async function assertReportHasData(page: Page): Promise<void> {
  // Wait for either the populated stat cards or the empty state — both
  // are terminal. Then fail if we landed in the empty state.
  await expect(
    page.getByText(/Total Results|Insufficient data/).first(),
  ).toBeVisible({ timeout: 15_000 });
  await expect(page.getByText("Insufficient data")).toHaveCount(0);
}

test.describe("OGC-307: TAT Report (US2-US5)", () => {
  // Force all 4 user stories onto a single worker in sequence so beforeAll
  // fires exactly once. Playwright runs beforeAll per-worker, not per-describe.
  test.describe.configure({ mode: "serial" });

  // Seed once: create N fresh accessions and run each through the full app
  // flow (create order → enter results → validate), populating the milestone
  // timestamps the TAT Report queries. Also seed holidays for Working Time
  // calculations.
  //
  // Self-contained: uses the REST API end-to-end. No pre-loaded SQL fixtures
  // required — works in both the core-mode and harness-mode Playwright CI
  // jobs. This depends on OGC-584 (silent-200-no-persist fix in
  // SamplePatientEntryRestController) landing in the same PR so createSampleOrder
  // actually persists.
  test.beforeAll(async ({ browser }) => {
    const context = await browser.newContext({
      storageState: "playwright/.auth/user.json",
    });
    const page = await context.newPage();
    // Navigate first so the browser context has an authenticated session
    // (JSESSIONID + CSRF in localStorage) before issuing REST calls.
    await page.goto("/");

    await createAndCompleteAccessions(page, SEEDED_ACCESSION_COUNT);
    await seedHolidays(page, 2026);

    await page.close();
    await context.close();
  });

  test("US2 — TAT Summary report workflow", async ({ page }, testInfo) => {
    test.setTimeout(120_000);
    const demo = createDemoPresentation(page, testInfo);

    await test.step("Title card", async () => {
      await demo.title(
        "User Story 2: TAT Summary Report",
        "As a lab manager, generate TAT summary with stats, histogram, breakdown",
      );
    });

    await test.step("US2.1 — Navigate to TAT Report page", async () => {
      await page.goto("/TATReport");
      await expect(
        page.getByRole("heading", { name: "Turn Around Time Report" }),
      ).toBeVisible({ timeout: 15_000 });
      await demo.evidence("US2.1-tat-report-page");
      await demo.pause(2000);
    });

    await test.step("US2.2 — Verify default filter state", async () => {
      await expect(page.locator("#tat-segment")).toBeVisible();
      await expect(
        page.locator('[data-testid="generate-report-button"]'),
      ).toBeVisible();
      await demo.evidence("US2.2-default-filters");
      await demo.pause(1500);
    });

    await test.step("US2.3 — Generate report shows populated data", async () => {
      await page.locator('[data-testid="generate-report-button"]').click();
      await assertReportHasData(page);
      // Stat tile for Total Results must render with a numeric count.
      const totalTile = page
        .locator(".cds--tile")
        .filter({ hasText: "Total Results" })
        .first();
      await expect(totalTile).toBeVisible();
      await expect(totalTile).toContainText(/\d/);
      await demo.evidence("US2.3-report-generated-with-data");
      await demo.pause(3000);
    });

    await test.step("US2.4 — Verify tabs are present", async () => {
      await expect(page.locator('[data-testid="tab-summary"]')).toBeVisible();
      await expect(page.locator('[data-testid="tab-detail"]')).toBeVisible();
      await expect(page.locator('[data-testid="tab-trends"]')).toBeVisible();
      await demo.evidence("US2.4-tabs-visible");
      await demo.pause(1000);
    });

    await test.step("US2.5 — Verify filter summary badges", async () => {
      await expect(
        page.locator('[data-testid="filter-summary-badges"]'),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="filter-summary-badges"]'),
      ).toContainText("Receipt to Validation");
      await demo.evidence("US2.5-filter-badges");
      await demo.pause(1000);
    });
  });

  test("US3 — Detail List tab has rows", async ({ page }, testInfo) => {
    test.setTimeout(90_000);
    const demo = createDemoPresentation(page, testInfo);

    await test.step("Title card", async () => {
      await demo.title(
        "User Story 3: TAT Detail List",
        "Sortable, paginated list with milestone timestamps",
      );
    });

    await test.step("Navigate and generate report", async () => {
      await page.goto("/TATReport");
      await page.locator('[data-testid="generate-report-button"]').click();
      await assertReportHasData(page);
    });

    await test.step("US3.1 — Switch to Detail List tab and verify rows", async () => {
      await page.locator('[data-testid="tab-detail"]').click();
      await expect(page.locator('[data-testid="tab-detail"]')).toHaveAttribute(
        "aria-selected",
        "true",
        { timeout: 5_000 },
      );
      // Data-bearing: at least 1 detail row must render (we seeded 13).
      // NOTE: `data-testid="tab-detail"` is on the Carbon <Tab> button; the
      // table lives in the sibling <TabPanel>. Use the active tabpanel
      // (Carbon hides inactive panels with `hidden`) as the scope.
      const activePanel = page.locator('[role="tabpanel"]:not([hidden])');
      await expect(activePanel.locator("table tbody tr").first()).toBeVisible({
        timeout: 10_000,
      });
      const rowCount = await activePanel.locator("table tbody tr").count();
      expect(rowCount).toBeGreaterThanOrEqual(1);
      await demo.pause(2000);
      await demo.evidence("US3.1-detail-list-populated");
    });
  });

  test("US4 — Trends tab", async ({ page }, testInfo) => {
    test.setTimeout(90_000);
    const demo = createDemoPresentation(page, testInfo);

    await test.step("Title card", async () => {
      await demo.title(
        "User Story 4: TAT Trends",
        "Time series with aggregation and multi-series comparison",
      );
    });

    await test.step("Navigate and generate report", async () => {
      await page.goto("/TATReport");
      await page.locator('[data-testid="generate-report-button"]').click();
      await assertReportHasData(page);
    });

    await test.step("US4.1 — Switch to Trends tab", async () => {
      await page.locator('[data-testid="tab-trends"]').click();
      await demo.pause(2000);
      await expect(page.locator("#trend-interval")).toBeVisible({
        timeout: 10_000,
      });
      await demo.evidence("US4.1-trends-tab");
    });
  });

  test("US5 — Export", async ({ page }, testInfo) => {
    test.setTimeout(60_000);
    const demo = createDemoPresentation(page, testInfo);

    await test.step("Title card", async () => {
      await demo.title("User Story 5: TAT Export", "Export report data as CSV");
    });

    await test.step("Navigate and generate report", async () => {
      await page.goto("/TATReport");
      await page.locator('[data-testid="generate-report-button"]').click();
      await assertReportHasData(page);
    });

    await test.step("US5.1 — Verify export menu exists", async () => {
      await expect(page.locator(".cds--overflow-menu").first()).toBeVisible({
        timeout: 5_000,
      });
      await demo.evidence("US5.1-export-menu");
      await demo.pause(2000);
    });
  });
});
