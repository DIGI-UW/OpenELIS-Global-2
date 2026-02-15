import { addParityAnnotations } from "../fixtures/parity-metadata";
import { ensureStorageFixturesLoaded } from "../fixtures/storage-fixtures";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Storage location CRUD smoke parity migration", () => {
  test.beforeAll(() => {
    ensureStorageFixturesLoaded();
  });

  test("rooms tab supports create modal and edit modal shell", async ({
    page,
    gotoAndWait,
    ensureAuthForScenario,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-041",
      riskTier: "P1",
      domain: "storage",
      notes: "Storage location CRUD smoke parity for create/edit modal access",
    });

    await gotoAndWait("/Storage/rooms");
    await ensureAuthForScenario("read");

    await page.getByTestId("tab-rooms").click();
    await expect(page.getByTestId("add-room-button")).toBeVisible();

    await page.getByTestId("add-room-button").click();
    await expect(page.getByTestId("storage-location-modal")).toBeVisible();
    await expect(page.locator("#room-name")).toBeVisible();
    await expect(page.locator("#room-code")).toBeVisible();
    await page
      .getByTestId("storage-location-modal")
      .locator('button[aria-label="Close"]')
      .click();
    await expect(page.getByTestId("storage-location-modal")).toBeHidden();

    const roomRows = page.locator('[data-testid^="room-row-"]');
    const rowCount = await roomRows.count();
    if (rowCount === 0) {
      return;
    }

    await roomRows
      .first()
      .getByTestId("location-actions-overflow-menu")
      .click({ force: true });
    await page.getByTestId("edit-location-menu-item").click();
    await expect(page.getByTestId("edit-location-modal")).toBeVisible();
    await expect(page.getByTestId("edit-location-room-name")).toBeVisible();
    await expect(page.getByTestId("edit-location-room-code")).toBeVisible();
    await page
      .getByTestId("edit-location-modal")
      .locator('button[aria-label="Close"]')
      .click();
    await expect(page.getByTestId("edit-location-modal")).toBeHidden();
  });
});
