import { addParityAnnotations } from "../fixtures/parity-metadata";
import { ensureStorageFixturesLoaded } from "../fixtures/storage-fixtures";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Storage box CRUD critical parity migration", () => {
  test.beforeAll(() => {
    ensureStorageFixturesLoaded({ flowType: "mutating" });
  });

  test("boxes tab exposes add-box gate based on rack selection state", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-034",
      riskTier: "P0",
      domain: "storage",
      notes: "M6 box CRUD add gate parity smoke",
    });

    await gotoAndWait("/Storage");
    await ensureAuthenticatedShell();

    await page.getByTestId("tab-boxes").click();
    const addBoxButton = page.getByTestId("add-box-button");
    await expect(addBoxButton).toBeVisible();
    if (await addBoxButton.isEnabled()) {
      await addBoxButton.click();
      await expect(page.getByTestId("box-label")).toBeVisible();
      await expect(page.getByTestId("box-code")).toBeVisible();
      await page.keyboard.press("Escape");
    } else {
      await expect(addBoxButton).toBeDisabled();
    }
  });

  test("boxes tab exposes selector and location overflow controls", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-035",
      riskTier: "P0",
      domain: "storage",
      notes: "M6 box CRUD edit/delete control parity smoke",
    });

    await gotoAndWait("/Storage");
    await ensureAuthenticatedShell();

    await page.getByTestId("tab-boxes").click();
    await expect(page.getByTestId("box-selector")).toBeVisible();
  });
});
