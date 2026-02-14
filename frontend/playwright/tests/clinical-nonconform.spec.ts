import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Clinical non-conformity parity migration", () => {
  test("loads report non-conforming event search shell", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-026",
      riskTier: "P0",
      domain: "clinical",
      notes: "M5 non-conformity search shell parity smoke",
    });

    await gotoAndWait("/ReportNonConformingEvent");
    await ensureAuthenticatedShell();

    await expect(page.locator("#type")).toBeVisible();
    await expect(
      page.locator('[data-testid="nce-search-button"]'),
    ).toBeVisible();
  });
});
