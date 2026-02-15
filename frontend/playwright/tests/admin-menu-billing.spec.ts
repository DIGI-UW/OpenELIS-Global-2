import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin billing menu parity migration", () => {
  test("loads billing menu management with core controls", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-006",
      riskTier: "P2",
      domain: "admin",
      notes: "Billing menu management parity smoke",
    });

    await gotoAndWait("/MasterListsPage/billingMenuManagement");
    await ensureAuthenticatedShell();

    await expect(page.locator("#billing_address")).toBeVisible();
    await expect(page.locator("#billing_active")).toBeVisible();
    await expect(page.getByRole("button", { name: /Submit/i })).toBeVisible();
  });
});
