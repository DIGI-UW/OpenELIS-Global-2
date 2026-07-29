import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import { seedMicrobiologyMvpCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

const next = (page: Page) =>
  page.getByRole("button", { name: "Next", exact: true });

async function selectSeededPatient(page: Page) {
  await page.goto("/SamplePatientEntry", { waitUntil: "domcontentloaded" });
  await expect(page.locator('[data-cy="searchPatientTabButton"]')).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await page.locator("input#lastName").fill("Microbiology");
  await page.locator("input#firstName").fill("UAT");
  await page
    .locator('[data-cy="searchPatientButton"], button#local_search')
    .click();

  const patientRadio = page.locator('[data-cy="radioButton"]').first();
  await expect(patientRadio).toBeVisible({ timeout: UI_TIMEOUT });
  await patientRadio.locator("xpath=..").locator("label").click();
  await expect(
    page
      .locator(
        '[data-cy="patientSelectionReady"], [data-cy="patientSelectionPending"]',
      )
      .first(),
  ).toBeVisible({ timeout: LONG_TIMEOUT });

  const birthDate = page.locator("input#date-picker-default-id");
  if ((await birthDate.isVisible()) && !(await birthDate.inputValue()).trim()) {
    await birthDate.fill("13/03/1990");
    await birthDate.press("Tab");
  }
}

async function openSampleStep(page: Page) {
  await expect(next(page)).toBeEnabled({ timeout: UI_TIMEOUT });
  await next(page).click();
  if (await next(page).isVisible()) {
    await expect(next(page)).toBeEnabled({ timeout: UI_TIMEOUT });
    await next(page).click();
  }
  await expect(page.locator("select#sampleId_0")).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
}

async function selectSampleAndTest(
  page: Page,
  sampleType: string,
  testName: string,
) {
  const sampleSelect = page.locator("select#sampleId_0");
  await expect(
    sampleSelect.locator("option").filter({ hasText: sampleType }),
  ).toHaveCount(1, { timeout: LONG_TIMEOUT });
  await sampleSelect.selectOption({ label: sampleType });

  const collectionDate = page.locator("input#collectionDate_0");
  if (await collectionDate.isVisible()) {
    await collectionDate.fill("28/07/2026");
    await collectionDate.press("Tab");
  }

  const testLabel = page.locator(`label:has-text("${testName}")`).first();
  await expect(testLabel).toBeVisible({ timeout: UI_TIMEOUT });
  await testLabel.click();
  await expect(next(page)).toBeEnabled({ timeout: UI_TIMEOUT });
}

test.describe("microbiology order entry", () => {
  test("shows culture details only for a culture-routed test", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    await seedMicrobiologyMvpCase(page);
    await selectSeededPatient(page);
    await openSampleStep(page);

    await selectSampleAndTest(
      page,
      "UAT micro specimen",
      "UAT microbiology culture",
    );
    await next(page).click();

    const microbiologyFields = page.getByTestId(
      "microbiology-order-entry-section",
    );
    await expect(microbiologyFields).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(microbiologyFields).toContainText("Bacteriology");
    await expect(page.getByLabel("Patient origin")).toBeVisible();
    await expect(page.getByLabel("Number of sets")).toBeVisible();
    await expect(page.getByLabel("Clinical history")).toBeVisible();
    await expect(page.getByLabel("Recent antibiotic exposure")).toBeVisible();
    await expect(
      page.getByLabel("Critical notification preference"),
    ).toBeVisible();

    await page.getByRole("button", { name: "Back", exact: true }).click();
    const cultureLabel = page
      .locator('label:has-text("UAT microbiology culture")')
      .first();
    await cultureLabel.click();

    const sampleSelect = page.locator("select#sampleId_0");
    const sampleOptions = await sampleSelect
      .locator("option")
      .allTextContents();
    const serum = sampleOptions.find((label) =>
      label.toLowerCase().includes("serum"),
    );
    expect(serum).toBeTruthy();
    await sampleSelect.selectOption({ label: serum!.trim() });

    const ordinaryTest = page
      .locator('label:has-text("Bilan Biochimique")')
      .first();
    await expect(ordinaryTest).toBeVisible({ timeout: UI_TIMEOUT });
    await ordinaryTest.click();
    await expect(next(page)).toBeEnabled({ timeout: UI_TIMEOUT });
    await next(page).click();

    await expect(microbiologyFields).toHaveCount(0);
  });
});
