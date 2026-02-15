import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin non-conformity menu parity migration", () => {
  test("loads non-conformity menu management shell", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-008",
      riskTier: "P2",
      domain: "admin",
      notes: "Non-conformity menu management parity smoke",
    });

    await gotoAndWait("/MasterListsPage/nonConformityMenuManagement");
    await ensureAuthenticatedShell();

    await expect(page.locator("#toggleShowChildren")).toBeVisible();
    await expect(page.getByRole("button", { name: /Submit/i })).toBeVisible();
  });
});
