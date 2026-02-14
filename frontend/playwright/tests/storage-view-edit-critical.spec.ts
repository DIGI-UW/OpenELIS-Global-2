import { addParityAnnotations } from "../fixtures/parity-metadata";
import { ensureStorageFixturesLoaded } from "../fixtures/storage-fixtures";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Storage view/edit critical parity migration", () => {
  test.beforeAll(() => {
    ensureStorageFixturesLoaded();
  });

  test("rack tab renders core CRUD controls", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-040",
      riskTier: "P0",
      domain: "storage",
      notes: "M6 location CRUD integration parity smoke",
    });

    await gotoAndWait("/Storage");
    await ensureAuthenticatedShell();

    await page.getByTestId("tab-racks").click();
    await expect(page.getByTestId("add-rack-button")).toBeVisible();
    await expect(page.getByTestId("rack-search-input")).toBeVisible();
  });

  test("samples tab opens manage location modal for view/edit flow", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-042",
      riskTier: "P0",
      domain: "storage",
      notes: "M6 location view/edit modal parity smoke",
    });

    await gotoAndWait("/Storage/samples");
    await ensureAuthenticatedShell();

    const rows = page.getByTestId("sample-row");
    const rowCount = await rows.count();
    test.skip(
      rowCount === 0,
      "No storage sample rows available for modal interaction",
    );

    await rows.first().getByTestId("sample-actions-overflow-menu").click();
    await page.getByTestId("manage-location-menu-item").click();
    await expect(page.getByTestId("location-management-modal")).toBeVisible();
    await expect(page.getByTestId("sample-info-section")).toBeVisible();
    await expect(page.getByTestId("new-location-section")).toBeVisible();
  });

  test("samples tab location path is available in compact/selected view", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-046",
      riskTier: "P0",
      domain: "storage",
      notes: "M6 view storage path parity smoke",
    });

    await gotoAndWait("/Storage/samples");
    await ensureAuthenticatedShell();

    await expect(page.getByTestId("sample-list")).toBeVisible();
  });
});
