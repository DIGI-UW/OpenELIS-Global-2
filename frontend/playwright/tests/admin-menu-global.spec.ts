import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin global menu parity migration", () => {
  test("loads global menu management with toggle and submit", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-007",
      riskTier: "P2",
      domain: "admin",
      notes: "Global menu management parity smoke",
    });

    await gotoAndWait("/MasterListsPage/globalMenuManagement");
    await ensureAuthenticatedShell();

    await expect(page.locator("#toggleShowChildren")).toBeVisible();
    await expect(page.getByRole("button", { name: /Submit/i })).toBeVisible();
  });
});
