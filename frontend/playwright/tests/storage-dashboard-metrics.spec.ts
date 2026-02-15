import { addParityAnnotations } from "../fixtures/parity-metadata";
import { ensureStorageFixturesLoaded } from "../fixtures/storage-fixtures";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Storage dashboard metrics parity migration", () => {
  test.beforeAll(() => {
    ensureStorageFixturesLoaded();
  });

  test("location metric card renders breakdown pills", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-037",
      riskTier: "P1",
      domain: "storage",
      notes: "Storage location metric-card breakdown parity smoke",
    });

    await gotoAndWait("/Storage");
    await ensureAuthenticatedShell();

    const disposedMetricCard = page.getByTestId("metric-disposed");
    await expect(disposedMetricCard).toBeVisible();
    await expect(disposedMetricCard.locator(".metric-value")).toBeVisible();

    const locationBreakdown = page.locator(".location-counts-breakdown");
    await expect(locationBreakdown).toBeVisible();
    await expect(locationBreakdown.locator(".location-count-pill")).toHaveCount(
      4,
    );
    await expect(
      locationBreakdown.locator(".location-count-rooms"),
    ).toBeVisible();
    await expect(
      locationBreakdown.locator(".location-count-devices"),
    ).toBeVisible();
    await expect(
      locationBreakdown.locator(".location-count-shelves"),
    ).toBeVisible();
    await expect(
      locationBreakdown.locator(".location-count-racks"),
    ).toBeVisible();
  });
});
