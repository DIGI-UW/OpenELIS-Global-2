import { addParityAnnotations } from "../fixtures/parity-metadata";
import { ensureStorageFixturesLoaded } from "../fixtures/storage-fixtures";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Storage samples visibility parity migration", () => {
  test.beforeAll(() => {
    ensureStorageFixturesLoaded();
  });

  test("samples tab renders list container and sample rows when present", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-044",
      riskTier: "P1",
      domain: "storage",
      notes: "M6 assigned samples visibility parity smoke",
    });

    await gotoAndWait("/Storage/samples");
    await ensureAuthenticatedShell();

    await expect(page.getByTestId("sample-list")).toBeVisible();
    const rows = page.getByTestId("sample-row");
    const rowCount = await rows.count();
    if (rowCount > 0) {
      await expect(rows.first()).toBeVisible();
    }
  });
});
