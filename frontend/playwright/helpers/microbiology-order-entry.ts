import type { Page } from "@playwright/test";
import { expect } from "@playwright/test";
import {
  seedMicrobiologyClassificationCase,
  type SeededMicrobiologyCase,
} from "./seed-microbiology-data";
import { LONG_TIMEOUT, UI_TIMEOUT } from "./timeouts";

export type MicrobiologyOrderCatalog = SeededMicrobiologyCase & {
  sampleTypeId: string;
  cultureTestId: string;
  tbCultureTestId: string;
  nonCultureTestId: string;
  alternateMethodId: string;
};

export const MICROBIOLOGY_CULTURE_TEST_NAME = "UAT microbiology culture";
export const MICROBIOLOGY_CULTURE_METHOD_NAME = "UAT micro culture";
export const MICROBIOLOGY_ALTERNATE_CULTURE_METHOD_NAME = "UAT alt culture";
export const MICROBIOLOGY_TB_CULTURE_TEST_NAME = "UAT microbiology TB culture";
export const MICROBIOLOGY_NON_CULTURE_TEST_NAME =
  "UAT routine non-culture test";

export async function seedMicrobiologyOrderCatalog(
  page: Page,
): Promise<MicrobiologyOrderCatalog> {
  const seeded = await seedMicrobiologyClassificationCase(page);
  const required = [
    "sampleTypeId",
    "cultureTestId",
    "tbCultureTestId",
    "nonCultureTestId",
    "alternateMethodId",
  ] as const;
  for (const field of required) {
    if (!seeded[field]) {
      throw new Error(`Microbiology order-entry scenario is missing ${field}`);
    }
  }
  return seeded as MicrobiologyOrderCatalog;
}

export async function startMicrobiologyOrder(
  page: Page,
  seeded: MicrobiologyOrderCatalog,
) {
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
  if (!seeded.patientExternalId) {
    throw new Error(
      "Microbiology order-entry scenario is missing patientExternalId",
    );
  }
  await patientSearch.getByLabel("Patient Id").fill(seeded.patientExternalId);
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
  await expect(page.getByLabel(MICROBIOLOGY_CULTURE_TEST_NAME)).toBeVisible({
    timeout: LONG_TIMEOUT,
  });

  return labNumber.inputValue();
}

export async function clickMicrobiologyOrderTest(page: Page, name: string) {
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

export async function selectMicrobiologyOrderTest(page: Page, name: string) {
  const checkbox = page.getByLabel(name);
  if (!(await checkbox.isChecked())) {
    await clickMicrobiologyOrderTest(page, name);
  }
  await expect(checkbox).toBeChecked({ timeout: UI_TIMEOUT });
}
