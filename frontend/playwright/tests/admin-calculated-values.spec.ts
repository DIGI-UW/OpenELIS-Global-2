import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin calculated values parity migration", () => {
  test("loads calculated value builder shell controls", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-003",
      riskTier: "P1",
      domain: "admin",
      notes: "Calculated values builder parity smoke",
    });

    await gotoAndWait("/MasterListsPage/calculatedValue");
    await ensureAuthenticatedShell();

    await expect(page.locator('input[id$="_name"]').first()).toBeVisible();
    await expect(
      page.locator('button[id$="_testresult"]').first(),
    ).toBeVisible();
    await expect(page.locator('button[id^="submit_"]').first()).toBeVisible();
    await expect(page.locator('[data-cy="calcRule"]')).toBeVisible();
  });
});
