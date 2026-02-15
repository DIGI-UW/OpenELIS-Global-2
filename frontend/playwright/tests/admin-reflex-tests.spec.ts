import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin reflex tests parity migration", () => {
  test("loads reflex rule builder shell controls", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-015",
      riskTier: "P2",
      domain: "admin",
      notes: "Reflex tests rule-builder parity smoke",
    });

    await gotoAndWait("/MasterListsPage/reflex");
    await ensureAuthenticatedShell();

    await expect(page.locator('input[id$="_rulename"]').first()).toBeVisible();
    await expect(
      page.locator('select[data-cy="addSample"]').first(),
    ).toBeVisible();
    await expect(page.locator('button[id^="submit_"]').first()).toBeVisible();
    await expect(page.locator('[data-cy="rule"]')).toBeVisible();
  });
});
