import { test, expect, Page, Route } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { csrfToken } from "../../../helpers/api-session";

/**
 * OGC-1389 — one failed or slow request on the sample step no longer replaces
 * the order screen. A failed sample-type test list (server error, expired
 * session answered with the login page, dropped connection) keeps what was
 * entered and offers Retry; failed referral lists disable the referral; Modify
 * Order shows the same message instead of a blank page.
 */

const API = "/api/OpenELIS-Global";
const TESTS_FOR_TYPE = /\/rest\/sample-type-tests\?sampleType=/;

const letters = (length: number) =>
  Array.from({ length }, () =>
    String.fromCharCode(65 + Math.floor(Math.random() * 26)),
  ).join("");

async function seedPatient(page: Page): Promise<string> {
  const lastName = `OTEN${letters(8)}`;
  const response = await page.request.post(`${API}/rest/PatientManagement`, {
    data: {
      patientPK: "",
      lastName,
      firstName: "Resilient",
      gender: "F",
      birthDateForDisplay: "01/02/1990",
      nationalId: `R9${letters(8)}`,
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

async function firstSampleType(page: Page) {
  const types = await (
    await page.request.get(`${API}/rest/user-sample-types`)
  ).json();
  for (const type of types) {
    const catalog = await (
      await page.request.get(
        `${API}/rest/sample-type-tests?sampleType=${type.id}`,
      )
    ).json();
    const orderable = (catalog.tests || []).find((t) => t.name);
    if (orderable) {
      return { id: String(type.id), name: type.value as string, orderable };
    }
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

const failures: Record<string, (route: Route) => Promise<void>> = {
  "a server error": (route) =>
    route.fulfill({
      status: 500,
      contentType: "application/json",
      body: '{"error":"boom"}',
    }),
  "an expired session answered with the login page": (route) =>
    route.fulfill({
      status: 200,
      contentType: "text/html",
      body: "<html><body>Login</body></html>",
    }),
  "a dropped connection": (route) => route.abort(),
};

test.describe("Sample step failed requests (OGC-1389)", () => {
  for (const [kind, fail] of Object.entries(failures)) {
    test(`${kind} on the test list keeps the order and offers Retry`, async ({
      page,
    }) => {
      const lastName = await seedPatient(page);
      const sampleType = await firstSampleType(page);
      await openSampleStep(page, lastName);

      await page.route(TESTS_FOR_TYPE, fail);
      await page.locator("#sampleId_0").selectOption(sampleType.id);

      await expect(
        page.getByText(`Tests for ${sampleType.name} could not be loaded.`),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(
        page.getByText("Sample entry could not be loaded"),
      ).toHaveCount(0);
      await expect(page.locator("#sampleId_0")).toHaveValue(sampleType.id);

      await page.unroute(TESTS_FOR_TYPE);
      await page.getByRole("button", { name: "Retry" }).click();
      await expect(
        page.locator(`label[for="test_0_${sampleType.orderable.id}"]`),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await page.getByRole("button", { name: "Back", exact: true }).click();
      await page.getByRole("button", { name: "Back", exact: true }).click();
      await expect(
        page.getByRole("textbox", { name: "Last Name" }).first(),
      ).toHaveValue(lastName);
    });
  }

  test("failed referral lists disable the referral instead of crashing", async ({
    page,
  }) => {
    const lastName = await seedPatient(page);
    const sampleType = await firstSampleType(page);
    await page.route(
      /\/rest\/displayList\/REFERRAL_(REASONS|ORGANIZATIONS)/,
      (route) => route.fulfill({ status: 500, body: "" }),
    );
    await openSampleStep(page, lastName);
    await page.locator("#sampleId_0").selectOption(sampleType.id);
    await page
      .locator(`label[for="test_0_${sampleType.orderable.id}"]`)
      .click();

    await expect(page.locator("#useReferral_0")).toBeDisabled();
    await expect(
      page.getByText(
        "The referral reasons or laboratories could not be loaded. Reload the page to refer a test.",
      ),
    ).toBeVisible();
    await expect(
      page.getByText("Sample entry could not be loaded"),
    ).toHaveCount(0);
  });

  test("Modify Order shows the failure on the sample instead of a blank page", async ({
    page,
  }) => {
    const sampleType = await firstSampleType(page);
    const labNumber = "DEV01260000000013890";
    await page.route("**/rest/order/search?labNumber=*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ labNumber, sampleOrderItems: {} }),
      }),
    );
    await page.route("**/rest/SampleEdit?*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          accessionNumber: labNumber,
          sampleOrderItems: {
            labNo: labNumber,
            referringSiteName: "Site",
            referringSiteId: "1",
            providerLastName: "Doe",
            providerFirstName: "Jane",
          },
          existingTests: [],
          possibleTests: [],
        }),
      }),
    );
    await page.goto(`/ModifyOrder?accessionNumber=${labNumber}`, {
      timeout: NAV_TIMEOUT,
    });
    await page
      .getByRole("button", { name: /^Add Sample/ })
      .first()
      .click();
    await expect(page.locator("#sampleId_0")).toBeVisible({
      timeout: UI_TIMEOUT,
    });

    await page.route(TESTS_FOR_TYPE, (route) =>
      route.fulfill({ status: 500, body: "" }),
    );
    await page.locator("#sampleId_0").selectOption(sampleType.id);

    await expect(
      page.getByText(`Tests for ${sampleType.name} could not be loaded.`),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(page.getByRole("button", { name: "Retry" })).toBeVisible();
  });
});
