import { test, expect } from "@playwright/test";
import { showTitleCard } from "../../../helpers/title-card";
import { videoPause } from "../../../helpers/video-pause";

test.describe("OGC-307: TAT Report (US2-US5)", () => {
  test("US2 — TAT Summary report workflow", async ({ page }, testInfo) => {
    test.setTimeout(120_000);

    await test.step("Title card", async () => {
      await showTitleCard(
        page,
        "User Story 2: TAT Summary Report",
        "As a lab manager, generate TAT summary with stats, histogram, breakdown",
        3000,
        testInfo,
      );
    });

    await test.step("US2.1 — Navigate to TAT Report page", async () => {
      await page.goto("/TATReport");
      await expect(page.getByText("Turn Around Time Report")).toBeVisible();
      await videoPause(page, 2000, testInfo);
    });

    await test.step("US2.2 — Verify default filter state", async () => {
      // Segment dropdown should default to "Receipt to Validation"
      await expect(page.locator("#tat-segment")).toBeVisible();
      // Generate Report button should be present
      await expect(
        page.locator('[data-testid="generate-report-button"]'),
      ).toBeVisible();
      await videoPause(page, 1500, testInfo);
    });

    await test.step("US2.3 — Generate report", async () => {
      await page.locator('[data-testid="generate-report-button"]').click();
      // Wait for summary data or no-results message
      await expect(
        page.getByText(/Total Results|No results found/).first(),
      ).toBeVisible({ timeout: 15_000 });
      await videoPause(page, 3000, testInfo);
    });

    await test.step("US2.4 — Verify tabs are present", async () => {
      await expect(page.locator('[data-testid="tab-summary"]')).toBeVisible();
      await expect(page.locator('[data-testid="tab-detail"]')).toBeVisible();
      await expect(page.locator('[data-testid="tab-trends"]')).toBeVisible();
      await videoPause(page, 1000, testInfo);
    });
  });

  test("US3 — Detail List tab", async ({ page }, testInfo) => {
    test.setTimeout(90_000);

    await test.step("Title card", async () => {
      await showTitleCard(
        page,
        "User Story 3: TAT Detail List",
        "Sortable, paginated list with milestone timestamps",
        3000,
        testInfo,
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
      await videoPause(page, 2000, testInfo);
      // Should see either a data table or no-results message
      await expect(
        page.getByText(/Lab Number|No results found/).first(),
      ).toBeVisible({ timeout: 10_000 });
    });
  });

  test("US4 — Trends tab", async ({ page }, testInfo) => {
    test.setTimeout(90_000);

    await test.step("Title card", async () => {
      await showTitleCard(
        page,
        "User Story 4: TAT Trends",
        "Time series with aggregation and multi-series comparison",
        3000,
        testInfo,
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
      await videoPause(page, 2000, testInfo);
      // Should see trend controls (aggregation dropdown)
      await expect(
        page.getByText(/Daily|No results found/).first(),
      ).toBeVisible({ timeout: 10_000 });
    });
  });

  test("US5 — Export", async ({ page }, testInfo) => {
    test.setTimeout(60_000);

    await test.step("Title card", async () => {
      await showTitleCard(
        page,
        "User Story 5: TAT Export",
        "Export report data as CSV or PDF",
        3000,
        testInfo,
      );
    });

    await test.step("Navigate and generate report", async () => {
      await page.goto("/TATReport");
      await page.locator('[data-testid="generate-report-button"]').click();
      await expect(
        page.getByText(/Total Results|No results found/).first(),
      ).toBeVisible({ timeout: 15_000 });
    });

    await test.step("US5.1 — Verify export menu exists", async () => {
      // Export menu should be visible after generating a report
      await expect(
        page.getByText("Export", { exact: true }).first(),
      ).toBeVisible();
      await videoPause(page, 2000, testInfo);
    });
  });
});
