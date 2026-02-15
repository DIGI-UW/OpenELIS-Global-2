import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin notify user parity migration", () => {
  test("loads notify user form shell controls", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-011",
      riskTier: "P2",
      domain: "admin",
      notes: "Notify user page shell parity smoke",
    });

    await gotoAndWait("/MasterListsPage/NotifyUser");
    await ensureAuthenticatedShell();

    await expect(page.locator("#message")).toBeVisible();
    await expect(page.locator("#user")).toBeVisible();
    await expect(page.locator('[data-cy="submitButton"]')).toBeVisible();
  });
});
