import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import {
  seedMicrobiologyClassificationCase,
  type SeededMicrobiologyCase,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

type OrderCatalog = SeededMicrobiologyCase & {
  sampleTypeId: string;
  cultureTestId: string;
  tbCultureTestId: string;
  nonCultureTestId: string;
};

const cultureTestName = "UAT microbiology culture";
const tbCultureTestName = "UAT microbiology TB culture";
const nonCultureTestName = "UAT routine non-culture test";

async function seedOrderCatalog(page: Page): Promise<OrderCatalog> {
  const seeded = await seedMicrobiologyClassificationCase(page);
  const required = [
    "sampleTypeId",
    "cultureTestId",
    "tbCultureTestId",
    "nonCultureTestId",
  ] as const;
  for (const field of required) {
    if (!seeded[field]) {
      throw new Error(`Microbiology order-entry scenario is missing ${field}`);
    }
  }
  return seeded as OrderCatalog;
}

async function startSupportedOrder(page: Page, seeded: OrderCatalog) {
  await page.goto("/order/enter", { waitUntil: "domcontentloaded" });
  await expect(page.getByRole("heading", { name: "Enter Order" })).toBeVisible({
    timeout: LONG_TIMEOUT,
  });

  const labNumber = page.getByRole("textbox", { name: "Lab Number *" });
  const generateLabNumber = page.getByText("Generate Lab Number", {
    exact: true,
  });
  await expect(generateLabNumber).toBeVisible({ timeout: UI_TIMEOUT });
  await generateLabNumber.click();
  await expect(labNumber).not.toHaveValue("", { timeout: UI_TIMEOUT });

  const patientSearch = page.getByTestId("patient-search-section");
  await patientSearch.getByLabel("Last Name").fill("Microbiology");
  await patientSearch.getByLabel("First Name").fill("UAT");
  await patientSearch
    .getByRole("button", { name: "Search", exact: true })
    .click();
  const patientRow = patientSearch.getByTestId(
    `patient-search-result-${seeded.patientId}`,
  );
  await expect(patientRow).toBeVisible({ timeout: LONG_TIMEOUT });
  await patientRow.getByRole("button", { name: "Select" }).click();
  await expect(
    page.getByRole("heading", { name: "UAT Microbiology", exact: true }),
  ).toBeVisible({ timeout: UI_TIMEOUT });

  const sampleType = page
    .getByTestId("order-sample-test-section")
    .getByLabel("Sample Type");
  await expect(sampleType).toBeVisible({ timeout: LONG_TIMEOUT });
  await sampleType.selectOption(seeded.sampleTypeId);
  await expect(page.getByLabel(cultureTestName)).toBeVisible({
    timeout: LONG_TIMEOUT,
  });

  return await labNumber.inputValue();
}

async function clickTestToggle(page: Page, name: string) {
  const checkbox = page.getByLabel(name);
  await expect(checkbox).toBeVisible({ timeout: LONG_TIMEOUT });
  const checkboxId = await checkbox.getAttribute("id");
  if (!checkboxId) {
    throw new Error(`Test checkbox ${name} is missing its label target`);
  }
  await page.locator(`label[for="${checkboxId}"]`).click({
    timeout: UI_TIMEOUT,
  });
  return checkbox;
}

async function selectTest(page: Page, name: string) {
  const checkbox = page.getByLabel(name);
  if (!(await checkbox.isChecked())) {
    await clickTestToggle(page, name);
  }
  await expect(checkbox).toBeChecked({ timeout: UI_TIMEOUT });
}

async function fillMicrobiologyDetails(page: Page) {
  const details = page.getByTestId("microbiology-order-entry-section");
  await expect(details).toBeVisible({ timeout: LONG_TIMEOUT });
  await expect(page.getByRole("combobox", { name: "Program" })).toBeDisabled();
  await expect(
    page.getByText("Microbiology is derived from the selected culture test."),
  ).toBeVisible();
  await expect(
    page.getByRole("combobox", { name: "Culture Method" }),
  ).not.toHaveValue("");
  await page.getByLabel("Patient Origin").selectOption("INPATIENT");
  await page.getByRole("spinbutton", { name: "Number of Sets" }).fill("2");
  await page
    .getByLabel("Clinical History")
    .fill("Persistent fever after antibiotics");
  await page
    .locator('label[for="microbiology-order-entry-antibiotic-exposure"]')
    .click();
  await page
    .locator(
      'label[for="microbiology-order-entry-critical-notification-preference"]',
    )
    .click();
}

async function saveEntryAndOpenCollect(page: Page) {
  const saveAndNext = page.getByRole("button", { name: "Save & Next" });
  await expect(saveAndNext).toBeEnabled({ timeout: LONG_TIMEOUT });
  await saveAndNext.click();
  await expect(page).toHaveURL(/\/order\/collect$/i, {
    timeout: LONG_TIMEOUT,
  });
  await expect(
    page.getByRole("heading", { name: "Collect", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByTestId("sample-collection-card-0").getByLabel("Sample Type"),
  ).not.toHaveValue("", { timeout: LONG_TIMEOUT });
}

async function collectAndRoute(page: Page) {
  const saveAndNext = page.getByRole("button", { name: "Save & Next" });
  await expect(saveAndNext).toBeEnabled({ timeout: LONG_TIMEOUT });
  await saveAndNext.click();
  await expect(page).toHaveURL(/\/order\/label$/i, {
    timeout: LONG_TIMEOUT,
  });
}

async function reloadThroughBarcode(page: Page, labNumber: string) {
  await page.reload({ waitUntil: "domcontentloaded" });
  const barcode = page.getByRole("searchbox", { name: "Scan barcode" });
  await expect(barcode).toBeVisible({ timeout: LONG_TIMEOUT });
  await barcode.fill(labNumber);
  await barcode.press("Enter");
  await expect(page.getByTestId("order-context-card")).toContainText(
    labNumber,
    { timeout: LONG_TIMEOUT },
  );
  await page.getByTestId("order-step-enter").click();
  await expect(page).toHaveURL(/\/order\/enter$/i);
}

test.describe("microbiology order entry on the supported workflow", () => {
  test("persists culture details across collect reload and creates one case", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const seeded = await seedOrderCatalog(page);
    const labNumber = await startSupportedOrder(page, seeded);
    await selectTest(page, cultureTestName);
    await expect(
      page.getByTestId("microbiology-order-entry-section"),
    ).toContainText("Bacteriology");
    await fillMicrobiologyDetails(page);
    await saveEntryAndOpenCollect(page);

    await reloadThroughBarcode(page, labNumber);
    await expect(page.getByLabel("Patient Origin")).toHaveValue("INPATIENT");
    await expect(
      page.getByRole("spinbutton", { name: "Number of Sets" }),
    ).toHaveValue("2");
    await expect(page.getByLabel("Clinical History")).toHaveValue(
      "Persistent fever after antibiotics",
    );
    await expect(
      page.locator("#microbiology-order-entry-antibiotic-exposure"),
    ).toBeChecked();
    await expect(
      page.locator(
        "#microbiology-order-entry-critical-notification-preference",
      ),
    ).toBeChecked();

    await page.getByTestId("order-step-collect").click();
    await expect(page).toHaveURL(/\/order\/collect$/i);
    await collectAndRoute(page);

    await page.goto(
      `/Microbiology/worklist?q=${encodeURIComponent(labNumber)}`,
      { waitUntil: "domcontentloaded" },
    );
    const rows = page.locator('[data-testid^="microbiology-worklist-row-"]');
    await expect(rows).toHaveCount(1, { timeout: LONG_TIMEOUT });
    await expect(rows).toContainText(labNumber);
    await expect(rows).toContainText("Bacteriology");
  });

  test("keeps a non-culture order out of the microbiology worklist", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const seeded = await seedOrderCatalog(page);
    const labNumber = await startSupportedOrder(page, seeded);
    await selectTest(page, nonCultureTestName);
    await expect(
      page.getByTestId("microbiology-order-entry-section"),
    ).toHaveCount(0);
    await saveEntryAndOpenCollect(page);
    await collectAndRoute(page);

    await page.goto(
      `/Microbiology/worklist?q=${encodeURIComponent(labNumber)}`,
      { waitUntil: "domcontentloaded" },
    );
    await expect(page.getByText(/No cultures match/)).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
  });

  test("confirms before discarding details with the final culture test", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const seeded = await seedOrderCatalog(page);
    await startSupportedOrder(page, seeded);
    await selectTest(page, cultureTestName);
    await fillMicrobiologyDetails(page);

    await clickTestToggle(page, cultureTestName);
    const dialog = page.getByRole("dialog", {
      name: "Remove microbiology workflow?",
    });
    await expect(dialog).toBeVisible();
    await dialog.getByRole("button", { name: "Cancel" }).click();
    await expect(page.getByLabel(cultureTestName)).toBeChecked();
    await expect(page.getByLabel("Clinical History")).toHaveValue(
      "Persistent fever after antibiotics",
    );

    await clickTestToggle(page, cultureTestName);
    await dialog.getByRole("button", { name: /Discard details$/ }).click();
    await expect(
      page.getByTestId("microbiology-order-entry-section"),
    ).toHaveCount(0);
    await expect(page.getByRole("combobox", { name: "Program" })).toBeEnabled();
  });

  test("creates bacteriology and TB sibling cases for one specimen", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const seeded = await seedOrderCatalog(page);
    const labNumber = await startSupportedOrder(page, seeded);
    await selectTest(page, cultureTestName);
    await selectTest(page, tbCultureTestName);
    const details = page.getByTestId("microbiology-order-entry-section");
    await expect(details).toContainText("Bacteriology");
    await expect(details).toContainText("Mycobacteriology TB");
    await fillMicrobiologyDetails(page);
    await saveEntryAndOpenCollect(page);
    await collectAndRoute(page);

    await page.goto(
      `/Microbiology/worklist?q=${encodeURIComponent(labNumber)}`,
      { waitUntil: "domcontentloaded" },
    );
    const rows = page.locator('[data-testid^="microbiology-worklist-row-"]');
    await expect(rows).toHaveCount(2, { timeout: LONG_TIMEOUT });
    await expect(
      rows.filter({
        has: page.getByText("Bacteriology", { exact: true }),
      }),
    ).toHaveCount(1);
    await expect(
      rows.filter({
        has: page.getByText("Mycobacteriology TB", { exact: true }),
      }),
    ).toHaveCount(1);
  });
});
