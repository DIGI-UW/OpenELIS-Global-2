import { test, expect, type Page } from "../../../helpers/test-base";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";
import {
  letters,
  seedOrderForPatient,
  seedPatient,
} from "../../../helpers/seed-patient-order";

/**
 * Add Or Modify Patient: creating a patient on the New Patient form, clearing
 * it, and the search criteria patient-search.spec.ts does not already cover
 * (sex, first name alone, date of birth, previous lab number).
 */

const PATIENT_MANAGEMENT = "/PatientManagement";

async function openSearch(page: Page) {
  await page.goto(PATIENT_MANAGEMENT, { waitUntil: "domcontentloaded" });
  await expect(
    page.getByRole("main").getByRole("button", { name: "Search", exact: true }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
}

async function openNewPatientForm(page: Page) {
  await openSearch(page);
  await page
    .getByRole("main")
    .getByRole("button", { name: "New Patient" })
    .click();
  await expect(page).toHaveURL(/\/PatientManagement\/new$/);
  await expect(
    page.getByRole("main").getByRole("button", { name: "Save" }),
  ).toBeVisible({ timeout: UI_TIMEOUT });
}

/** Submits the search form; resolves once the answer has arrived. */
async function runSearch(page: Page) {
  const answered = page.waitForResponse((r) =>
    r.url().includes("/rest/patient-search-results"),
  );
  await page
    .getByRole("main")
    .getByRole("button", { name: "Search", exact: true })
    .click();
  await answered;
}

function resultRowFor(page: Page, nationalId: string) {
  return page
    .locator('[data-cy="patientResultsTable"]')
    .getByRole("row")
    .filter({ hasText: nationalId });
}

async function chooseSearchSex(page: Page, sex: "Male" | "Female") {
  await page
    .getByRole("group", { name: "Sex", exact: true })
    .getByText(sex, { exact: true })
    .click();
  await expect(
    page.getByRole("radio", { name: sex, exact: true }),
  ).toBeChecked();
}

/** Day equals month, so the date reads the same in either display order. */
function unambiguousBirthDate(): string {
  const dayMonth = String(1 + Math.floor(Math.random() * 12)).padStart(2, "0");
  const year = 1930 + Math.floor(Math.random() * 70);
  return `${dayMonth}/${dayMonth}/${year}`;
}

test.describe("Add or modify patient", () => {
  test("External Search is unavailable without an external patient source", async ({
    page,
  }) => {
    await openSearch(page);
    await expect(
      page.getByRole("heading", { name: "Add Or Modify Patient" }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "External Search" }),
    ).toBeDisabled();
  });

  test("a patient entered on the New Patient form is saved and found by search", async ({
    page,
  }) => {
    const suffix = letters(8);
    const lastName = `Entrylast${suffix}`;
    const firstName = `Entryfirst${suffix}`;
    const nationalId = `PE${Date.now()}${letters(4)}`;
    const birthDate = unambiguousBirthDate();

    await test.step("fill and save the New Patient form", async () => {
      await openNewPatientForm(page);
      await page
        .getByRole("textbox", { name: "National ID*" })
        .fill(nationalId);
      await page.getByRole("textbox", { name: "Last Name" }).fill(lastName);
      await page.getByRole("textbox", { name: "First Name" }).fill(firstName);
      await page
        .getByRole("group", { name: "Sex *" })
        .getByText("Male", { exact: true })
        .click();
      await page
        .getByRole("textbox", { name: "Date of Birth *" })
        .fill(birthDate);
      await expect(
        page.getByRole("spinbutton", { name: "Age/Years" }),
      ).not.toHaveValue("");

      await page
        .getByRole("main")
        .getByRole("button", { name: "Save" })
        .click();

      await expect(
        page
          .getByRole("status")
          .filter({ hasText: "Patient Saved Successfully" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });

    await test.step("the saved patient reopens with what was entered", async () => {
      await expect(page).toHaveURL(/\/PatientManagement\/\d+$/);
      await expect(
        page.getByRole("textbox", { name: "National ID*" }),
      ).toHaveValue(nationalId);
      await expect(
        page.getByRole("textbox", { name: "Last Name" }),
      ).toHaveValue(lastName);
      await expect(
        page.getByRole("textbox", { name: "First Name" }),
      ).toHaveValue(firstName);
      await expect(
        page.getByRole("textbox", { name: "Date of Birth *" }),
      ).toHaveValue(birthDate);
      await expect(
        page.getByRole("radio", { name: "Male", exact: true }),
      ).toBeChecked();
    });

    await test.step("the saved patient is found by last name", async () => {
      await openSearch(page);
      await page.getByRole("textbox", { name: "Last Name" }).fill(lastName);
      await runSearch(page);
      const row = resultRowFor(page, nationalId);
      await expect(row).toHaveCount(1, { timeout: UI_TIMEOUT });
      await expect(row).toContainText(firstName);
      await expect(row).toContainText(birthDate);
    });
  });

  test("Clear empties everything typed on the New Patient form", async ({
    page,
  }) => {
    await openNewPatientForm(page);
    const nationalId = page.getByRole("textbox", { name: "National ID*" });
    const lastName = page.getByRole("textbox", { name: "Last Name" });
    const firstName = page.getByRole("textbox", { name: "First Name" });
    const birthDate = page.getByRole("textbox", { name: "Date of Birth *" });
    const female = page.getByRole("radio", { name: "Female", exact: true });
    await nationalId.fill(`PE${Date.now()}${letters(4)}`);
    await lastName.fill(`Clearlast${letters(6)}`);
    await firstName.fill(`Clearfirst${letters(6)}`);
    await page
      .getByRole("group", { name: "Sex *" })
      .getByText("Female", { exact: true })
      .click();
    await birthDate.fill("03/03/1975");
    await expect(female).toBeChecked();
    await expect(lastName).not.toHaveValue("");

    await page
      .getByRole("main")
      .getByRole("button", { name: /Clear$/ })
      .click();

    await expect(nationalId).toHaveValue("");
    await expect(lastName).toHaveValue("");
    await expect(firstName).toHaveValue("");
    await expect(birthDate).toHaveValue("");
    await expect(female).not.toBeChecked();
    await expect(page).toHaveURL(/\/PatientManagement\/new$/);
  });

  test("the sex filter keeps only the patient of that sex", async ({
    page,
  }) => {
    const male = await seedPatient(page, { gender: "M" });
    const female = await seedPatient(page, {
      gender: "F",
      lastName: male.lastName,
    });

    await openSearch(page);
    await page.getByRole("textbox", { name: "Last Name" }).fill(male.lastName);

    await chooseSearchSex(page, "Male");
    await runSearch(page);
    await expect(resultRowFor(page, male.nationalId)).toHaveCount(1, {
      timeout: UI_TIMEOUT,
    });
    await expect(resultRowFor(page, female.nationalId)).toHaveCount(0);

    await chooseSearchSex(page, "Female");
    await runSearch(page);
    await expect(resultRowFor(page, female.nationalId)).toHaveCount(1, {
      timeout: UI_TIMEOUT,
    });
    await expect(resultRowFor(page, male.nationalId)).toHaveCount(0);
  });

  test("first name alone finds the patient", async ({ page }) => {
    const patient = await seedPatient(page);

    await openSearch(page);
    await page
      .getByRole("textbox", { name: "First Name" })
      .fill(patient.firstName);
    await runSearch(page);

    const row = resultRowFor(page, patient.nationalId);
    await expect(row).toHaveCount(1, { timeout: UI_TIMEOUT });
    await expect(row).toContainText(patient.lastName);
  });

  test("date of birth alone finds the patient", async ({ page }) => {
    const patient = await seedPatient(page, {
      birthDate: unambiguousBirthDate(),
    });

    await openSearch(page);
    await page
      .getByRole("textbox", { name: "Date of Birth" })
      .fill(patient.birthDate);
    await runSearch(page);

    const row = resultRowFor(page, patient.nationalId);
    await expect(row).toHaveCount(1, { timeout: UI_TIMEOUT });
    await expect(row).toContainText(patient.lastName);
    await expect(row).toContainText(patient.birthDate);
  });

  test("a previous lab number finds the patient the order belongs to", async ({
    page,
  }) => {
    const patient = await seedPatient(page);
    const labNumber = await seedOrderForPatient(page, patient);

    await openSearch(page);
    await page
      .getByRole("textbox", { name: "Previous Lab Number" })
      .fill(labNumber);
    await runSearch(page);

    const row = resultRowFor(page, patient.nationalId);
    await expect(row).toHaveCount(1, { timeout: UI_TIMEOUT });
    await expect(row).toContainText(patient.lastName);
  });
});
