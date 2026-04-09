import { test, expect } from "@playwright/test";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { createCompleteSample } from "../../../helpers/seed-tat-data";
import { seedHolidays } from "../../../helpers/seed-calendar-data";
import type { SampleConfig } from "../../../helpers/seed-tat-data";

const TEST_SAMPLES: SampleConfig[] = [
  {
    labNo: "TAT-E2E-001",
    receivedDate: "2026-03-01",
    receivedTime: "08:00",
    priority: "routine",
  },
  {
    labNo: "TAT-E2E-002",
    receivedDate: "2026-03-05",
    receivedTime: "14:30",
    priority: "stat",
  },
  {
    labNo: "TAT-E2E-003",
    receivedDate: "2026-03-10",
    receivedTime: "10:00",
    priority: "routine",
  },
];

test.describe("OGC-307: TAT Report (US2-US5)", () => {
  // API-based setup: create 3 complete samples + seed holidays
  // beforeAll creates data once for all tests in this describe block
  test.beforeAll(async ({ browser }) => {
    const context = await browser.newContext({
      storageState: "playwright/.auth/user.json",
    });
    const page = await context.newPage();

    for (const sample of TEST_SAMPLES) {
      await createCompleteSample(page, sample);
    }
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

    await test.step("US2.3 — Generate report", async () => {
      await page.locator('[data-testid="generate-report-button"]').click();
      await expect(
        page.getByText(/Total Results|No results found/).first(),
      ).toBeVisible({ timeout: 15_000 });
      await demo.evidence("US2.3-report-generated");
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

  test("US3 — Detail List tab", async ({ page }, testInfo) => {
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
      await expect(
        page.getByText(/Total Results|No results found/).first(),
      ).toBeVisible({ timeout: 15_000 });
    });

    await test.step("US3.1 — Switch to Detail List tab", async () => {
      await page.locator('[data-testid="tab-detail"]').click();
      await expect(page.locator('[data-testid="tab-detail"]')).toHaveAttribute(
        "aria-selected",
        "true",
        { timeout: 5_000 },
      );
      await demo.pause(2000);
      await demo.evidence("US3.1-detail-list-tab");
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
      await expect(
        page.getByText(/Total Results|No results found/).first(),
      ).toBeVisible({ timeout: 15_000 });
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
      await expect(
        page.getByText(/Total Results|No results found/).first(),
      ).toBeVisible({ timeout: 15_000 });
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
