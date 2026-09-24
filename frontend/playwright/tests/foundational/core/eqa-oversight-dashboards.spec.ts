import { test, expect } from "../../../helpers/test-base";
import { UI_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";
import {
  seedOversightData,
  OversightSeed,
} from "../../../helpers/seed-eqa-data";

/**
 * EQA oversight dashboards: coverage, recent cycles and analyst competency
 * (OGC-613).
 *
 * All three are read-only rollups over rows other parts of the module write,
 * and all three were dark in the browser. They are also the pages an
 * accreditation assessor is shown, so a rollup that silently drops a scored
 * result is worse than one that errors. The spec seeds one scored cycle with
 * a known verdict mix and asserts each dashboard renders that cycle rather
 * than only that the page loads.
 */

const RUN = Date.now().toString(36);

let seed: OversightSeed;

test.describe("EQA oversight dashboards", () => {
  test.beforeAll(() => {
    seed = seedOversightData(RUN);
  });

  test.afterAll(() => {
    seed?.restore();
  });

  test("coverage, recent cycles and competency all render the scored cycle", async ({
    page,
  }) => {
    test.setTimeout(180_000);

    await test.step("coverage places the scheme in its section with the worst verdict", async () => {
      await page.goto("/qa/eqa/lab-performance/coverage", {
        timeout: NAV_TIMEOUT,
      });
      await expect(
        page.getByRole("heading", {
          name: "Lab EQA Performance — Coverage",
        }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      for (const tile of [
        "kpi-acceptance",
        "kpi-ontime",
        "kpi-nce",
        "kpi-uncovered",
      ]) {
        await expect(page.getByTestId(tile)).toBeVisible();
      }
      await expect(
        page.getByText("Each scheme's last four cycles, by section."),
      ).toBeVisible();

      // One acceptable and one unacceptable result on the same cycle, so the
      // cell takes the worst of the two.
      const coverageRow = page.locator("tr", { hasText: seed.schemeName });
      await expect(coverageRow).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(coverageRow.getByText(seed.sectionName)).toBeVisible();
      await expect(coverageRow.getByText("!", { exact: true })).toBeVisible();
    });

    await test.step("the switcher moves to recent cycles and the row counts the results", async () => {
      await page.getByText("Recent Cycles", { exact: true }).click();
      await expect(page).toHaveURL(/lab-performance\/recent/, {
        timeout: UI_TIMEOUT,
      });
      await expect(
        page.getByRole("heading", {
          name: "Lab EQA Performance — Recent Cycles",
        }),
      ).toBeVisible({ timeout: UI_TIMEOUT });

      const recentRow = page.locator("tr", { hasText: seed.cycleName });
      await expect(recentRow).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(recentRow.getByText("1 of 2")).toBeVisible();
      await expect(recentRow.getByText("Unacceptable")).toBeVisible();

      // The scheme filter is client-side, so a name that matches nothing
      // empties the table rather than erroring.
      const filter = page.getByPlaceholder("Filter scheme");
      await filter.fill(seed.schemeName);
      await expect(recentRow).toBeVisible();
      await filter.fill(`no-such-scheme-${RUN}`);
      await expect(
        page.getByText("No cycles reported in the last 12 months."),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    });

    await test.step("competency bands the analyst on the evidence it has", async () => {
      await page.goto("/qa/eqa/analyst-competency", { timeout: NAV_TIMEOUT });
      await expect(
        page.getByRole("heading", { name: "Analyst Competency" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      for (const tile of [
        "kpi-analysts",
        "kpi-competent",
        "kpi-under-review",
        "kpi-not-competent",
      ]) {
        await expect(page.getByTestId(tile)).toBeVisible();
      }

      const analystRow = page
        .locator("tr", { hasText: seed.analystName })
        .first();
      await expect(analystRow).toBeVisible({ timeout: UI_TIMEOUT });
      // One evaluable sample is below the four-sample evidence floor, so the
      // honest band is under review rather than a pass or a fail.
      await expect(analystRow.getByText("Under review").first()).toBeVisible();

      await analystRow.getByRole("button", { name: "View history" }).click();
      const history = page.getByTestId(/^history-/).first();
      await expect(history).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(history.getByText("Competency by analyte")).toBeVisible();
      await expect(
        history.getByText("Evidence · every event in the window"),
      ).toBeVisible();
      await expect(history.getByText(seed.analyteName).first()).toBeVisible();
      // The planted event is a failure, and the window's other leg reports
      // the accepted sample as merely assessed.
      await expect(history.getByText("Unacceptable score")).toBeVisible();
      await expect(history.getByText("Failure").first()).toBeVisible();
    });
  });
});
