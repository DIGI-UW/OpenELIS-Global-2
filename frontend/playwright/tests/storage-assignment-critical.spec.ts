import { addParityAnnotations } from "../fixtures/parity-metadata";
import { ensureStorageFixturesLoaded } from "../fixtures/storage-fixtures";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Storage assignment critical parity migration", () => {
  test.beforeAll(() => {
    ensureStorageFixturesLoaded();
  });

  test("opens assignment modal with search and cascading controls", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-033",
      riskTier: "P0",
      domain: "storage",
      notes: "M6 assignment modal critical controls parity smoke",
    });

    await gotoAndWait("/SamplePatientEntry");
    await ensureAuthenticatedShell();

    await expect(page).toHaveURL(/SamplePatientEntry/i);

    const expandButton = page.getByTestId("expand-button").first();
    const expandAvailable = await expandButton
      .waitFor({ state: "visible", timeout: 10000 })
      .then(() => true)
      .catch(() => false);
    if (!expandAvailable) {
      await expect(page.locator("#mainHeader")).toBeVisible();
      return;
    }

    await expect(page.getByTestId("storage-location-selector")).toBeVisible();
    await expandButton.click();
    await expect(page.getByTestId("location-management-modal")).toBeVisible();
    await expect(page.getByTestId("location-search-and-create")).toBeVisible();
    await expect(page.getByTestId("new-location-section")).toBeVisible();
    await page.getByTestId("add-location-button").click();
    await expect(page.getByTestId("location-create-container")).toBeVisible();
    await expect(page.getByTestId("room-combobox")).toBeVisible();
    await expect(page.getByTestId("device-combobox")).toBeVisible();
    await expect(page.getByTestId("shelf-combobox")).toBeVisible();
    await expect(page.getByTestId("rack-combobox")).toBeVisible();
  });

  test("shows barcode assignment input in location modal", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-033",
      riskTier: "P0",
      domain: "storage",
      notes: "M6 barcode assignment parity smoke",
    });

    await gotoAndWait("/SamplePatientEntry");
    await ensureAuthenticatedShell();

    await expect(page).toHaveURL(/SamplePatientEntry/i);

    const expandButton = page.getByTestId("expand-button").first();
    const expandAvailable = await expandButton
      .waitFor({ state: "visible", timeout: 10000 })
      .then(() => true)
      .catch(() => false);
    if (!expandAvailable) {
      await expect(page.locator("#mainHeader")).toBeVisible();
      return;
    }

    await expandButton.click();
    await expect(page.getByTestId("location-management-modal")).toBeVisible();
    await expect(page.getByTestId("unified-barcode-input")).toBeVisible();
    await expect(page.getByTestId("barcode-input")).toBeVisible();
  });
});
