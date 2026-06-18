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

/**
 * V-04 positivity remediation guard.
 *
 * The dashboard's positivity figures (MIR numerator, positivity panel,
 * sporozoite rate) are only meaningful when the test catalog carries the
 * `test_result.significance` classification (the SILNAS distro). On a plain
 * `develop` catalog the backend honestly reports `positivityConfigured=false`
 * and the frontend renders the "not configured" banner instead of fabricating
 * zeros (degradation contract).
 *
 * So this guard asserts the *correct branch for the data actually present*:
 *   - configured  → at least one per-pathogen MIR row AND a real, non-withheld
 *                   sporozoite value (a `%`), proving catalog-driven positivity
 *                   produced figures rather than the empty/withheld fallback;
 *   - unconfigured → the explicit "not configured" banner is shown and NO fake
 *                   positivity figures leak through.
 *
 * It does NOT use response.ok() as a pass/fail — it asserts on visible,
 * auto-retrying UI state only.
 *
 * @returns true when the catalog was configured (positivity figures asserted).
 */
async function assertPositivityRemediation(page: Page): Promise<boolean> {
  const notConfigured = page.locator(
    '[data-testid="vector-positivity-not-configured"]',
  );
  const mir = page.locator('[data-testid="panel-mir"]');

  // Terminal: either the not-configured banner or a populated MIR table.
  await expect(
    notConfigured.or(mir.locator('[data-testid="mir-row"]').first()),
  ).toBeVisible({ timeout: 15_000 });

  if (await notConfigured.isVisible()) {
    // Degradation path (e.g. develop without the SILNAS catalog): the banner is
    // the reason there are no figures — NOT a silent zero-fill. Assert the
    // positivity-dependent panels are ABSENT (a deterministic state, not the
    // vacuous "no % text" on a missing element).
    await expect(page.locator('[data-testid="panel-mir"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="panel-positivity"]')).toHaveCount(
      0,
    );
    await expect(page.locator('[data-testid="vector-sporozoite"]')).toHaveCount(
      0,
    );
    return false;
  }

  // Configured path: at least one per-pathogen MIR row, each naming a pathogen.
  const rows = mir.locator('[data-testid="mir-row"]');
  await expect(rows.first()).toBeVisible({ timeout: 10_000 });
  await expect(
    rows.first().locator('[data-testid="mir-pathogen"]'),
  ).not.toBeEmpty();

  // The sporozoite KPI must carry a real value (Anopheles CSP-ELISA positive
  // pools / Anopheles specimens), i.e. a numeric percentage — NOT "withheld".
  const sporozoite = page.locator('[data-testid="vector-sporozoite"]');
  await expect(sporozoite).toBeVisible();
  await expect(sporozoite).toContainText(/\d+(\.\d+)?\s*%/);
  return true;
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

    await test.step("US1.4 — Per-pathogen MIR rows + real sporozoite value (V-04 positivity fix)", async () => {
      // Asserts catalog-driven positivity actually produced figures (≥1
      // per-pathogen MIR row + a non-withheld sporozoite %), or — on a catalog
      // without significance metadata — the honest "not configured" banner with
      // NO fabricated positivity. See assertPositivityRemediation.
      const configured = await assertPositivityRemediation(page);
      expect(
        configured,
        "Vector demo data must carry test_result.significance so the dashboard " +
          "shows real per-pathogen positivity; got the 'not configured' banner " +
          "instead. Seed the SILNAS catalog (significance column) for this demo.",
      ).toBe(true);
      await demo.evidence("US1.4-mir-per-pathogen-and-sporozoite");
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
