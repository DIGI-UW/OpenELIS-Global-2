import { test, expect, Page } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

/**
 * OGC-585 / V-04 — Vector Surveillance Reporting demo story proof.
 *
 * Build-stack UI demo (no analyzer harness): lives in demo/core, so it runs
 * under the `core-demo` (CI) and `core-demo-video` (local, slowMo + video)
 * Playwright projects. Record the video with:
 *   npm run pw:test:core-demo-video -- ogc-585-vector-surveillance.spec.ts
 *
 * The dashboard computes its indices from OpenELIS's own recorded vector data
 * (V-01/02/03 must be populated on the instance). Empty dates → the backend
 * returns all data, so the demo just opens the page and clicks Apply.
 */

/**
 * Guard: fail loudly if the dashboard is empty rather than record an empty
 * video. The MIR panel (or the empty-state) is terminal; the empty-state must
 * not be present.
 */
async function assertDashboardHasData(page: Page): Promise<void> {
  await expect(
    page
      .locator('[data-testid="panel-mir"], [data-testid="vector-empty"]')
      .first(),
  ).toBeVisible({ timeout: 15_000 });
  await expect(page.locator('[data-testid="vector-empty"]')).toHaveCount(0);
}

test.describe("OGC-585: Vector Surveillance Reporting (V-04)", () => {
  // One worker, in order, so the demo narration flows as a single story.
  test.describe.configure({ mode: "serial" });

  test("US1 — Dashboard renders computed surveillance indices", async ({
    page,
  }, testInfo) => {
    test.setTimeout(120_000);
    const demo = createDemoPresentation(page, testInfo);

    await test.step("Title card", async () => {
      await demo.title(
        "OGC-585 / V-04: Vector Surveillance Reporting",
        "Collection density, species, MIR, pathogen positivity and QC — computed from OpenELIS's own data",
      );
    });

    await test.step("US1.1 — Open Reports → Vector Surveillance", async () => {
      await page.goto("/VectorSurveillanceReport");
      await expect(
        page.getByRole("heading", { name: /Vector Surveillance/i }),
      ).toBeVisible({ timeout: 15_000 });
      await demo.evidence("US1.1-vector-surveillance-page");
      await demo.pause(2000);
    });

    await test.step("US1.2 — Apply and verify the dashboard populates", async () => {
      await page.locator('[data-testid="vector-apply"]').click();
      await assertDashboardHasData(page);
      await demo.evidence("US1.2-dashboard-populated");
      await demo.pause(3000);
    });

    await test.step("US1.3 — Verify the five surveillance panels render", async () => {
      for (const id of [
        "panel-density",
        "panel-species",
        "panel-mir",
        "panel-positivity",
        "panel-qc",
      ]) {
        await expect(page.locator(`[data-testid="${id}"]`)).toBeVisible();
      }
      await demo.evidence("US1.3-five-panels");
      await demo.pause(2000);
    });

    await test.step("US1.4 — MIR table shows resolution % and withheld sporozoite", async () => {
      const mir = page.locator('[data-testid="panel-mir"]');
      await expect(mir.locator("table tbody tr").first()).toBeVisible({
        timeout: 10_000,
      });
      await demo.evidence("US1.4-mir-table");
      await demo.pause(2000);
    });

    await test.step("US1.5 — Data freshness is shown (FR-013)", async () => {
      await expect(
        page.locator('[data-testid="vector-freshness"]'),
      ).toBeVisible();
      await demo.evidence("US1.5-freshness");
      await demo.pause(1500);
    });
  });

  test("US2/US3 — Site filter + PDF export", async ({ page }, testInfo) => {
    test.setTimeout(90_000);
    const demo = createDemoPresentation(page, testInfo);

    await test.step("Title card", async () => {
      await demo.title(
        "Filters + PDF export",
        "Scope by sampling site and date range; export the dashboard as a shareable PDF",
      );
    });

    await test.step("Navigate and apply", async () => {
      await page.goto("/VectorSurveillanceReport");
      await page.locator('[data-testid="vector-apply"]').click();
      await assertDashboardHasData(page);
    });

    await test.step("US2 — Site filter is available", async () => {
      await expect(page.locator("#vector-site")).toBeVisible();
      await demo.evidence("US2-site-filter");
      await demo.pause(1500);
    });

    await test.step("US3 — Export PDF is enabled with data present", async () => {
      await expect(
        page.locator('[data-testid="vector-export-pdf"]'),
      ).toBeEnabled();
      await demo.evidence("US3-export-pdf");
      await demo.pause(1500);
    });
  });
});
