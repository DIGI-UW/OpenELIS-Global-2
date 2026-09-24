import { expect, test } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import {
  MICROBIOLOGY_CASE_READY_MARK,
  MICROBIOLOGY_WORKLIST_READY_MARK,
} from "../../../../src/components/microbiology/MicrobiologyPerformance";
import { parseMicrobiologyWorklistSearch } from "../../../../src/components/microbiology/MicrobiologyRoutes";
import {
  seedDenseMicrobiologyCase,
  seedMicrobiologyAstWorklistCases,
  seedMicrobiologyWorklistCases,
} from "../../../helpers/seed-microbiology-data";
import {
  attachBrowserPerformanceEvidence,
  measureBrowserOperation,
  P95_MEASURED_ITERATIONS,
  type BrowserPerformanceEvidence,
} from "../../../helpers/performance-evidence";

const WARMUPS = 2;
const MEASURED = P95_MEASURED_ITERATIONS;
const CULTURE_WORKLIST_URL =
  "/Microbiology/worklist?workflow=BACTERIOLOGY&sort=priority&page=1&pageSize=100";
const AST_WORKLIST_URL =
  "/Microbiology/worklist?grain=ast&workflow=BACTERIOLOGY&sort=priority&page=1&pageSize=100";
const CULTURE_WORKLIST_ENDPOINT =
  "/api/OpenELIS-Global/rest/microbiology/worklist?grain=cultures&workflow=BACTERIOLOGY&sort=priority&page=1&pageSize=100";
const AST_WORKLIST_ENDPOINT =
  "/api/OpenELIS-Global/rest/microbiology/worklist?grain=ast&workflow=BACTERIOLOGY&sort=priority&page=1&pageSize=100";

const waitForReadyMark = async (page: Page, markName: string) => {
  await page.waitForFunction(
    (name) => performance.getEntriesByName(name, "mark").length > 0,
    markName,
  );
  return page.evaluate((name) => {
    const marks = performance.getEntriesByName(name, "mark");
    return marks[marks.length - 1].startTime;
  }, markName);
};

const measureNavigation = async (page: Page, url: string, markName: string) => {
  await page.goto(url, { waitUntil: "domcontentloaded" });
  const readyAt = await waitForReadyMark(page, markName);
  await page.waitForLoadState("networkidle");
  return readyAt;
};

const measureBrowserFetch = async (page: Page, endpoint: string) =>
  page.evaluate(async (url) => {
    const startedAt = performance.now();
    const response = await fetch(url, { credentials: "same-origin" });
    if (!response.ok) {
      throw new Error(`Worklist request failed: ${response.status}`);
    }
    await response.json();
    return performance.now() - startedAt;
  }, endpoint);

const measureSummaryFilter = async (
  page: Page,
  activeTestId: string,
  allTestId: string,
  operationIndex: number,
) => {
  const activate = operationIndex % 2 === 0;
  const expectedStatus = activate
    ? activeTestId.replace("microbiology-worklist-summary-", "")
    : "";
  await page.evaluate(
    (markName) => performance.clearMarks(markName),
    MICROBIOLOGY_WORKLIST_READY_MARK,
  );
  const startedAt = await page.evaluate(() => performance.now());
  await Promise.all([
    page.waitForURL(
      (url) =>
        parseMicrobiologyWorklistSearch(url.search).status === expectedStatus,
    ),
    page.getByTestId(activate ? activeTestId : allTestId).click(),
  ]);
  const readyAt = await waitForReadyMark(
    page,
    MICROBIOLOGY_WORKLIST_READY_MARK,
  );
  return readyAt - startedAt;
};

test.describe("Microbiology browser performance qualification", () => {
  test("meets worklist, case-render, and page-interaction budgets", async ({
    page,
    browser,
  }, testInfo) => {
    test.setTimeout(15 * 60_000);
    expect(
      process.env.MICROBIOLOGY_QUALIFICATION_DISPOSABLE,
      "Set MICROBIOLOGY_QUALIFICATION_DISPOSABLE=true only for a throwaway stack",
    ).toBe("true");
    const commit = process.env.OGC782_COMMIT || process.env.GITHUB_SHA;
    expect(
      commit,
      "Set OGC782_COMMIT to the exact application commit",
    ).toBeTruthy();

    const worklistCases = await seedMicrobiologyWorklistCases(page, 200);
    const astWorklistCases = await seedMicrobiologyAstWorklistCases(page, 200);
    const denseCase = await seedDenseMicrobiologyCase(page);
    const measurements = [];

    await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });
    measurements.push(
      await measureBrowserOperation(
        "server-culture-worklist-fetch",
        WARMUPS,
        MEASURED,
        2000,
        () => measureBrowserFetch(page, CULTURE_WORKLIST_ENDPOINT),
      ),
    );
    measurements.push(
      await measureBrowserOperation(
        "server-ast-worklist-fetch",
        WARMUPS,
        MEASURED,
        2000,
        () => measureBrowserFetch(page, AST_WORKLIST_ENDPOINT),
      ),
    );

    measurements.push(
      await measureBrowserOperation(
        "culture-worklist-initial-render",
        WARMUPS,
        MEASURED,
        2000,
        () =>
          measureNavigation(
            page,
            CULTURE_WORKLIST_URL,
            MICROBIOLOGY_WORKLIST_READY_MARK,
          ),
      ),
    );
    measurements.push(
      await measureBrowserOperation(
        "ast-worklist-initial-render",
        WARMUPS,
        MEASURED,
        2000,
        () =>
          measureNavigation(
            page,
            AST_WORKLIST_URL,
            MICROBIOLOGY_WORKLIST_READY_MARK,
          ),
      ),
    );
    measurements.push(
      await measureBrowserOperation(
        "dense-case-initial-render",
        WARMUPS,
        MEASURED,
        1000,
        () =>
          measureNavigation(
            page,
            `/Microbiology/cases/${denseCase.caseId}`,
            MICROBIOLOGY_CASE_READY_MARK,
          ),
      ),
    );

    await page.goto(CULTURE_WORKLIST_URL, { waitUntil: "domcontentloaded" });
    await waitForReadyMark(page, MICROBIOLOGY_WORKLIST_READY_MARK);
    measurements.push(
      await measureBrowserOperation(
        "culture-worklist-filter",
        WARMUPS,
        MEASURED,
        300,
        async (iteration, warmup) => {
          const operationIndex = warmup ? iteration : WARMUPS + iteration;
          return measureSummaryFilter(
            page,
            "microbiology-worklist-summary-incubating",
            "microbiology-worklist-summary-total",
            operationIndex,
          );
        },
      ),
    );
    await page.goto(AST_WORKLIST_URL, { waitUntil: "domcontentloaded" });
    await waitForReadyMark(page, MICROBIOLOGY_WORKLIST_READY_MARK);
    measurements.push(
      await measureBrowserOperation(
        "ast-worklist-filter",
        WARMUPS,
        MEASURED,
        300,
        async (iteration, warmup) => {
          const operationIndex = warmup ? iteration : WARMUPS + iteration;
          return measureSummaryFilter(
            page,
            "microbiology-worklist-summary-in-progress",
            "microbiology-worklist-summary-in-queue",
            operationIndex,
          );
        },
      ),
    );

    const [culturePage, astPage] = await Promise.all([
      page.request.get(CULTURE_WORKLIST_ENDPOINT),
      page.request.get(AST_WORKLIST_ENDPOINT),
    ]);
    expect(culturePage.ok()).toBe(true);
    expect(astPage.ok()).toBe(true);
    const cultureVolume = await culturePage.json();
    const astVolume = await astPage.json();
    expect(cultureVolume.total).toBeGreaterThanOrEqual(200);
    expect(astVolume.total).toBeGreaterThanOrEqual(200);
    expect(denseCase.isolateIds).toHaveLength(5);
    expect(denseCase.astReadingCount).toBe(80);
    expect(denseCase.timelineEventCount).toBeGreaterThanOrEqual(30);

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
        seededCultureCases: worklistCases.length,
        activeCultureRows: cultureVolume.total,
        seededAstRuns: astWorklistCases.length,
        activeAstRows: astVolume.total,
        denseCaseIsolates: denseCase.isolateIds.length,
        denseCaseReadings: denseCase.astReadingCount,
        denseCaseTimelineEvents: denseCase.timelineEventCount,
      },
      measurements,
      passed: measurements.every((measurement) => measurement.passed),
    };
    await attachBrowserPerformanceEvidence(testInfo, evidence);

    expect(evidence.passed, "One or more browser p95 budgets failed").toBe(
      true,
    );
  });
});
