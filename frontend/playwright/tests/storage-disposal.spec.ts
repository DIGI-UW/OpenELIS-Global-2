import { addParityAnnotations } from "../fixtures/parity-metadata";
import { ensureStorageFixturesLoaded } from "../fixtures/storage-fixtures";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Storage disposal parity migration", () => {
  test.beforeAll(() => {
    ensureStorageFixturesLoaded();
  });

  test("opens dispose modal and renders core disposal controls", async ({
    page,
    gotoAndWait,
    ensureAuthForScenario,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-038",
      riskTier: "P1",
      domain: "storage",
      notes: "Disposal modal structure parity smoke",
    });

    await gotoAndWait("/Storage/samples");
    await ensureAuthForScenario("read");

    await expect(page.getByTestId("sample-list")).toBeVisible();
    const sampleRows = page.getByTestId("sample-row");
    const rowCount = await sampleRows.count();
    if (rowCount === 0) {
      return;
    }

    await sampleRows
      .first()
      .getByTestId("sample-actions-overflow-menu")
      .click();
    await page.getByTestId("dispose-menu-item").click();

    await expect(page.getByTestId("dispose-modal")).toBeVisible();
    await expect(page.getByTestId("warning-alert")).toBeVisible();
    await expect(page.locator("#disposal-reason")).toBeVisible();
    await expect(page.locator("#disposal-method")).toBeVisible();
    await expect(page.locator("#disposal-confirmation")).toBeVisible();
    await expect(
      page.getByRole("button", { name: /Confirm Disposal/i }),
    ).toBeVisible();
  });
});
