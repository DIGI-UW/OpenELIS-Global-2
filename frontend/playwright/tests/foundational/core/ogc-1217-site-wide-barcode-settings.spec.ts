import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { csrfToken } from "../../../helpers/api-session";

/**
 * OGC-1217 — the site-wide pre-printed accession setting is visible and
 * editable again, as a card above the preset list on the Label Presets page.
 * The backend (SiteWideBarcodeSettingsRestController) shipped with OGC-285 and
 * no screen rendered it, so whatever a site had stored was frozen.
 *
 * The walk saves the separate pre-printed series with a prefix, proves the
 * choice is site-wide by taking the next number of that series, printing it
 * and attaching it to a new order through Add Order, and checks that the same
 * format is refused once the setting is back on the order entry pool. The
 * stored setting is restored at the end.
 */

const API = "/api/OpenELIS-Global";
const SETTINGS = `${API}/api/siteSettings/barcode`;
const LIST = "/MasterListsPage/labelPresets";
const ORDER_ENTRY_LABEL = 'label[for="preprint-source-order-entry"]';
const SEPARATE_LABEL = 'label[for="preprint-source-separate"]';

interface Settings {
  prePrintUseAltAccession: boolean;
  prePrintAltAccessionPrefix?: string;
}

// A fresh 4-character prefix per run: the series starts at its first number
// and never collides with a prefix a site may already use.
const PREFIX = `P${Math.floor(Math.random() * 900 + 100)}`;

const letters = (length: number) =>
  Array.from({ length }, () =>
    String.fromCharCode(65 + Math.floor(Math.random() * 26)),
  ).join("");

async function readSettings(page: Page): Promise<Settings> {
  const response = await page.request.get(SETTINGS);
  expect(response.status()).toBe(200);
  return (await response.json()) as Settings;
}

async function restoreSettings(page: Page, settings: Settings) {
  const response = await page.request.post(SETTINGS, {
    headers: {
      "X-CSRF-Token": await csrfToken(page),
      "Content-Type": "application/json",
    },
    data: {
      prePrintUseAltAccession: Boolean(settings.prePrintUseAltAccession),
      prePrintAltAccessionPrefix: settings.prePrintAltAccessionPrefix ?? "",
    },
  });
  // A site whose stored prefix predates the 4-character rule cannot be
  // written back through the validated endpoint; its row is untouched then.
  expect([200, 422]).toContain(response.status());
}

/** The next number of the separate pre-printed series, without reserving it. */
async function nextPrePrintedNumber(page: Page): Promise<string> {
  const response = await page.request.get(
    `${API}/rest/SampleEntryGenerateScanProvider?format=ALT_YEAR&noIncrement=true`,
  );
  expect(response.status()).toBe(200);
  return String((await response.json()).body || "");
}

/** The number printed just before `next` in a prefix + year + 13-digit series. */
function previousNumber(next: string): string {
  const head = next.slice(0, -13);
  const tail = String(Number(next.slice(-13)) - 1).padStart(13, "0");
  return head + tail;
}

/** What order entry answers when this lab number is typed or scanned. */
async function labNumberAccepted(
  page: Page,
  labNumber: string,
): Promise<boolean> {
  const response = await page.request.get(
    `${API}/rest/SampleEntryAccessionNumberValidation?ignoreYear=false&ignoreUsage=false&field=labNo&accessionNumber=${encodeURIComponent(labNumber)}`,
  );
  expect(response.status()).toBe(200);
  return (await response.json()).status === true;
}

async function seedPatient(page: Page): Promise<string> {
  const lastName = `OSWB${letters(8)}`;
  const response = await page.request.post(`${API}/rest/PatientManagement`, {
    data: {
      patientPK: "",
      lastName,
      firstName: "Preprinted",
      gender: "F",
      birthDateForDisplay: "01/02/1990",
      nationalId: `S7${letters(8)}`,
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

/** The order step with a lab number typed in, as a scanner would enter it. */
async function fillOrderStepWithLabNumber(page: Page, labNumber: string) {
  const siteListLoaded = page.waitForResponse(
    (r) =>
      /rest\/SamplePatientEntry(\?|$)/.test(r.url()) &&
      r.request().method() === "GET",
  );
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await siteListLoaded;
  const labNo = page.getByRole("textbox", { name: "Lab Number *" });
  const validated = page.waitForResponse((r) =>
    r.url().includes("SampleEntryAccessionNumberValidation"),
  );
  await labNo.fill(labNumber);
  await validated;
  await expect(labNo).toHaveValue(labNumber);
  const site = page.getByRole("textbox", { name: "Search Site Name *" });
  await site.click();
  await site.pressSequentially("a");
  const suggestion = page.locator("ul.suggestions li").first();
  await expect(suggestion).toBeVisible({ timeout: UI_TIMEOUT });
  await suggestion.click();
}

test.describe("Site-wide barcode settings on Label Presets (OGC-1217)", () => {
  test("the card shows the stored choice, validates the prefix, and the pre-printed series is accepted at order entry only while it is on", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const original = await readSettings(page);

    try {
      await page.goto(LIST, { waitUntil: "domcontentloaded" });
      const card = page.getByTestId("site-wide-barcode-settings");
      const saveButton = card.getByRole("button", { name: "Save settings" });
      const prefix = card.getByLabel("Prefix", { exact: true });
      const separate = card.locator("#preprint-source-separate");
      const orderEntry = card.locator("#preprint-source-order-entry");

      await test.step("the card renders above the preset list with the stored choice", async () => {
        await expect(card).toBeVisible({ timeout: UI_TIMEOUT });
        await expect(
          card.getByText("Site-wide Barcode Settings"),
        ).toBeVisible();
        await expect(card.getByText("Applies laboratory-wide")).toBeVisible();
        await expect(
          card.getByText("Pre-printed label numbers come from"),
        ).toBeVisible();
        const table = page.getByRole("table");
        await expect(table).toBeVisible({ timeout: UI_TIMEOUT });
        const cardBox = await card.boundingBox();
        const tableBox = await table.boundingBox();
        expect(cardBox && tableBox && cardBox.y < tableBox.y).toBe(true);

        if (original.prePrintUseAltAccession) {
          await expect(separate).toBeChecked();
          await expect(prefix).toBeEnabled();
          await expect(prefix).toHaveValue(
            original.prePrintAltAccessionPrefix ?? "",
          );
        } else {
          await expect(orderEntry).toBeChecked();
          await expect(prefix).toBeDisabled();
        }
        await expect(saveButton).toBeDisabled();
      });

      await test.step("a short prefix is refused on the page without a request", async () => {
        await card.locator(SEPARATE_LABEL).click();
        await expect(separate).toBeChecked();
        await expect(prefix).toBeEnabled();
        await prefix.fill("ab");
        await expect(prefix).toHaveValue("AB");
        let posted = false;
        page.on("request", (request) => {
          if (
            request.method() === "POST" &&
            request.url().includes("siteSettings/barcode")
          ) {
            posted = true;
          }
        });
        await saveButton.click();
        await expect(
          card.getByText("The prefix must be exactly 4 letters or digits"),
        ).toBeVisible({ timeout: UI_TIMEOUT });
        expect(posted).toBe(false);
      });

      await test.step("a 4-character prefix saves, confirms, and survives a reload", async () => {
        await prefix.fill(PREFIX.toLowerCase());
        await expect(prefix).toHaveValue(PREFIX);
        const saved = page.waitForResponse(
          (r) =>
            r.url().includes("siteSettings/barcode") &&
            r.request().method() === "POST",
        );
        await saveButton.click();
        expect((await saved).status()).toBe(200);
        await expect(
          page.getByText("Site-wide barcode settings saved"),
        ).toBeVisible({ timeout: UI_TIMEOUT });
        expect(await readSettings(page)).toEqual({
          prePrintUseAltAccession: true,
          prePrintAltAccessionPrefix: PREFIX,
        });

        await page.reload({ waitUntil: "domcontentloaded" });
        await expect(separate).toBeChecked({ timeout: UI_TIMEOUT });
        await expect(prefix).toBeEnabled();
        await expect(prefix).toHaveValue(PREFIX);
        await expect(saveButton).toBeDisabled();
      });

      let prePrinted = "";
      await test.step("pre-printed labels now draw from the separate series", async () => {
        const preview = await nextPrePrintedNumber(page);
        expect(preview).toMatch(new RegExp(`^${PREFIX}\\d{15}$`));
        const { test: printedTest } = await sampleTypeWithTest(page);
        const pdf = await page.request.get(
          `${API}/LabelMakerServlet?prePrinting=true&numSetsOfLabels=1&numOrderLabelsPerSet=1&numSpecimenLabelsPerSet=1&facilityName=&testIds=${printedTest.id}`,
        );
        expect(pdf.status()).toBe(200);
        expect(pdf.headers()["content-type"]).toContain("application/pdf");
        // Printing one set reserves one number of the series; the number on
        // the label is the one just before the next available one.
        const next = await nextPrePrintedNumber(page);
        expect(next).toMatch(new RegExp(`^${PREFIX}\\d{15}$`));
        expect(next, "printing advanced the series").not.toBe(preview);
        prePrinted = previousNumber(next);
        expect(
          await labNumberAccepted(page, prePrinted),
          "order entry accepts the printed number while the series is on",
        ).toBe(true);
      });

      await test.step("the pre-printed number is attached to a new order through Add Order", async () => {
        const lastName = await seedPatient(page);
        const { typeId, test: orderTest } = await sampleTypeWithTest(page);
        await openSampleStep(page, lastName);
        await chooseSampleType(page, typeId);
        await page.locator(`label[for="test_0_${orderTest.id}"]`).click();
        await fillOrderStepWithLabNumber(page, prePrinted);
        const saved = page.waitForResponse(
          (r) =>
            /rest\/SamplePatientEntry$/.test(r.url()) &&
            r.request().method() === "POST",
        );
        await page.getByRole("button", { name: "Submit" }).click();
        expect((await saved).status()).toBe(200);
        await expect(page.getByText("Successfully saved")).toBeVisible({
          timeout: NAV_TIMEOUT,
        });
        const order = await (
          await page.request.get(
            `${API}/rest/order/search?labNumber=${encodeURIComponent(prePrinted)}`,
          )
        ).json();
        expect(order.labNumber).toBe(prePrinted);
        expect(order.id).toBeTruthy();
      });

      await test.step("back on the order entry pool the pre-printed format is refused", async () => {
        await page.goto(LIST, { waitUntil: "domcontentloaded" });
        await expect(separate).toBeChecked({ timeout: UI_TIMEOUT });
        await card.locator(ORDER_ENTRY_LABEL).click();
        await expect(orderEntry).toBeChecked();
        await expect(prefix).toBeDisabled();
        await expect(prefix).toHaveValue(PREFIX);
        const saved = page.waitForResponse(
          (r) =>
            r.url().includes("siteSettings/barcode") &&
            r.request().method() === "POST",
        );
        await saveButton.click();
        expect((await saved).status()).toBe(200);
        await expect(
          page.getByText("Site-wide barcode settings saved"),
        ).toBeVisible({ timeout: UI_TIMEOUT });
        expect(await readSettings(page)).toEqual({
          prePrintUseAltAccession: false,
          prePrintAltAccessionPrefix: PREFIX,
        });

        const unused =
          prePrinted.slice(0, -1) + ((+prePrinted.slice(-1) + 1) % 10);
        expect(
          await labNumberAccepted(page, unused),
          "the separate series is no longer a valid lab number format",
        ).toBe(false);
      });
    } finally {
      await restoreSettings(page, original);
    }
  });
});
