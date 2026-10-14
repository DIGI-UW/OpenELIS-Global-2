import { writeFile } from "fs/promises";
import { expect, test } from "../../../helpers/test-base";
import type { Page, TestInfo } from "@playwright/test";
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
const FIRST_MEANINGFUL_PAINT_BUDGET_MS = 2500;
// Each section loads on its own request; one slower than a second leaves its
// card blank well after the first paint budget.
const ENDPOINT_BUDGET_MS = 1000;

const seed = async (page: Page) => {
  expect(
    process.env.MANAGEMENT_DASHBOARD_QUALIFICATION_DISPOSABLE,
    "Set MANAGEMENT_DASHBOARD_QUALIFICATION_DISPOSABLE=true only for a throwaway stack",
  ).toBe("true");
  await page.goto("/", { waitUntil: "domcontentloaded" });
  return {
    ...(await seedManagementDashboardVolume(page)),
    isolates: await seedManagementDashboardMicrobiology(page),
  };
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
  const rows = [];
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

// First meaningful paint is the KPI strip showing values.
const measureFirstMeaningfulPaint = async (page: Page) => {
  await page.goto(PAGE, { waitUntil: "commit" });
  const paint = await (
    await page.waitForFunction(
      () => {
        const strip = document.querySelector('[data-testid="mgmt-kpis"]');
        if (strip && !strip.querySelector(".cds--skeleton__text")) {
          return performance.now();
        }
        return document.querySelector(
          '[data-testid="mgmt-dashboard"] .cds--actionable-notification',
        )
          ? -1
          : null;
      },
      null,
      { timeout: 120_000, polling: "raf" },
    )
  ).jsonValue();
  if (paint === -1) throw new Error("The KPI strip failed to load");
  return paint as number;
};

const attachEvidence = async (
  testInfo: TestInfo,
  name: string,
  evidence: object,
) => {
  const path = testInfo.outputPath(`${name}.json`);
  await writeFile(
    path,
    `${JSON.stringify(
      {
        commit: process.env.APP_SHA || process.env.GITHUB_SHA || "local",
        ...evidence,
      },
      null,
      2,
    )}\n`,
  );
  await testInfo.attach(name, { path, contentType: "application/json" });
};

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
    expect(
      endpoints
        .filter(
          (row) => row.status !== 200 || row.medianMs > ENDPOINT_BUDGET_MS,
        )
        .map((row) => `/${row.endpoint}?scope=${row.scope}`),
    ).toEqual([]);
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
    const firstMeaningfulPaint = await measureBrowserOperation(
      "first-meaningful-paint-30d",
      1,
      P95_MEASURED_ITERATIONS,
      FIRST_MEANINGFUL_PAINT_BUDGET_MS,
      () => measureFirstMeaningfulPaint(page),
    );
    const endpoints = await measureEndpoints(page, TAT_ENDPOINTS);
    await attachEvidence(testInfo, "management-dashboard-first-paint", {
      volume,
      firstMeaningfulPaint,
      endpoints,
    });
    expect(firstMeaningfulPaint.passed).toBe(true);
    expect(
      endpoints
        .filter(
          (row) => row.status !== 200 || row.medianMs > ENDPOINT_BUDGET_MS,
        )
        .map((row) => `/${row.endpoint}?scope=${row.scope}`),
    ).toEqual([]);
  });
});
