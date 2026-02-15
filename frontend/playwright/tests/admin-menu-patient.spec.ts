import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin patient menu parity migration", () => {
  test("loads patient menu management shell", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-009",
      riskTier: "P2",
      domain: "admin",
      notes: "Patient menu management parity smoke",
    });

    await gotoAndWait("/MasterListsPage/patientMenuManagement");
    await ensureAuthenticatedShell();

    await expect(page.locator("#toggleShowChildren")).toBeVisible();
    await expect(page.getByRole("button", { name: /Submit/i })).toBeVisible();
  });
});
