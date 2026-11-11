import { expect, test, Page } from "../../../helpers/test-base";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Patient search from the search form.
 *
 * The predicates themselves are pinned by PatientSearchFuzzyTest; what this
 * spec adds is the part only a browser can prove - that the real form, with the
 * parameters it actually sends, finds a patient created moments earlier. Two of
 * those are easy to regress and invisible from a service test:
 *
 *   - the "Patient Id" field is sent as STNumber AND subjectNumber AND
 *     nationalID together, so those criteria have to behave as alternatives;
 *   - a misspelled surname has to come back with the patient, which is the
 *     whole point of the trigram/levenshtein predicates.
 *
 * Run with:
 *   cd frontend && npm run pw:test:core-foundational -- patient-search
 */

/** Alphabetic suffix - the site's @ValidName(LAST_NAME) charset rejects digits. */
function alphabeticSuffix(length = 6): string {
  const letters = "abcdefghijklmnopqrstuvwxyz";
  let out = "";
  for (let i = 0; i < length; i++) {
    out += letters[Math.floor(Math.random() * letters.length)];
  }
  return out;
}

interface SeededPatient {
  firstName: string;
  lastName: string;
  nationalId: string;
}

/** Creates a patient through the endpoint the Create Patient form posts to. */
async function createPatient(page: Page): Promise<SeededPatient> {
  const suffix = alphabeticSuffix();
  const patient: SeededPatient = {
    firstName: `Searchfirst${suffix}`,
    lastName: `Searchlast${suffix}`,
    nationalId: `PSN-${Date.now()}`,
  };

  await page.goto("/PatientManagement", { waitUntil: "domcontentloaded" });

  const result = await page.evaluate(async (seed) => {
    const csrf = localStorage.getItem("CSRF") || "";
    const response = await fetch(
      "/api/OpenELIS-Global/rest/PatientManagement",
      {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", "X-CSRF-Token": csrf },
        body: JSON.stringify({
          patientUpdateStatus: "ADD",
          nationalId: seed.nationalId,
          subjectNumber: "",
          lastName: seed.lastName,
          firstName: seed.firstName,
          gender: "M",
          birthDateForDisplay: "01/01/1990",
          idDocuments: [],
          // Omitting this yields a 500 in PatientUtil#setSystemUserID.
          patientContact: {
            person: {
              firstName: "",
              lastName: "",
              primaryPhone: "",
              email: "",
            },
          },
        }),
      },
    );
    return { status: response.status, body: await response.text() };
  }, patient);

  expect(
    result.status,
    `POST /rest/PatientManagement returned ${result.status}: ${result.body}`,
  ).toBeLessThan(400);

  return patient;
}

/** Fills the patient search form and submits it. */
async function search(
  page: Page,
  criteria: { lastName?: string; firstName?: string; patientId?: string },
): Promise<void> {
  await page.goto("/PatientHistory", { waitUntil: "domcontentloaded" });
  await expect(page.locator("#local_search")).toBeVisible({
    timeout: LONG_TIMEOUT,
  });

  for (const field of ["lastName", "firstName", "patientId"] as const) {
    const value = criteria[field];
    if (value !== undefined) {
      await page.locator(`#${field}`).fill(value);
    }
  }

  await page.locator("#local_search").click();
}

/** The results table cell carrying the patient's surname. */
function resultFor(page: Page, patient: SeededPatient) {
  return page
    .locator('[data-cy="patientResultsTable"]')
    .getByText(patient.lastName, { exact: false });
}

test.describe("Patient search", () => {
  let patient: SeededPatient;

  test.beforeAll(async ({ browser }) => {
    const page = await browser.newPage();
    patient = await createPatient(page);
    await page.close();
  });

  test("a patient saved through the UI is searchable straight away", async ({
    page,
  }) => {
    await search(page, { lastName: patient.lastName });

    await expect(resultFor(page, patient)).toBeVisible({ timeout: UI_TIMEOUT });
  });

  test("a two-character term still matches anywhere in the surname", async ({
    page,
  }) => {
    await search(page, { lastName: patient.lastName.slice(2, 4) });

    await expect(resultFor(page, patient)).toBeVisible({ timeout: UI_TIMEOUT });
  });

  test("a surname with two letters swapped still finds the patient", async ({
    page,
  }) => {
    const typo =
      patient.lastName.slice(0, -2) +
      patient.lastName.slice(-1) +
      patient.lastName.slice(-2, -1);

    await search(page, { lastName: typo });

    await expect(resultFor(page, patient)).toBeVisible({ timeout: UI_TIMEOUT });
  });

  test("the Patient Id field finds a patient by national id", async ({
    page,
  }) => {
    // The form sends this one value as STNumber, subjectNumber and nationalID
    // together; only the national id matches, and that has to be enough.
    await search(page, { patientId: patient.nationalId });

    await expect(resultFor(page, patient)).toBeVisible({ timeout: UI_TIMEOUT });
  });

  test("criteria that no patient satisfies return no rows", async ({
    page,
  }) => {
    await search(page, {
      lastName: patient.lastName,
      firstName: `Nomatch${alphabeticSuffix()}`,
    });

    await expect(resultFor(page, patient)).toHaveCount(0, {
      timeout: UI_TIMEOUT,
    });
  });
});
