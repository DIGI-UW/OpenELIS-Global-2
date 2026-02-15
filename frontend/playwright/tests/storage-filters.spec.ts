import { addParityAnnotations } from "../fixtures/parity-metadata";
import { ensureStorageFixturesLoaded } from "../fixtures/storage-fixtures";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Storage filters parity migration", () => {
  test.beforeAll(() => {
    ensureStorageFixturesLoaded();
  });

  test("tab-specific filter controls render across storage tabs", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-039",
      riskTier: "P1",
      domain: "storage",
      notes: "Storage tab filter controls parity smoke",
    });

    await gotoAndWait("/Storage/samples");
    await ensureAuthenticatedShell();

    await expect(
      page.locator('[data-testid="location-filter-dropdown"]:visible').first(),
    ).toBeVisible();

    await page.getByTestId("tab-rooms").click();
    await expect(page.locator("#filter-status:visible").first()).toBeVisible();

    await page.getByTestId("tab-devices").click();
    await expect(page.locator("#filter-room:visible").first()).toBeVisible();
    await expect(page.locator("#filter-status:visible").first()).toBeVisible();

    await page.getByTestId("tab-shelves").click();
    await expect(page.locator("#filter-room:visible").first()).toBeVisible();
    await expect(page.locator("#filter-device:visible").first()).toBeVisible();
    await expect(page.locator("#filter-status:visible").first()).toBeVisible();

    await page.getByTestId("tab-racks").click();
    await expect(page.locator("#filter-room:visible").first()).toBeVisible();
    await expect(page.locator("#filter-device:visible").first()).toBeVisible();
    await expect(page.locator("#filter-status:visible").first()).toBeVisible();
  });
});
