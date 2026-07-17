import { test, expect } from "../../../helpers/test-base";
import { Page } from "@playwright/test";
import { seedSigmaData, SigmaSeed } from "../../../helpers/seed-qc-sigma-data";

/**
 * C.1 Westgard sigma metrics (OGC-704 compute, OGC-705 tile) — E2E.
 *
 * The exhaustive band math (WORLD_CLASS/ACCEPTABLE/MARGINAL/POOR + edge cases)
 * is covered deterministically by the SigmaMetrics unit test; this spec covers
 * only what a unit test can't:
 *
 *  1. real serialization — the calculable body (sigma present) and the
 *     NOT_CALCULABLE body (null sigma omitted by Jackson), fetched through the
 *     authenticated browser session (the seed-tat-data pattern).
 *  2. UI regression — the sigma tile renders, exposes the bias qualifier, the
 *     controlLots query works with instrumentId alone, and labels are
 *     translated. Each test names the defect it guards against.
 *
 * TEa is seeded once per block (Test rows are cached server-side, so mutating
 * TEa mid-block would not be observed). Data is seeded straight into Postgres
 * (see seed-qc-sigma-data.ts): neither a qc_statistics row nor per-test TEa has
 * a REST create path.
 */

const API = "/api/OpenELIS-Global";

interface StatsResponse {
  // Absent (omitted) when the category is NOT_CALCULABLE.
  sigma?: number;
  sigmaCategory: string;
}

async function fetchStatistics(
  page: Page,
  lotId: string,
): Promise<{ status: number; body: StatsResponse }> {
  return page.evaluate(
    async ({ api, lot }) => {
      const res = await fetch(`${api}/rest/qc/charts/${lot}/statistics`, {
        credentials: "include",
      });
      // Surface the status + raw text if the body isn't JSON (e.g. an auth
      // redirect or 500), instead of an opaque "Unexpected token" parse error.
      const text = await res.text();
      let body: StatsResponse;
      try {
        body = JSON.parse(text);
      } catch {
        throw new Error(
          `Non-JSON ${res.status} from statistics: ${text.slice(0, 200)}`,
        );
      }
      return { status: res.status, body };
    },
    { api: API, lot: lotId },
  );
}

test.describe("QC sigma metric — NOT_CALCULABLE serialization (OGC-704)", () => {
  let seed: SigmaSeed;

  // TEa unset → the compute short-circuits. Assert the real response body:
  // sigmaCategory present, sigma omitted (Jackson drops the null) — a contract
  // the controller unit test can't observe.
  test.beforeAll(() => {
    seed = seedSigmaData({ tea: null, mean: 100, sd: 2 });
  });

  test.afterAll(() => {
    seed?.restore();
  });

  test("TEa unset → NOT_CALCULABLE with sigma omitted", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });

    const { status, body } = await fetchStatistics(page, seed.lotId);

    expect(status).toBe(200);
    expect(body.sigmaCategory).toBe("NOT_CALCULABLE");
    expect(body).not.toHaveProperty("sigma");
  });
});

test.describe("QC sigma metric — calculable path + UI (OGC-705)", () => {
  let seed: SigmaSeed;

  // mean=100, sd=2 → CV 2%; TEa=10 → sigma 5.0 / ACCEPTABLE.
  test.beforeAll(() => {
    seed = seedSigmaData({ tea: 10, mean: 100, sd: 2 });
  });

  test.afterAll(() => {
    seed?.restore();
  });

  test("statistics endpoint returns the computed sigma", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });

    const { status, body } = await fetchStatistics(page, seed.lotId);

    expect(status).toBe(200);
    expect(body.sigma).toBeCloseTo(5.0, 5);
    expect(body.sigmaCategory).toBe("ACCEPTABLE");
  });

  // Regression: QCRestController#getActiveControlLots once required BOTH testId
  // and instrumentId, but ControlChartDetail requests it with instrumentId
  // alone → 400 → the whole chart page hung on its loading spinner and the
  // sigma tile was never reached. testId is now optional.
  test("controlLots accepts instrumentId alone", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });

    const status = await page.evaluate(
      async ({ api, id }) => {
        const res = await fetch(
          `${api}/rest/qc/controlLots?instrumentId=${id}`,
          { credentials: "include" },
        );
        return res.status;
      },
      { api: API, id: seed.analyzerId },
    );

    expect(status).toBe(200);
  });

  // The seeded lot is selected, /statistics is fetched, and the tile renders.
  test("sigma tile renders on the control chart", async ({ page }) => {
    await page.goto(`/analyzers/qc/charts/${seed.analyzerId}`, {
      waitUntil: "domcontentloaded",
    });

    await expect(page.getByTestId("sigma-value")).toBeVisible();
    await expect(page.getByTestId("sigma-badge")).toHaveText(/Acceptable/);
  });

  // Regression: Carbon <Tag> ignores an arbitrary title prop, so the
  // "sigma is CV-only, bias=0" caveat never reached the user. The caveat now
  // lives on a wrapping span (native tooltip) via the qc.chart.sigma.bias key.
  test("sigma badge exposes the bias qualifier", async ({ page }) => {
    await page.goto(`/analyzers/qc/charts/${seed.analyzerId}`, {
      waitUntil: "domcontentloaded",
    });

    const badge = page.getByTestId("sigma-badge");
    await expect(badge).toBeVisible();
    await expect(badge).toHaveAttribute("title", /bias/i);
  });

  // Regression: ControlChartDetail shipped without its en.json keys, so the
  // footer, filters, and actions rendered raw "qc.chart.*" ids. They now all
  // resolve — assert no raw key leaks anywhere on the rendered page.
  test("labels are translated, not raw i18n keys", async ({ page }) => {
    await page.goto(`/analyzers/qc/charts/${seed.analyzerId}`, {
      waitUntil: "domcontentloaded",
    });

    // Gate on the tile so a blank/blocked page can't spuriously satisfy the
    // count-zero assertion below.
    await expect(page.getByTestId("sigma-value")).toBeVisible();
    await expect(page.getByText(/\bqc\.chart\.[a-z]/i)).toHaveCount(0);
  });
});
