import { test, expect, Page } from "../../../helpers/test-base";
import {
  AMYLASE_TEST,
  RBC_TEST,
  SERUM,
  WHOLE_BLOOD,
  labUnitOf,
  openServerPageHolding,
  orderTests,
} from "../../../helpers/results-nce-ui";
import { enterResults } from "../../../helpers/seed-tat-data";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * Validation by Routine opens a lab unit's queue of results awaiting
 * validation; Validation by Range of Order loads the queue from a given lab
 * number onwards. Release happens per row from the review panel and is
 * covered with the order search, so neither case releases anything.
 */

const queueLoad = (page: Page, match: (url: string) => boolean) =>
  page.waitForResponse(
    (response) =>
      response.request().method() === "GET" &&
      response.url().includes("/rest/AccessionValidation?") &&
      !response.url().includes("&page=") &&
      match(response.url()),
    { timeout: LONG_TIMEOUT },
  );

const queued = (page: Page, accession: string) =>
  page.getByRole("main").getByTestId("LabNo").filter({ hasText: accession });

async function orderWithResult(
  page: Page,
  sampleTypeId: string,
  testId: string,
): Promise<string> {
  const accession = await orderTests(page, sampleTypeId, [testId]);
  await enterResults(page, accession);
  return accession;
}

test.describe("Result validation by routine and by range", () => {
  test("choosing a lab unit loads that unit's validation queue", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const hematologyUnit = await labUnitOf(page, RBC_TEST);
    const biochemistryUnit = await labUnitOf(page, AMYLASE_TEST);
    const hematologyOrder = await orderWithResult(page, WHOLE_BLOOD, RBC_TEST);
    const biochemistryOrder = await orderWithResult(page, SERUM, AMYLASE_TEST);
    const unit = page
      .getByRole("main")
      .getByRole("combobox", { name: "Select Test Unit" });

    const openUnit = async (unitId: string, accession: string) => {
      const loaded = queueLoad(page, (url) =>
        url.includes(`unitType=${unitId}&`),
      );
      await unit.selectOption(unitId);
      await openServerPageHolding(page, await loaded, accession);
    };

    await test.step("Hematology's queue holds the hematology result only", async () => {
      await page.goto("/ResultValidation", { waitUntil: "domcontentloaded" });
      await expect(
        unit.locator(`option[value="${hematologyUnit}"]`),
      ).toBeAttached({ timeout: NAV_TIMEOUT });
      await openUnit(hematologyUnit, hematologyOrder);
      await expect(queued(page, hematologyOrder)).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(queued(page, biochemistryOrder)).toHaveCount(0);
    });

    await test.step("Biochemistry's queue holds the biochemistry result only", async () => {
      await openUnit(biochemistryUnit, biochemistryOrder);
      await expect(queued(page, biochemistryOrder)).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(queued(page, hematologyOrder)).toHaveCount(0);
    });
  });

  test("a range search loads the queue from the lab number entered onwards", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const first = await orderWithResult(page, WHOLE_BLOOD, RBC_TEST);
    const second = await orderWithResult(page, WHOLE_BLOOD, RBC_TEST);
    const main = page.getByRole("main");
    const from = main.getByRole("textbox", {
      name: "Load Next 99 Records Starting at Lab Number",
    });

    const searchFrom = async (accession: string) => {
      await from.fill(accession);
      const loaded = queueLoad(page, (url) =>
        url.includes(`accessionNumber=${accession}&`),
      );
      await main.getByRole("button", { name: "Search", exact: true }).click();
      await openServerPageHolding(page, await loaded, accession);
    };

    await test.step("from the first lab number, both results are queued", async () => {
      await page.goto("/AccessionValidationRange", {
        waitUntil: "domcontentloaded",
      });
      await expect(from).toBeVisible({ timeout: NAV_TIMEOUT });
      await searchFrom(first);
      await expect(queued(page, first)).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(queued(page, second)).toBeVisible();
    });

    await test.step("from the second lab number, the first is left out", async () => {
      await searchFrom(second);
      await expect(queued(page, second)).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(queued(page, first)).toHaveCount(0);
    });
  });
});
