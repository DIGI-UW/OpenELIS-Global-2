import { addParityAnnotations } from "../fixtures/parity-metadata";
import { ensureStorageFixturesLoaded } from "../fixtures/storage-fixtures";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Storage location expandable rows parity migration", () => {
  test.beforeAll(() => {
    ensureStorageFixturesLoaded();
  });

  test("rooms and devices rows expand to reveal detail panels", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-043",
      riskTier: "P1",
      domain: "storage",
      notes: "Storage expandable-row detail panel parity smoke",
    });

    await gotoAndWait("/Storage/rooms");
    await ensureAuthenticatedShell();

    await page.getByTestId("tab-rooms").click();
    const roomRows = page.locator('[data-testid^="room-row-"]');
    const roomCount = await roomRows.count();
    if (roomCount > 0) {
      const firstRoom = roomRows.first();
      const roomRowId = await firstRoom.getAttribute("data-testid");
      if (roomRowId) {
        const roomId = roomRowId.replace("room-row-", "");
        await firstRoom.click();
        await expect(page.getByTestId(`expanded-room-${roomId}`)).toBeVisible();
      }
    }

    await page.getByTestId("tab-devices").click();
    const deviceRows = page.locator('[data-testid^="device-row-"]');
    const deviceCount = await deviceRows.count();
    if (deviceCount > 0) {
      const firstDevice = deviceRows.first();
      const deviceRowId = await firstDevice.getAttribute("data-testid");
      if (deviceRowId) {
        const deviceId = deviceRowId.replace("device-row-", "");
        await firstDevice.click();
        await expect(
          page.getByTestId(`expanded-device-${deviceId}`),
        ).toBeVisible();
      }
    }
  });
});
