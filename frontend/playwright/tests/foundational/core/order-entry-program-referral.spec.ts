import { test, expect, type Page } from "../../../helpers/test-base";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";
import {
  seedPatient,
  type SeededPatient,
} from "../../../helpers/seed-patient-order";
import { isSettingOn, setSetting } from "../../../fixtures/esig-admin";

/**
 * Add Order (/SamplePatientEntry) beyond the happy path covered elsewhere: the
 * program picked on the Program step, a test referred to a reference lab on the
 * Sample step, and "Remember site and requester" carrying both into the next
 * order.
 */

const API = "/api/OpenELIS-Global/rest";
const SERUM = "Serum";
const TEST_NAME = "Creatinine";
const REFERRAL_REASON = "Equipment failure";
const REFERENCE_LAB = "CEDRES";
const SITE = "279 - CAMES MAN";
const REQUESTER = "Prime, Optimus";
const UNIFIED_RESULTS = "resultsEntryUnifiedRoute";

async function serumTestId(page: Page, name: string): Promise<string> {
  const types: Array<{ id: string; value: string }> = await (
    await page.request.get(`${API}/user-sample-types`)
  ).json();
  const serum = types.find((type) => type.value === SERUM);
  expect(serum, `sample type ${SERUM}`).toBeTruthy();
  const catalog: { tests: Array<{ id: string; name: string }> } = await (
    await page.request.get(`${API}/sample-type-tests?sampleType=${serum!.id}`)
  ).json();
  const found = catalog.tests.find((t) => t.name === name);
  expect(found, `${name} orderable on ${SERUM}`).toBeTruthy();
  return found!.id;
}

async function selectPatientOnAddOrder(page: Page, patient: SeededPatient) {
  await page.goto("/SamplePatientEntry", { waitUntil: "domcontentloaded" });
  const main = page.getByRole("main");
  await main.getByRole("textbox", { name: "Last Name" }).fill(patient.lastName);
  await main.getByRole("button", { name: "Search", exact: true }).click();
  await page.locator(`label[for="${patient.patientPK}"]`).click();
  await expect(main.getByRole("textbox", { name: "National ID*" })).toHaveValue(
    patient.nationalId,
    { timeout: UI_TIMEOUT },
  );
  await main.getByRole("button", { name: "Next", exact: true }).click();
  await expect(main.getByRole("heading", { name: "Program" })).toBeVisible();
}

async function toSampleStep(page: Page) {
  await page.getByRole("button", { name: "Next", exact: true }).click();
  const sampleType = page.locator("#sampleId_0");
  await expect(sampleType).toBeVisible({ timeout: UI_TIMEOUT });
  await sampleType.selectOption({ label: SERUM });
  await page.getByText(TEST_NAME, { exact: true }).click();
  await expect(
    page.getByRole("checkbox", { name: TEST_NAME, exact: true }),
  ).toBeChecked();
}

async function chooseSuggestion(
  page: Page,
  field: string,
  typed: string,
  shown: string,
) {
  await page.getByRole("textbox", { name: field }).fill(typed);
  const suggestion = page
    .locator('[data-cy="auto-suggestion"]')
    .filter({ hasText: new RegExp(`^${shown}$`) });
  await expect(suggestion).toBeVisible({ timeout: UI_TIMEOUT });
  await suggestion.click();
}

/** Order step: generated lab number, site and requester. Returns the lab number. */
async function fillOrderStep(page: Page): Promise<string> {
  await page.getByRole("button", { name: "Next", exact: true }).click();
  const labNumber = page.getByRole("textbox", { name: "Lab Number *" });
  await expect(labNumber).toBeVisible({ timeout: UI_TIMEOUT });
  await page.getByRole("link", { name: "Generate" }).click();
  await expect(labNumber).not.toHaveValue("", { timeout: UI_TIMEOUT });
  await chooseSuggestion(page, "Search Site Name *", "CAMES MAN", SITE);
  await chooseSuggestion(page, "Search Requester", "Prime", REQUESTER);
  await expect(
    page.getByRole("textbox", { name: "Requester's FirstName:" }),
  ).toHaveValue("Optimus");
  return labNumber.inputValue();
}

async function submitOrder(page: Page) {
  const submit = page.getByRole("button", { name: "Submit", exact: true });
  await expect(submit).toBeEnabled({ timeout: UI_TIMEOUT });
  await submit.click();
  await expect(
    page.getByRole("heading", { name: "Successfully saved" }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
}

/** The order's test on the results worklist, expanded. */
async function openResultRow(page: Page, labNumber: string) {
  await page.goto(`/Results?accessionNumber=${encodeURIComponent(labNumber)}`, {
    waitUntil: "domcontentloaded",
  });
  const row = page
    .getByRole("row")
    .filter({ hasText: labNumber })
    .filter({ hasText: `${TEST_NAME}(${SERUM})` });
  await expect(row).toHaveCount(1, { timeout: NAV_TIMEOUT });
  await row.getByRole("button", { name: "Expand row" }).click();
  return row;
}

test.describe("Add Order program, referral and remembered requester", () => {
  test.describe("checked on the results worklist", () => {
    let unifiedWasOn = true;

    test.beforeEach(async ({ page }) => {
      unifiedWasOn = await isSettingOn(page, UNIFIED_RESULTS);
      if (!unifiedWasOn) await setSetting(page, UNIFIED_RESULTS, true);
    });

    test.afterEach(async ({ page }) => {
      if (!unifiedWasOn) await setSetting(page, UNIFIED_RESULTS, false);
    });

    test("the program picked on the Program step is saved with the order", async ({
      page,
    }) => {
      test.setTimeout(120_000);
      const patient = await seedPatient(page);
      let labNumber = "";

      await test.step("pick Cytology and enter the order", async () => {
        await selectPatientOnAddOrder(page, patient);
        await page
          .getByRole("combobox", { name: "Program" })
          .selectOption({ label: "Cytology" });
        await expect(
          page.getByRole("combobox", { name: "Source of Smear" }),
        ).toBeVisible();
        await toSampleStep(page);
        labNumber = await fillOrderStep(page);
        await submitOrder(page);
        await expect(
          page.getByRole("button", { name: "Place same site order" }),
        ).toHaveCount(0);
      });

      await test.step("the saved order carries the program", async () => {
        await openResultRow(page, labNumber);
        await expect(
          page.getByRole("button", { name: /Program info\s+Cytology/ }),
        ).toBeVisible({ timeout: UI_TIMEOUT });
      });
    });

    test("a test referred to a reference lab is saved as a referral with its reason and lab", async ({
      page,
    }) => {
      test.setTimeout(120_000);
      const patient = await seedPatient(page);
      const testId = await serumTestId(page, TEST_NAME);
      let labNumber = "";

      await test.step("refer the test on the Sample step", async () => {
        await selectPatientOnAddOrder(page, patient);
        await toSampleStep(page);
        await page.getByText("Refer test to a reference lab").click();
        await expect(
          page.getByRole("checkbox", { name: "Refer test to a reference lab" }),
        ).toBeChecked();
        const reason = page.locator(`#referralReasonId_0_${testId}`);
        const institute = page.locator(`#referredInstituteId_0_${testId}`);
        await reason.selectOption({ label: REFERRAL_REASON });
        await institute.selectOption({ label: REFERENCE_LAB });
        await expect(reason.locator("option:checked")).toHaveText(
          REFERRAL_REASON,
        );
        await expect(institute.locator("option:checked")).toHaveText(
          REFERENCE_LAB,
        );
        labNumber = await fillOrderStep(page);
        await submitOrder(page);
      });

      await test.step("the test shows as referred out on the worklist", async () => {
        const row = await openResultRow(page, labNumber);
        await expect(row).toContainText("Referred out");
        await expect(
          page.getByRole("button", { name: "Referred out", exact: true }),
        ).toBeDisabled();
      });

      await test.step("the referral records the reference lab and reason", async () => {
        const referred = await (
          await page.request.get(
            `${API}/ReferredOutTests?searchType=LAB_NUMBER&labNumber=${encodeURIComponent(labNumber)}`,
          )
        ).json();
        expect(referred.referralDisplayItems).toEqual([
          expect.objectContaining({
            accessionNumber: labNumber,
            referringTestName: TEST_NAME,
            referenceLabDisplay: REFERENCE_LAB,
            patientLastName: patient.lastName,
          }),
        ]);
        const reasons: Array<{ id: string; value: string }> = await (
          await page.request.get(`${API}/displayList/REFERRAL_REASONS`)
        ).json();
        const reasonId = reasons.find((r) => r.value === REFERRAL_REASON)?.id;
        const logbook = await (
          await page.request.get(
            `${API}/LogbookResults?labNumber=${encodeURIComponent(labNumber)}`,
          )
        ).json();
        expect(logbook.testResult).toEqual([
          expect.objectContaining({
            referredOut: true,
            referralReasonId: reasonId,
          }),
        ]);
      });
    });
  });

  test("Remember site and requester carries both into the next order", async ({
    page,
  }) => {
    test.setTimeout(150_000);
    const first = await seedPatient(page);
    const second = await seedPatient(page);

    await test.step("save an order remembering site and requester", async () => {
      await selectPatientOnAddOrder(page, first);
      await toSampleStep(page);
      await fillOrderStep(page);
      await page.getByText("Remember site and requester").click();
      await expect(
        page.getByRole("checkbox", { name: "Remember site and requester" }),
      ).toBeChecked();
      await submitOrder(page);
    });

    await test.step("the next order starts with the same site and requester", async () => {
      await page.getByRole("button", { name: "Place same site order" }).click();
      const main = page.getByRole("main");
      await main
        .getByRole("textbox", { name: "Last Name" })
        .fill(second.lastName);
      await main.getByRole("button", { name: "Search", exact: true }).click();
      await page.locator(`label[for="${second.patientPK}"]`).click();
      await expect(
        main.getByRole("textbox", { name: "National ID*" }),
      ).toHaveValue(second.nationalId, { timeout: UI_TIMEOUT });
      await main.getByRole("button", { name: "Next", exact: true }).click();
      await toSampleStep(page);
      await page.getByRole("button", { name: "Next", exact: true }).click();

      await expect(
        page.getByRole("textbox", { name: "Search Site Name *" }),
      ).toHaveValue(SITE, { timeout: UI_TIMEOUT });
      await expect(
        page.getByRole("textbox", { name: "Requester's FirstName:" }),
      ).toHaveValue("Optimus");
      await expect(
        page.getByRole("textbox", { name: "Requester's LastName:" }),
      ).toHaveValue("Prime");
    });
  });
});
