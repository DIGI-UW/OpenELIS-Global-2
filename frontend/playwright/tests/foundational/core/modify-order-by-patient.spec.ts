import { test, expect, type Page } from "../../../helpers/test-base";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";
import {
  seedOrderForPatient,
  seedPatient,
  type SeededPatient,
} from "../../../helpers/seed-patient-order";

/**
 * Modify Order (/SampleEdit) reached through the patient search rather than a
 * lab number, and a new sample with a panel added to an existing order.
 */

const API = "/api/OpenELIS-Global/rest";
const SERUM_ID = "2";
const PANEL = "Serologie VIH";

async function openSearchByPatient(page: Page) {
  await page.goto("/SampleEdit", { waitUntil: "domcontentloaded" });
  await expect(
    page.getByRole("heading", { name: "Search By Patient" }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
}

async function search(page: Page) {
  const answered = page.waitForResponse((r) =>
    r.url().includes("/rest/patient-search-results"),
  );
  await page
    .getByRole("main")
    .getByRole("button", { name: "Search", exact: true })
    .click();
  await answered;
}

function resultRowFor(page: Page, patient: SeededPatient) {
  return page
    .locator('[data-cy="patientResultsTable"]')
    .getByRole("row")
    .filter({ hasText: patient.nationalId });
}

/** Picks the patient's result row; Modify Order opens on their order. */
async function openPatientOrder(
  page: Page,
  patient: SeededPatient,
  labNumber: string,
) {
  await expect(resultRowFor(page, patient)).toHaveCount(1, {
    timeout: UI_TIMEOUT,
  });
  await page.locator(`label[for="${patient.patientPK}"]`).click();
  await expect(page).toHaveURL(
    new RegExp(`/ModifyOrder\\?patientId=${patient.patientPK}$`),
    { timeout: NAV_TIMEOUT },
  );
  await expect(
    page.getByRole("button", { name: `National ID : ${patient.nationalId}` }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
  await expect(
    page.getByRole("button", { name: `Accession Number : ${labNumber}` }),
  ).toBeVisible();
}

test.describe("Modify Order by patient", () => {
  test("a search by first and last name opens the patient's order", async ({
    page,
  }) => {
    const patient = await seedPatient(page);
    const labNumber = await seedOrderForPatient(page, patient);

    await openSearchByPatient(page);
    const main = page.getByRole("main");
    await main
      .getByRole("textbox", { name: "Last Name" })
      .fill(patient.lastName);
    await main
      .getByRole("textbox", { name: "First Name" })
      .fill(patient.firstName);
    await search(page);

    await expect(resultRowFor(page, patient)).toContainText(patient.lastName, {
      timeout: UI_TIMEOUT,
    });
    await openPatientOrder(page, patient, labNumber);
  });

  test("a search by Patient Id opens the patient's order", async ({ page }) => {
    const patient = await seedPatient(page);
    const labNumber = await seedOrderForPatient(page, patient);

    await openSearchByPatient(page);
    await page
      .getByRole("textbox", { name: "Patient Id" })
      .fill(patient.nationalId);
    await search(page);

    await expect(resultRowFor(page, patient)).toContainText(patient.lastName, {
      timeout: UI_TIMEOUT,
    });
    await openPatientOrder(page, patient, labNumber);
  });

  test("the sex filter keeps only the patient of that sex", async ({
    page,
  }) => {
    const male = await seedPatient(page, { gender: "M" });
    const female = await seedPatient(page, {
      gender: "F",
      lastName: male.lastName,
    });

    await openSearchByPatient(page);
    await page
      .getByRole("main")
      .getByRole("textbox", { name: "Last Name" })
      .fill(male.lastName);
    await page
      .getByRole("group", { name: "Sex", exact: true })
      .getByText("Male", { exact: true })
      .click();
    await expect(
      page.getByRole("radio", { name: "Male", exact: true }),
    ).toBeChecked();
    await search(page);

    await expect(resultRowFor(page, male)).toHaveCount(1, {
      timeout: UI_TIMEOUT,
    });
    await expect(resultRowFor(page, female)).toHaveCount(0);
  });

  test("a sample with a panel added to the patient's order is saved on it", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const patient = await seedPatient(page);
    const labNumber = await seedOrderForPatient(page, patient);
    const catalog: {
      panels: Array<{ name: string; testIds: string }>;
      tests: Array<{ id: string; name: string }>;
    } = await (
      await page.request.get(`${API}/sample-type-tests?sampleType=${SERUM_ID}`)
    ).json();
    const panelTestIds = catalog.panels
      .find((panel) => panel.name === PANEL)!
      .testIds.split(",");
    const panelTests = catalog.tests
      .filter((t) => panelTestIds.includes(t.id))
      .map((t) => t.name);
    expect(panelTests.length, `${PANEL} tests orderable on serum`).toBe(
      panelTestIds.length,
    );
    const currentTests = page.getByRole("table", { name: "Current Tests" });

    await test.step("open the patient's order from the patient search", async () => {
      await openSearchByPatient(page);
      await page
        .getByRole("main")
        .getByRole("textbox", { name: "Last Name" })
        .fill(patient.lastName);
      await search(page);
      await openPatientOrder(page, patient, labNumber);
      await expect(
        page.getByRole("combobox", { name: "Program" }),
      ).toBeDisabled();
      await page.getByRole("button", { name: "Next", exact: true }).click();
      await expect(currentTests.getByRole("row")).toHaveCount(2, {
        timeout: UI_TIMEOUT,
      });
    });

    await test.step("add a serum sample with the panel and submit", async () => {
      await page.locator("#sampleId_0").selectOption({ label: "Serum" });
      await page.getByText(PANEL, { exact: true }).click();
      await expect(
        page.getByRole("checkbox", { name: PANEL, exact: true }),
      ).toBeChecked();
      await page.getByRole("button", { name: "Next", exact: true }).click();
      await expect(
        page.getByRole("heading", { name: `Lab Number: ${labNumber}` }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await page.getByRole("button", { name: "Submit", exact: true }).click();
      await expect(
        page.getByText("Sample Order Entry has been saved successfully"),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });

    await test.step("the order now lists the new sample's panel tests", async () => {
      await page.goto(
        `/ModifyOrder?accessionNumber=${encodeURIComponent(labNumber)}`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(
        page.getByRole("button", { name: `Accession Number : ${labNumber}` }),
      ).toBeVisible({ timeout: NAV_TIMEOUT });
      await page.getByRole("button", { name: "Next", exact: true }).click();
      await expect(currentTests.getByRole("row")).toHaveCount(
        2 + panelTests.length,
        { timeout: UI_TIMEOUT },
      );
      await expect(
        currentTests.getByRole("row").filter({ hasText: `${labNumber}-1` }),
      ).toContainText("Creatinine");
      for (const name of panelTests) {
        await expect(
          currentTests.getByRole("row").filter({ hasText: name }),
        ).toHaveCount(1);
      }
      await expect(
        currentTests.getByRole("row").filter({ hasText: `${labNumber}-2` }),
      ).toContainText("Serum");
    });
  });
});
