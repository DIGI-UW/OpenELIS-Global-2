import { expect, test } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import { seedMicrobiologyMvpCase } from "../../../helpers/seed-microbiology-data";
import {
  attachBrowserPerformanceEvidence,
  measureBrowserOperation,
  type BrowserPerformanceEvidence,
} from "../../../helpers/performance-evidence";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const WARMUPS = 2;
const MEASURED = 10;
const PICKER_BUDGET_MS = 500;

const measureReagentOverviewRequest = async (page: Page, caseUrl: string) => {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes("/rest/microbiology/cases/") &&
      response.url().endsWith("/reagent-lots") &&
      response.status() === 200,
  );
  await page.goto(caseUrl, { waitUntil: "domcontentloaded" });
  const response = await responsePromise;
  await response.finished();
  const duration = response.request().timing().responseEnd;
  expect(
    duration,
    "Reagent overview request must expose response timing",
  ).toBeGreaterThanOrEqual(0);
  return duration;
};

test.describe("Microbiology reagent lot picker performance qualification", () => {
  test("loads service data and renders the culture picker below 500 ms p95", async ({
    page,
    browser,
  }, testInfo) => {
    test.setTimeout(5 * 60_000);
    expect(
      process.env.MICROBIOLOGY_QUALIFICATION_DISPOSABLE,
      "Set MICROBIOLOGY_QUALIFICATION_DISPOSABLE=true only for a throwaway stack",
    ).toBe("true");
    const commit = process.env.OGC782_COMMIT || process.env.GITHUB_SHA;
    expect(
      commit,
      "Set OGC782_COMMIT to the exact application commit",
    ).toBeTruthy();

    const seeded = await seedMicrobiologyMvpCase(page);
    const caseUrl = `/Microbiology/cases/${seeded.caseId}?section=setup`;
    const measurements = [];
    measurements.push(
      await measureBrowserOperation(
        "reagent-lot-service-load",
        WARMUPS,
        MEASURED,
        PICKER_BUDGET_MS,
        () => measureReagentOverviewRequest(page, caseUrl),
      ),
    );

    await page.goto(caseUrl, { waitUntil: "domcontentloaded" });
    const setup = page.getByRole("region", { name: "Inoculation" });
    await expect(
      setup.getByRole("button", { name: "Start inoculation" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    measurements.push(
      await measureBrowserOperation(
        "reagent-lot-picker-render",
        WARMUPS,
        MEASURED,
        PICKER_BUDGET_MS,
        async () => {
          const startedAt = await page.evaluate(() => performance.now());
          await setup
            .getByRole("button", { name: "Start inoculation" })
            .click();
          await expect(
            setup.getByRole("searchbox", {
              name: "Scan or enter lot number",
            }),
          ).toBeVisible();
          const renderedAt = await page.evaluate(() => performance.now());
          await setup.getByRole("button", { name: "Cancel" }).click();
          return renderedAt - startedAt;
        },
      ),
    );

    const overviewResponse = await page.request.get(
      `/api/OpenELIS-Global/rest/microbiology/cases/${seeded.caseId}/reagent-lots`,
    );
    expect(overviewResponse.ok()).toBe(true);
    const overview = await overviewResponse.json();
    const viewport = page.viewportSize();
    const evidence: BrowserPerformanceEvidence = {
      commit: commit as string,
      environment: {
        browser: browser.browserType().name(),
        browserVersion: browser.version(),
        node: process.version,
        platform: `${process.platform}-${process.arch}`,
        viewport: viewport ? `${viewport.width}x${viewport.height}` : "unknown",
        baseUrl: String(testInfo.project.use.baseURL || ""),
      },
      dataVolume: {
        reagentRequirements: overview.requirements?.length || 0,
        eligibleAndBlockedLots: (overview.requirements || []).reduce(
          (total: number, requirement: { lots?: unknown[] }) =>
            total + (requirement.lots?.length || 0),
          0,
        ),
      },
      measurements,
      passed: measurements.every((measurement) => measurement.passed),
    };
    await attachBrowserPerformanceEvidence(testInfo, evidence);
    expect(
      evidence.passed,
      "One or more reagent picker p95 budgets failed",
    ).toBe(true);
  });
});
