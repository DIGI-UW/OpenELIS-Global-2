import { expect, test } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import {
  MICROBIOLOGY_CASE_READY_MARK,
  MICROBIOLOGY_WORKLIST_READY_MARK,
} from "../../../../src/components/microbiology/MicrobiologyPerformance";
import { parseMicrobiologyWorklistSearch } from "../../../../src/components/microbiology/MicrobiologyRoutes";
import {
  seedDenseMicrobiologyCase,
  seedMicrobiologyWorklistCases,
} from "../../../helpers/seed-microbiology-data";
import {
  attachBrowserPerformanceEvidence,
  measureBrowserOperation,
  type BrowserPerformanceEvidence,
} from "../../../helpers/performance-evidence";

const WARMUPS = 2;
const MEASURED = 10;
const WORKLIST_URL =
  "/Microbiology/worklist?workflow=BACTERIOLOGY&stage=ALL&urgency=ALL&due=ALL&q=&sort=priority&page=1&pageSize=50";

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
  return waitForReadyMark(page, markName);
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
    const denseCase = await seedDenseMicrobiologyCase(page);
    const measurements = [];

    measurements.push(
      await measureBrowserOperation(
        "worklist-initial-render",
        WARMUPS,
        MEASURED,
        2000,
        () =>
          measureNavigation(
            page,
            WORKLIST_URL,
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

    await page.goto(WORKLIST_URL, { waitUntil: "domcontentloaded" });
    await waitForReadyMark(page, MICROBIOLOGY_WORKLIST_READY_MARK);
    await expect(
      page.getByLabel("Next page"),
      "The 200-case qualification workload must produce a second page",
    ).toBeEnabled();
    measurements.push(
      await measureBrowserOperation(
        "worklist-page-interaction",
        WARMUPS,
        MEASURED,
        300,
        async (iteration, warmup) => {
          const operationIndex = warmup ? iteration : WARMUPS + iteration;
          const nextPage = operationIndex % 2 === 0;
          const expectedPage = nextPage ? 2 : 1;
          await page.evaluate(
            (markName) => performance.clearMarks(markName),
            MICROBIOLOGY_WORKLIST_READY_MARK,
          );
          const startedAt = await page.evaluate(() => performance.now());
          await Promise.all([
            page.waitForURL(
              (url) =>
                parseMicrobiologyWorklistSearch(url.search).page ===
                expectedPage,
            ),
            page.getByLabel(nextPage ? "Next page" : "Previous page").click(),
          ]);
          const readyAt = await waitForReadyMark(
            page,
            MICROBIOLOGY_WORKLIST_READY_MARK,
          );
          return readyAt - startedAt;
        },
      ),
    );

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
        worklistCases: worklistCases.length,
        denseCaseIsolates: denseCase.isolateIds.length,
        denseCaseReadings: denseCase.astReadingCount,
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
