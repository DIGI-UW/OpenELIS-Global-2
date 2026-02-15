import { addParityAnnotations } from "../fixtures/parity-metadata";
import { ensureStorageFixturesLoaded } from "../fixtures/storage-fixtures";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Storage dashboard parity migration", () => {
  test.beforeAll(() => {
    ensureStorageFixturesLoaded();
  });

  test("dashboard loads with metric cards and tab navigation", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-036",
      riskTier: "P1",
      domain: "storage",
      notes: "Storage dashboard shell/metrics/tabs parity smoke",
    });

    await gotoAndWait("/Storage");
    await ensureAuthenticatedShell();

    await expect(page.locator(".storage-dashboard")).toBeVisible();
    const metricTileCount = await page.locator(".cds--tile").count();
    expect(metricTileCount).toBeGreaterThanOrEqual(4);

    await expect(page.getByTestId("tab-samples")).toBeVisible();
    await expect(page.getByTestId("tab-rooms")).toBeVisible();
    await expect(page.getByTestId("tab-devices")).toBeVisible();
    await expect(page.getByTestId("tab-shelves")).toBeVisible();
    await expect(page.getByTestId("tab-racks")).toBeVisible();
  });
});
