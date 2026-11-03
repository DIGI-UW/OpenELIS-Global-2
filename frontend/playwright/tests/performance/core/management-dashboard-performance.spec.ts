import { expect, test } from "../../../helpers/test-base";
import { errors, type Page, type TestInfo } from "@playwright/test";
import {
  measureBrowserOperation,
  nearestRankPercentile,
  P95_MEASURED_ITERATIONS,
} from "../../../helpers/performance-evidence";
import {
  seedManagementDashboardMicrobiology,
  seedManagementDashboardVolume,
} from "../../../helpers/seed-management-dashboard-data";

const PAGE = "/dashboards/management";
const API = "/api/OpenELIS-Global/rest/dashboards/management";
const FAST_ENDPOINTS = [
  "summary",
  "equipment",
  "antibiogram",
  "stock",
  "quality",
  "surveillance",
];
const TAT_ENDPOINTS = ["kpis", "workload"];
const SCOPES = ["30d", "year"];
const CALLS_PER_ENDPOINT = 3;
const SAMPLES = 5000;
const FIRST_MEANINGFUL_PAINT_BUDGET_MS = 2500;
const ENDPOINT_BUDGET_MS = 1000;

type EndpointRow = {
  endpoint: string;
  scope: string;
  status: number;
  samplesMs: number[];
  medianMs: number;
};

const seed = async (page: Page) => {
  expect(
    process.env.MANAGEMENT_DASHBOARD_QUALIFICATION_DISPOSABLE,
    "Set MANAGEMENT_DASHBOARD_QUALIFICATION_DISPOSABLE=true only for a throwaway stack",
  ).toBe("true");
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const volume = {
    ...(await seedManagementDashboardVolume(page, SAMPLES)),
    isolates: await seedManagementDashboardMicrobiology(page),
  };
  expect(volume.samplesLast30Days).toBeGreaterThanOrEqual(SAMPLES);
  return volume;
};

const timeEndpoint = (page: Page, url: string) =>
  page.evaluate(async (target) => {
    const startedAt = performance.now();
    const response = await fetch(target, { credentials: "same-origin" });
    await response.text();
    return { ms: performance.now() - startedAt, status: response.status };
  }, url);

// A request the proxy times out keeps running on the server, so an endpoint
// that fails is not called again.
const measureEndpoints = async (page: Page, endpoints: string[]) => {
  const rows: EndpointRow[] = [];
  for (const scope of SCOPES) {
    for (const endpoint of endpoints) {
      const samplesMs: number[] = [];
      let status = 200;
      while (status === 200 && samplesMs.length < CALLS_PER_ENDPOINT) {
        const call = await timeEndpoint(
          page,
          `${API}/${endpoint}?scope=${scope}`,
        );
        samplesMs.push(call.ms);
        status = call.status;
      }
      rows.push({
        endpoint,
        scope,
        status,
        samplesMs,
        medianMs: nearestRankPercentile(samplesMs, 50),
      });
    }
  }
  return rows;
};

const slowEndpoints = (rows: EndpointRow[], budgetMs: number) =>
  rows
    .filter((row) => row.status !== 200 || row.medianMs > budgetMs)
    .map((row) => `/${row.endpoint}?scope=${row.scope}`);

// First meaningful paint is the KPI strip showing values.
const paintKpiStrip = async (
  page: Page,
): Promise<{ ms?: number; error?: string }> => {
  await page.goto(PAGE, { waitUntil: "commit" });
  try {
    return await (
      await page.waitForFunction(
        () => {
          const strip = document.querySelector('[data-testid="mgmt-kpis"]');
          if (strip && !strip.querySelector(".cds--skeleton__text")) {
            return { ms: performance.now() };
          }
          const failed = document.querySelector(
            '[data-testid="mgmt-dashboard"] .cds--actionable-notification',
          );
          return failed
            ? {
                error: `${failed.closest("[data-testid]:not([data-testid='mgmt-dashboard'])")?.getAttribute("data-testid") ?? "a section"} shows a server error`,
              }
            : null;
        },
        null,
        { timeout: 120_000, polling: "raf" },
      )
    ).jsonValue();
  } catch (error) {
    if (!(error instanceof errors.TimeoutError)) throw error;
    return { error: "the KPI strip did not paint within 120 s" };
  }
};

const attachEvidence = (testInfo: TestInfo, name: string, evidence: object) =>
  testInfo.attach(name, {
    body: JSON.stringify(
      {
        commit: process.env.APP_SHA || process.env.GITHUB_SHA || "local",
        ...evidence,
      },
      null,
      2,
    ),
    contentType: "application/json",
  });

test.describe("Management Overview performance at about 5,000 samples over 30 days", () => {
  test("section endpoints answer within a second", async ({
    page,
  }, testInfo) => {
    test.setTimeout(30 * 60_000);
    const volume = await seed(page);
    const endpoints = await measureEndpoints(page, FAST_ENDPOINTS);
    await attachEvidence(testInfo, "management-dashboard-endpoints", {
      volume,
      endpoints,
    });
    expect(slowEndpoints(endpoints, ENDPOINT_BUDGET_MS)).toEqual([]);
  });

  test("the KPI strip paints within 2.5 seconds and the TAT endpoints answer within a second", async ({
    page,
  }, testInfo) => {
    test.fail(
      true,
      "The TAT query loads each sample's ordering site in its own query and slows quadratically",
    );
    test.setTimeout(30 * 60_000);
    const volume = await seed(page);
    const probe = await paintKpiStrip(page);
    const firstMeaningfulPaint = probe.error
      ? null
      : await measureBrowserOperation(
          "first-meaningful-paint-30d",
          1,
          P95_MEASURED_ITERATIONS,
          FIRST_MEANINGFUL_PAINT_BUDGET_MS,
          async () => {
            const paint = await paintKpiStrip(page);
            if (paint.error) throw new Error(paint.error);
            return paint.ms!;
          },
        );
    const endpoints = probe.error
      ? []
      : await measureEndpoints(page, TAT_ENDPOINTS);
    await attachEvidence(testInfo, "management-dashboard-first-paint", {
      volume,
      probe,
      firstMeaningfulPaint,
      endpoints,
    });
    expect(probe.error).toBeUndefined();
    expect(firstMeaningfulPaint!.passed).toBe(true);
    expect(slowEndpoints(endpoints, ENDPOINT_BUDGET_MS)).toEqual([]);
  });
});
