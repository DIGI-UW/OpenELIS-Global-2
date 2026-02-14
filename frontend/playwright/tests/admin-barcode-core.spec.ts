import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin barcode configuration parity migration", () => {
  test("opens barcode configuration and validates core controls", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-001",
      riskTier: "P0",
      domain: "admin",
      notes: "M4b barcode configuration core parity smoke",
    });

    await gotoAndWait("/MasterListsPage");
    await ensureAuthenticatedShell();

    await page.locator('[data-cy="barcodeConfig"]').click();
    await expect(page).toHaveURL(/\/MasterListsPage\/barcodeConfiguration/);
    await expect(page.locator("#order")).toBeVisible();
    await expect(page.locator("#specimen")).toBeVisible();
    await expect(page.locator("#maxOrder")).toBeVisible();
    await expect(page.locator("#maxSpecimen")).toBeVisible();
    await expect(page.getByRole("button", { name: "Save" })).toBeVisible();
  });
});
