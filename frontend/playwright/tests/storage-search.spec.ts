import { addParityAnnotations } from "../fixtures/parity-metadata";
import { ensureStorageFixturesLoaded } from "../fixtures/storage-fixtures";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Storage search parity migration", () => {
  test.beforeAll(() => {
    ensureStorageFixturesLoaded();
  });

  test("samples tab search input accepts and applies search text", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-045",
      riskTier: "P0",
      domain: "storage",
      notes:
        "Sample search input parity smoke for storage dashboard samples tab",
    });

    await gotoAndWait("/Storage/samples");
    await ensureAuthenticatedShell();

    const sampleSearchInput = page.getByTestId("sample-search-input");
    await expect(sampleSearchInput).toBeVisible();
    await sampleSearchInput.fill("101");
    await expect(sampleSearchInput).toHaveValue("101");

    await expect(page.getByTestId("sample-list")).toBeVisible();
    const sampleRows = page.getByTestId("sample-row");
    const rowCount = await sampleRows.count();
    if (rowCount > 0) {
      await expect(sampleRows.first()).toBeVisible();
    }
  });

  test("tab-specific search fields are available across storage dashboard tabs", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-045",
      riskTier: "P0",
      domain: "storage",
      notes:
        "Storage tab-specific search parity smoke for rooms/devices/shelves/racks",
    });

    await gotoAndWait("/Storage/samples");
    await ensureAuthenticatedShell();

    await page.getByTestId("tab-rooms").click();
    const roomSearchInput = page.getByTestId("room-search-input");
    await expect(roomSearchInput).toBeVisible();
    await roomSearchInput.fill("Main");
    await expect(roomSearchInput).toHaveValue("Main");

    await page.getByTestId("tab-devices").click();
    const deviceSearchInput = page.getByTestId("device-search-input");
    await expect(deviceSearchInput).toBeVisible();
    await deviceSearchInput.fill("Freezer");
    await expect(deviceSearchInput).toHaveValue("Freezer");

    await page.getByTestId("tab-shelves").click();
    const shelfSearchInput = page.getByTestId("shelf-search-input");
    await expect(shelfSearchInput).toBeVisible();
    await shelfSearchInput.fill("Shelf");
    await expect(shelfSearchInput).toHaveValue("Shelf");

    await page.getByTestId("tab-racks").click();
    const rackSearchInput = page.getByTestId("rack-search-input");
    await expect(rackSearchInput).toBeVisible();
    await rackSearchInput.fill("Rack");
    await expect(rackSearchInput).toHaveValue("Rack");
  });
});
