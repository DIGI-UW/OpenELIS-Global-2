import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { csrfToken } from "../../../helpers/api-session";

/**
 * OGC-1406 — a sample added with "Add Sample" is saved even when Sample 1 is
 * left blank, and a sample with a type but no test blocks Submit with a
 * message naming it.
 */

const API = "/api/OpenELIS-Global";

const letters = (length: number) =>
  Array.from({ length }, () =>
    String.fromCharCode(65 + Math.floor(Math.random() * 26)),
  ).join("");

async function seedPatient(page: Page): Promise<string> {
  const lastName = `OSIX${letters(8)}`;
  const response = await page.request.post(`${API}/rest/PatientManagement`, {
    data: {
      patientPK: "",
      lastName,
      firstName: "Added",
      gender: "F",
      birthDateForDisplay: "01/02/1990",
      nationalId: `S6${letters(8)}`,
      patientUpdateStatus: "ADD",
      patientContact: {
        person: { firstName: "", lastName: "", primaryPhone: "", email: "" },
      },
    },
    headers: { "X-CSRF-Token": await csrfToken(page) },
  });
  expect(response.status()).toBe(200);
  return lastName;
}

/** A sample type with at least one test, and its first test. */
async function sampleTypeWithTest(page: Page) {
  const types = await (
    await page.request.get(`${API}/rest/user-sample-types`)
  ).json();
  for (const type of types) {
    const catalog = await (
      await page.request.get(
        `${API}/rest/sample-type-tests?sampleType=${type.id}`,
      )
    ).json();
    const test = (catalog.tests || []).find((t) => t.name);
    if (test) return { typeId: String(type.id), test };
  }
  throw new Error("No sample type with a test");
}

async function openSampleStep(page: Page, lastName: string) {
  await page.goto("/SamplePatientEntry", { timeout: NAV_TIMEOUT });
  await page.getByRole("textbox", { name: "Last Name" }).fill(lastName);
  await page
    .locator("main")
    .getByRole("button", { name: "Search", exact: true })
    .click();
  const row = page.locator("main table tbody tr", { hasText: lastName });
  await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
  await row.locator("label.cds--radio-button__label").click();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.locator("#sampleId_0")).toBeVisible({
    timeout: UI_TIMEOUT,
  });
}

async function chooseSampleType(page: Page, index: number, typeId: string) {
  const loaded = page.waitForResponse((r) =>
    r.url().includes(`sample-type-tests?sampleType=${typeId}`),
  );
  await page.locator(`#sampleId_${index}`).selectOption(typeId);
  await loaded;
}

async function fillOrderStep(page: Page) {
  const siteListLoaded = page.waitForResponse(
    (r) =>
      /rest\/SamplePatientEntry(\?|$)/.test(r.url()) &&
      r.request().method() === "GET",
  );
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await siteListLoaded;
  await page.getByRole("link", { name: "Generate" }).click();
  await expect(
    page.getByRole("textbox", { name: "Lab Number", exact: true }),
  ).not.toHaveValue("", { timeout: UI_TIMEOUT });
  const site = page.getByRole("textbox", {
    name: "Search Site Name",
    exact: true,
  });
  await site.click();
  await site.pressSequentially("a");
  const suggestion = page.locator("ul.suggestions li").first();
  await expect(suggestion).toBeVisible({ timeout: UI_TIMEOUT });
  await suggestion.click();
}

test.describe("Add Order added samples (OGC-1406)", () => {
  test("a sample added after a blank Sample 1 is saved", async ({ page }) => {
    const lastName = await seedPatient(page);
    const { typeId, test: orderTest } = await sampleTypeWithTest(page);
    await openSampleStep(page, lastName);

    await page
      .getByRole("button", { name: /^Add Sample/ })
      .last()
      .click();
    await chooseSampleType(page, 1, typeId);
    await page.locator(`label[for="test_1_${orderTest.id}"]`).click();
    await fillOrderStep(page);

    await expect(page.getByTestId("order-sample-missing-tests")).toHaveCount(0);
    const submit = page.getByRole("button", { name: "Submit" });
    await expect(submit).toBeEnabled();
    await submit.click();
    await expect(page.getByText("Successfully saved")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
  });

  test("a sample with a type but no test blocks Submit and is named", async ({
    page,
  }) => {
    const lastName = await seedPatient(page);
    const { typeId, test: orderTest } = await sampleTypeWithTest(page);
    await openSampleStep(page, lastName);

    await chooseSampleType(page, 0, typeId);
    await page.locator(`label[for="test_0_${orderTest.id}"]`).click();
    await page
      .getByRole("button", { name: /^Add Sample/ })
      .last()
      .click();
    await chooseSampleType(page, 1, typeId);
    await fillOrderStep(page);

    await expect(page.getByTestId("order-sample-missing-tests")).toHaveText(
      /Sample 2 has a sample type but no test/,
    );
    await expect(page.getByRole("button", { name: "Submit" })).toBeDisabled();
  });
});
