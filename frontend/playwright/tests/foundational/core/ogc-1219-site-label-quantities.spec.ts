import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { csrfToken } from "../../../helpers/api-session";

/**
 * OGC-1219 — the system label presets are seeded from the site's real
 * site_information keys, and the quantity they carry reaches an order entered
 * through Add Order even when nobody touches the labels section.
 *
 * Every database carries numDefaultOrderLabels=2 from the 2.5.x.x seed, so the
 * Order Label preset must say 2 wherever the seed (or its repair) read the real
 * key; the old changeset read a namespace that never existed and left 1.
 */

const API = "/api/OpenELIS-Global";
const SEEDED_ORDER_LABEL_DEFAULT = 2;

const letters = (length: number) =>
  Array.from({ length }, () =>
    String.fromCharCode(65 + Math.floor(Math.random() * 26)),
  ).join("");

async function seedPatient(page: Page): Promise<string> {
  const lastName = `OSLQ${letters(8)}`;
  const response = await page.request.post(`${API}/rest/PatientManagement`, {
    data: {
      patientPK: "",
      lastName,
      firstName: "Labels",
      gender: "F",
      birthDateForDisplay: "01/02/1990",
      nationalId: `S9${letters(8)}`,
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
    const found = (catalog.tests || []).find((t) => t.name);
    if (found) return { typeId: String(type.id), test: found };
  }
  throw new Error("No sample type with a test");
}

async function orderLabelPreset(page: Page) {
  const presets = await (
    await page.request.get(`${API}/api/labelPresets`)
  ).json();
  const list = Array.isArray(presets) ? presets : presets.content || [];
  const preset = list.find((p) => p.isSystem && p.printsPerOrder);
  expect(preset, "a system preset printed per order").toBeTruthy();
  return preset;
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

async function chooseSampleType(page: Page, typeId: string) {
  const loaded = page.waitForResponse((r) =>
    r.url().includes(`sample-type-tests?sampleType=${typeId}`),
  );
  await page.locator("#sampleId_0").selectOption(typeId);
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
  const labNo = page.getByRole("textbox", { name: "Lab Number", exact: true });
  await expect(labNo).not.toHaveValue("", { timeout: UI_TIMEOUT });
  const site = page.getByRole("textbox", {
    name: "Search Site Name",
    exact: true,
  });
  await site.click();
  await site.pressSequentially("a");
  const suggestion = page.locator("ul.suggestions li").first();
  await expect(suggestion).toBeVisible({ timeout: UI_TIMEOUT });
  await suggestion.click();
  return labNo.inputValue();
}

test.describe("Site label quantities reach the order (OGC-1219)", () => {
  test("the seeded Order Label default is proposed and stored without touching the labels section", async ({
    page,
  }) => {
    const preset = await orderLabelPreset(page);
    expect(preset.defaultPerOrder, "seeded from numDefaultOrderLabels").toBe(
      SEEDED_ORDER_LABEL_DEFAULT,
    );

    const lastName = await seedPatient(page);
    const { typeId, test: orderTest } = await sampleTypeWithTest(page);
    await openSampleStep(page, lastName);
    await chooseSampleType(page, typeId);
    await page.locator(`label[for="test_0_${orderTest.id}"]`).click();
    const accessionNumber = await fillOrderStep(page);

    await test.step("the labels section proposes the preset's default", async () => {
      const section = page.getByTestId("labels-section-root");
      await expect(section).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(
        section.locator(`#order-label-order-${preset.id}`),
      ).toHaveValue(String(preset.defaultPerOrder));
    });

    await test.step("an untouched section still saves that quantity", async () => {
      const saved = page.waitForResponse(
        (r) =>
          /rest\/SamplePatientEntry$/.test(r.url()) &&
          r.request().method() === "POST",
      );
      await page.getByRole("button", { name: "Submit" }).click();
      await saved;
      await expect(page.getByText("Successfully saved")).toBeVisible({
        timeout: NAV_TIMEOUT,
      });

      const stored = await (
        await page.request.get(
          `${API}/api/orders/by-accession/${encodeURIComponent(accessionNumber)}/labels`,
        )
      ).json();
      const orderRow = (Array.isArray(stored) ? stored : []).find(
        (r) => String(r.presetId ?? r.preset_id) === String(preset.id),
      );
      expect(orderRow, "an order-label request row").toBeTruthy();
      expect(orderRow.qty).toBe(preset.defaultPerOrder);
    });
  });
});
