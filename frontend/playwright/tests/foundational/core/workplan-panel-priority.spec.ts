import { test, expect, Page } from "../../../helpers/test-base";
import {
  AMYLASE_TEST,
  RBC_TEST,
  SERUM,
  WHOLE_BLOOD,
  getJson,
  openServerPageHolding,
  orderTests,
} from "../../../helpers/results-nce-ui";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * A work plan lists the tests still to be run for the panel or the order
 * priority chosen, and offers Print Workplan once it has rows; printing opens
 * the plan as a PDF. Each case orders its own accessions and looks for them by
 * lab number.
 */

const planned = (page: Page, accession: string) =>
  page
    .getByRole("main")
    .locator('[data-cy="workplanResultsTable"]')
    .getByRole("row")
    .filter({ hasText: accession });

/**
 * The plan's selector has no label of its own (its title is helper text), so
 * it is found by id. Choosing an entry loads that plan; the page holding
 * `accession` is then opened.
 */
async function choosePlan(
  page: Page,
  query: string,
  value: string,
  accession: string,
) {
  const loaded = page.waitForResponse(
    (response) =>
      response.request().method() === "GET" &&
      response.url().includes(`/rest/${query}=${value}`) &&
      !response.url().includes("&page="),
    { timeout: LONG_TIMEOUT },
  );
  await page.getByRole("main").locator("#select-1").selectOption(value);
  await openServerPageHolding(page, await loaded, accession);
}

async function openPlan(
  page: Page,
  path: string,
  title: string,
  value: string,
) {
  await page.goto(path, { waitUntil: "domcontentloaded" });
  const main = page.getByRole("main");
  await expect(main.getByRole("heading", { name: title })).toBeVisible({
    timeout: NAV_TIMEOUT,
  });
  await expect(
    main.locator("#select-1").locator(`option[value="${value}"]`),
  ).toBeAttached({ timeout: NAV_TIMEOUT });
}

/** The list shows Print Workplan above and below its rows. */
async function expectPrintOffered(page: Page) {
  const print = page
    .getByRole("main")
    .getByRole("button", { name: "Print Workplan" });
  await expect(print).toHaveCount(2);
  await expect(print.first()).toBeVisible();
  await expect(print.last()).toBeVisible();
}

/** Print Workplan renders the plan as a PDF and opens it in a new tab. */
async function expectPrintOpensPdf(page: Page) {
  const printed = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().includes("/rest/PrintWorkplanReport"),
    { timeout: LONG_TIMEOUT },
  );
  const opened = page.waitForEvent("popup");
  await page
    .getByRole("main")
    .getByRole("button", { name: "Print Workplan" })
    .first()
    .click();
  const response = await printed;
  expect(response.headers()["content-type"]).toContain("application/pdf");
  expect((await response.body()).subarray(0, 5).toString()).toBe("%PDF-");
  const tab = await opened;
  await tab.waitForURL(/^blob:/);
  await tab.close();
}

async function panelOf(page: Page, testId: string) {
  const { memberships } = await getJson<{
    memberships: { panelId: string; panelName: string }[];
  }>(page, `/rest/test-catalog/tests/${testId}/panels`);
  expect(memberships.length, `test ${testId} is in a panel`).toBeGreaterThan(0);
  return memberships[0];
}

test.describe("Workplan by panel and by priority", () => {
  test("choosing a panel lists that panel's pending tests and offers printing", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const hematologyPanel = await panelOf(page, RBC_TEST);
    const otherPanel = await panelOf(page, AMYLASE_TEST);
    expect(otherPanel.panelId).not.toBe(hematologyPanel.panelId);
    const inPanel = await orderTests(page, WHOLE_BLOOD, [RBC_TEST]);
    const outOfPanel = await orderTests(page, SERUM, [AMYLASE_TEST]);
    const query = "WorkPlanByPanel?panel_id";

    await test.step(`${hematologyPanel.panelName} lists the RBC order only`, async () => {
      await openPlan(
        page,
        "/WorkplanByPanel",
        "Workplan By Panel",
        hematologyPanel.panelId,
      );
      await choosePlan(page, query, hematologyPanel.panelId, inPanel);
      await expect(planned(page, inPanel)).toContainText(
        "Red Blood Cells Count (RBC)",
        { timeout: UI_TIMEOUT },
      );
      await expect(planned(page, outOfPanel)).toHaveCount(0);
      await expectPrintOffered(page);
      await expectPrintOpensPdf(page);
    });

    await test.step(`${otherPanel.panelName} lists the Amylase order only`, async () => {
      await choosePlan(page, query, otherPanel.panelId, outOfPanel);
      await expect(planned(page, outOfPanel)).toContainText("Amylase", {
        timeout: UI_TIMEOUT,
      });
      await expect(planned(page, inPanel)).toHaveCount(0);
    });
  });

  test("choosing a priority lists the orders of that priority and offers printing", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const statOrder = await orderTests(page, WHOLE_BLOOD, [RBC_TEST], "stat");
    const routineOrder = await orderTests(page, WHOLE_BLOOD, [RBC_TEST]);
    const query = "WorkPlanByPriority?priority";

    await test.step("STAT lists the STAT order only", async () => {
      await openPlan(
        page,
        "/WorkplanByPriority",
        "Workplan By Priority",
        "STAT",
      );
      await choosePlan(page, query, "STAT", statOrder);
      await expect(planned(page, statOrder)).toContainText(
        "Red Blood Cells Count (RBC)",
        { timeout: UI_TIMEOUT },
      );
      await expect(planned(page, routineOrder)).toHaveCount(0);
      await expectPrintOffered(page);
    });

    await test.step("Routine lists the routine order only", async () => {
      await choosePlan(page, query, "ROUTINE", routineOrder);
      await expect(planned(page, routineOrder)).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(planned(page, statOrder)).toHaveCount(0);
    });
  });
});
