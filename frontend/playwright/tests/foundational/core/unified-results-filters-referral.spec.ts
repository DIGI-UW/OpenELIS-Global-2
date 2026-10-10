import { test, expect, Page } from "../../../helpers/test-base";
import {
  AMYLASE_TEST,
  HEMOGLOBIN_TEST,
  RBC_TEST,
  SERUM,
  WHOLE_BLOOD,
  firstRealOption,
  labUnitOf,
  openServerPageHolding,
  orderTests,
  signIfAsked,
} from "../../../helpers/results-nce-ui";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * The unified Results worklist narrows what it shows by lab unit, by patient
 * and by analysis status, and refers a test to a reference laboratory from
 * the row itself. Each case orders its own accessions and asserts only on
 * those.
 */

const worklistLoad = (page: Page, match: (url: string) => boolean) =>
  page.waitForResponse(
    (response) =>
      response.request().method() === "GET" &&
      response.url().includes("/rest/LogbookResults?") &&
      !response.url().includes("&page=") &&
      match(response.url()),
    { timeout: LONG_TIMEOUT },
  );

/** A worklist row (the expanded panel under it is a row of its own). */
const worklistRow = (page: Page, text: string) =>
  page
    .getByRole("main")
    .getByRole("row")
    .filter({ hasText: text })
    .filter({
      has: page.getByRole("button", { name: /^(Expand|Collapse) row$/ }),
    });

async function openResults(page: Page, query = "") {
  const labUnits = page.waitForResponse(
    (response) => response.url().includes("/rest/results-entry/lab-units"),
    { timeout: NAV_TIMEOUT },
  );
  await page.goto(`/Results${query}`, { waitUntil: "domcontentloaded" });
  await labUnits;
  await expect(
    page.getByRole("main").getByRole("combobox", { name: "Lab Unit" }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
}

async function chooseLabUnit(page: Page, unitId: string, accession: string) {
  const loaded = worklistLoad(page, (url) =>
    url.includes(`testSectionId=${unitId}`),
  );
  await page
    .getByRole("main")
    .getByRole("combobox", { name: "Lab Unit" })
    .selectOption(unitId);
  await openServerPageHolding(page, await loaded, accession);
}

test.describe("Unified Results filters", () => {
  test("the lab unit filter shows that unit's work and not another unit's", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const hematologyUnit = await labUnitOf(page, RBC_TEST);
    const biochemistryUnit = await labUnitOf(page, AMYLASE_TEST);
    expect(hematologyUnit).not.toBe(biochemistryUnit);
    const hematologyOrder = await orderTests(page, WHOLE_BLOOD, [RBC_TEST]);
    const biochemistryOrder = await orderTests(page, SERUM, [AMYLASE_TEST]);

    await test.step("Hematology lists the hematology order only", async () => {
      await openResults(page);
      await chooseLabUnit(page, hematologyUnit, hematologyOrder);
      await expect(worklistRow(page, hematologyOrder)).toContainText(
        "Red Blood Cells",
        { timeout: UI_TIMEOUT },
      );
      await expect(worklistRow(page, biochemistryOrder)).toHaveCount(0);
      await expect(page).toHaveURL(
        new RegExp(`testSectionId=${hematologyUnit}(&|$)`),
      );
    });

    await test.step("Biochemistry lists the biochemistry order only", async () => {
      await chooseLabUnit(page, biochemistryUnit, biochemistryOrder);
      await expect(worklistRow(page, biochemistryOrder)).toContainText(
        "Amylase",
        { timeout: UI_TIMEOUT },
      );
      await expect(worklistRow(page, hematologyOrder)).toHaveCount(0);
    });
  });

  test("searching by patient shows that patient's orders only", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const patientOrder = await orderTests(page, WHOLE_BLOOD, [RBC_TEST]);
    const otherPatientOrder = await orderTests(page, WHOLE_BLOOD, [RBC_TEST]);
    const main = page.getByRole("main");

    await test.step("find the patient by the order's lab number", async () => {
      await openResults(page);
      await main.getByRole("button", { name: "Search by patient" }).click();
      const panel = main.getByTestId("patient-search-panel");
      await panel
        .getByRole("textbox", { name: "Previous Lab Number" })
        .fill(patientOrder);
      await panel.getByRole("button", { name: "Search", exact: true }).click();
      await expect(
        panel.locator('[data-cy^="patient-result-row-"]'),
      ).toHaveCount(1, { timeout: LONG_TIMEOUT });
    });

    await test.step("choosing the patient loads that patient's worklist", async () => {
      const match = main
        .getByTestId("patient-search-panel")
        .locator('[data-cy^="patient-result-row-"]');
      const loaded = worklistLoad(page, (url) => url.includes("patientPK="));
      await match
        .locator('input[type="radio"]')
        .locator("xpath=..")
        .locator("label")
        .click();
      await openServerPageHolding(page, await loaded, patientOrder);
      await expect(main.getByTestId("selected-patient")).toContainText(
        "Testpatient",
      );
      await expect(worklistRow(page, patientOrder)).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(worklistRow(page, otherPatientOrder)).toHaveCount(0);
      await expect(page).toHaveURL(/patientId=\d+/);
    });
  });

  test("the status chips keep the rows in that status, and the chosen status survives a reload", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const accession = await orderTests(page, WHOLE_BLOOD, [
      RBC_TEST,
      HEMOGLOBIN_TEST,
    ]);
    const main = page.getByRole("main");
    const chips = main.locator(".unifiedResultsChips");
    const rbc = worklistRow(page, "Red Blood Cells");
    const hemoglobin = worklistRow(page, "Hemoglobin");
    const notStarted = chips.getByText("Not started (1)", { exact: true });
    const entered = chips.getByText("Accepted by technician (1)", {
      exact: true,
    });

    await test.step("enter a result on one of the order's two tests", async () => {
      await openResults(page, `?accessionNumber=${accession}`);
      await expect(rbc).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(hemoglobin).toBeVisible();
      await rbc.locator('input[id^="unifiedResultValue-"]').fill("4.5");
      await rbc.getByRole("button", { name: "Save", exact: true }).click();
      await signIfAsked(page);
      await expect(
        rbc.getByRole("button", { name: "Edit", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(chips.getByText("All (2)", { exact: true })).toBeVisible();
      await expect(notStarted).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(entered).toBeVisible();
    });

    await test.step("each status chip keeps its own rows", async () => {
      await notStarted.click();
      await expect(hemoglobin).toBeVisible();
      await expect(rbc).toHaveCount(0);
      await entered.click();
      await expect(rbc).toBeVisible();
      await expect(hemoglobin).toHaveCount(0);
      await expect(page).toHaveURL(/[?&]status=\d+/);
    });

    await test.step("a reload keeps the chosen status and the saved value", async () => {
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(rbc).toContainText("4.5", { timeout: NAV_TIMEOUT });
      await expect(hemoglobin).toHaveCount(0);
      await chips.getByText("All (2)", { exact: true }).click();
      await expect(hemoglobin).toBeVisible();
    });
  });
});

test.describe("Unified Results referral", () => {
  test("Refer this test records the referral with the row's save", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const accession = await orderTests(page, WHOLE_BLOOD, [RBC_TEST]);
    const main = page.getByRole("main");
    const row = worklistRow(page, accession);
    const referToggle = main.locator('[data-testid^="referral-toggle-"]');
    const referral = main.locator('[data-testid^="referral-"]').filter({
      has: page.getByRole("combobox", { name: "Reference laboratory" }),
    });

    await test.step("choose a reference laboratory and reason", async () => {
      await openResults(page, `?accessionNumber=${accession}`);
      await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
      await row.getByRole("button", { name: "Expand row" }).click();
      await expect(referToggle).toHaveText("Refer this test", {
        timeout: UI_TIMEOUT,
      });
      await referToggle.click();
      const lab = referral.getByRole("combobox", {
        name: "Reference laboratory",
      });
      const reason = referral.getByRole("combobox", {
        name: "Referral reason",
      });
      await lab.selectOption((await firstRealOption(lab)).value);
      await reason.selectOption((await firstRealOption(reason)).value);
      await expect(
        referral.getByRole("textbox", { name: "Referral date" }),
      ).toHaveValue(/^\d{2}\/\d{2}\/\d{4}$/);
      await expect(referToggle).toHaveText("Referral pending — edit");
    });

    await test.step("save the row", async () => {
      const saved = page.waitForResponse(
        (response) =>
          response.url().includes("/rest/results-entry/analysis/") &&
          response.request().method() === "POST",
        { timeout: LONG_TIMEOUT },
      );
      await row.getByRole("button", { name: "Save", exact: true }).click();
      await signIfAsked(page);
      await saved;
      await expect(referral).toHaveCount(0, { timeout: UI_TIMEOUT });
    });

    await test.step("after a reload the test shows as referred out", async () => {
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(
        row.getByText("Referred out", { exact: true }),
      ).toBeVisible();
      await row.getByRole("button", { name: "Expand row" }).click();
      await expect(referToggle).toHaveText("Referred out", {
        timeout: UI_TIMEOUT,
      });
      await expect(referToggle).toBeDisabled();
      await expect(
        main.getByRole("textbox", { name: "Reference lab report date" }),
      ).toBeVisible();
    });
  });
});
