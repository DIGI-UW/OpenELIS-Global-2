import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";
import { csrfToken } from "../../../helpers/api-session";

/**
 * OGC-1266 (Clinical Order Entry v4, slice M1) — the fix-now bundle, driven
 * through the real clinical order screens.
 *
 * The patient search on Enter Order is the shared search form; a test ordered
 * on its own reloads without the panel it belongs to; the Collect step offers
 * only the sample types the catalog maps to the test; saving the sample marks
 * the requested sample type collected with no phantom label row; and a double
 * click on Save sends one write.
 */

const API = "/api/OpenELIS-Global";
const CLINICAL_ENTER = "/order/clinical/enter";

const letters = (length: number) =>
  Array.from({ length }, () =>
    String.fromCharCode(65 + Math.floor(Math.random() * 26)),
  ).join("");

async function seedPatient(page: Page): Promise<string> {
  const lastName = `MONE${letters(8)}`;
  const response = await page.request.post(`${API}/rest/PatientManagement`, {
    data: {
      patientPK: "",
      lastName,
      firstName: "Order",
      gender: "F",
      birthDateForDisplay: "01/02/1990",
      nationalId: `M1${letters(8)}`,
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

/** A sample type with a panel, and one of that panel's members to order alone. */
async function panelMemberTest(page: Page) {
  const types = await (
    await page.request.get(`${API}/rest/user-sample-types`)
  ).json();
  for (const type of types) {
    const catalog = await (
      await page.request.get(
        `${API}/rest/sample-type-tests?sampleType=${type.id}`,
      )
    ).json();
    const testIds = new Set((catalog.tests || []).map((t) => t.id));
    for (const panel of catalog.panels || []) {
      const member = (panel.testIds || "")
        .split(",")
        .find((id: string) => testIds.has(id));
      if (member) {
        return { sampleTypeId: type.id, testId: member, panelId: panel.id };
      }
    }
  }
  throw new Error("No sample type offers a panel with an orderable member");
}

async function openEnterOrder(page: Page) {
  await page.goto(CLINICAL_ENTER, { waitUntil: "domcontentloaded" });
  await expect(page.locator("#labNumber")).not.toHaveValue("", {
    timeout: NAV_TIMEOUT,
  });
}

async function selectPatient(page: Page, lastName: string) {
  const search = page.getByTestId("order-patient-search");
  await search.getByRole("textbox", { name: "Last Name" }).fill(lastName);
  await search.getByRole("button", { name: "Search", exact: true }).click();
  const row = search.locator('[data-cy^="patient-result-row-"]').first();
  await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
  await row.locator("label").first().click();
  await expect(
    page.getByRole("heading", { name: `Order ${lastName}` }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
}

test.describe("OGC-1266 order entry fix-now bundle", () => {
  test("Enter Order searches with the shared patient search form", async ({
    page,
  }) => {
    const lastName = await seedPatient(page);
    await openEnterOrder(page);

    await selectPatient(page, lastName);
    await expect(page.getByTestId("order-patient-search")).toBeHidden();

    await page.locator(".selected-entity-card").getByText("Clear").click();
    const search = page.getByTestId("order-patient-search");
    await expect(search).toBeVisible();
    await expect(
      search.locator('[data-cy^="patient-result-row-"]').first(),
    ).toBeVisible();
    await expect(page.locator(".selected-entity-card")).toHaveCount(0);
  });

  test("a test ordered on its own is collected, fulfilled and reloaded without its panel", async ({
    page,
  }) => {
    const lastName = await seedPatient(page);
    const { sampleTypeId, testId } = await panelMemberTest(page);
    const mapped = await (
      await page.request.get(`${API}/rest/test-sample-types?testIds=${testId}`)
    ).json();
    const mappedNames = mapped.tests[0].compatibleSampleTypes.map(
      (type) => `+ ${type.name}`,
    );
    expect(mappedNames.length).toBeGreaterThan(0);

    await openEnterOrder(page);
    const labNumber = await page.locator("#labNumber").inputValue();
    await selectPatient(page, lastName);
    const samples = page.getByTestId("order-sample-test-section");
    await samples.getByLabel("Sample Type").first().selectOption(sampleTypeId);
    await samples.locator(`label[for="test-0-${testId}"]`).click();
    await expect(samples.locator(`#test-0-${testId}`)).toBeChecked();
    await page.getByRole("button", { name: "Save & Next" }).click();
    await expect(page).toHaveURL(/\/order\/clinical\/collect/, {
      timeout: NAV_TIMEOUT,
    });

    const offered = page
      .locator(".requested-tests-section tbody tr")
      .first()
      .locator(".sample-type-tag");
    await expect(offered).toHaveCount(mappedNames.length, {
      timeout: NAV_TIMEOUT,
    });
    await expect(offered).toHaveText(mappedNames);

    const saves: string[] = [];
    page.on("request", (request) => {
      if (
        request.method() === "POST" &&
        request.url().includes("/rest/SamplePatientEntry")
      ) {
        saves.push(request.url());
      }
    });
    const saved = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/SamplePatientEntry") &&
        response.request().method() === "POST",
    );
    await page.getByRole("button", { name: "Save", exact: true }).dblclick();
    await saved;
    await expect(page.getByRole("button", { name: "Save & Next" })).toBeEnabled(
      { timeout: NAV_TIMEOUT },
    );
    expect(saves).toHaveLength(1);

    const order = await (
      await page.request.get(
        `${API}/rest/order/search?labNumber=${encodeURIComponent(labNumber)}`,
      )
    ).json();
    expect(order.samples).toHaveLength(1);
    expect(order.samples[0].tests.map((t) => t.id)).toEqual([testId]);
    expect(order.samples[0].panels).toEqual([]);
    const requests = await (
      await page.request.get(
        `${API}/rest/sample-type-requests/sample/${order.id}`,
      )
    ).json();
    expect(requests.map((r) => r.status)).toEqual(["COLLECTED"]);

    await page.getByRole("button", { name: "Save & Next" }).click();
    await expect(page).toHaveURL(/\/order\/clinical\/label/, {
      timeout: NAV_TIMEOUT,
    });
    await expect(page.locator("main")).toContainText(`${labNumber}.1`, {
      timeout: NAV_TIMEOUT,
    });
    await expect(page.locator("main")).not.toContainText(`${labNumber}-2`);
  });
});
