import { test, expect } from "../../../helpers/test-base";
import { seedSigmaData, SigmaSeed } from "../../../helpers/seed-qc-sigma-data";

/**
 * Westgard sigma metrics (OGC-704 compute, OGC-705 tile) — E2E.
 *
 * The band math and the serialized response body (sigma present, and omitted
 * when NOT_CALCULABLE) are covered deterministically by SigmaMetricsTest and
 * QCChartDataRestControllerTest. What only a browser can show is the UI
 * regressions below: each test names the defect it guards against, and each
 * reaches the statistics endpoint through the authenticated session to render.
 *
 * TEa is seeded once per block (Test rows are cached server-side, so mutating
 * TEa mid-block would not be observed). Data is seeded straight into Postgres
 * (see seed-qc-lot.ts): neither a qc_statistics row nor per-test TEa has a
 * REST create path.
 */

test.describe("QC sigma metric — calculable path + UI (OGC-705)", () => {
  let seed: SigmaSeed;

  // mean=100, sd=2 → CV 2%; TEa=10 → sigma 5.0 / ACCEPTABLE.
  test.beforeAll(() => {
    seed = seedSigmaData({ tea: 10, mean: 100, sd: 2 });
  });

  test.afterAll(() => {
    seed?.restore();
  });

  test.beforeEach(async ({ page }) => {
    await page.goto(`/analyzers/qc/charts/${seed.analyzerId}`, {
      waitUntil: "domcontentloaded",
    });
  });

  // The seeded lot is selected, /statistics is fetched, and the tile renders.
  // This also covers the regression where QCRestController#getActiveControlLots
  // required BOTH testId and instrumentId while ControlChartDetail requests it
  // with instrumentId alone → 400 → the page hung on its loading spinner and
  // the tile was never reached.
  test("sigma tile renders on the control chart", async ({ page }) => {
    await expect(page.getByTestId("sigma-value")).toBeVisible();
    await expect(page.getByTestId("sigma-badge")).toHaveText(/Acceptable/);
  });

  // Regression: Carbon <Tag> ignores an arbitrary title prop, so the
  // "sigma is CV-only, bias=0" caveat never reached the user. The caveat now
  // lives on a wrapping span (native tooltip) via the qc.chart.sigma.bias key.
  test("sigma badge exposes the bias qualifier", async ({ page }) => {
    const badge = page.getByTestId("sigma-badge");
    await expect(badge).toBeVisible();
    await expect(badge).toHaveAttribute("title", /bias/i);
  });

  // Regression: ControlChartDetail shipped without its en.json keys, so the
  // footer, filters, and actions rendered raw "qc.chart.*" ids. They now all
  // resolve — assert no raw key leaks anywhere on the rendered page.
  test("labels are translated, not raw i18n keys", async ({ page }) => {
    // Gate on the tile so a blank/blocked page can't spuriously satisfy the
    // count-zero assertion below.
    await expect(page.getByTestId("sigma-value")).toBeVisible();
    await expect(page.getByText(/\bqc\.chart\.[a-z]/i)).toHaveCount(0);
  });
});
