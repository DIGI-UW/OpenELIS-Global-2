import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin study menu parity migration", () => {
  test("loads study menu management shell", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-010",
      riskTier: "P2",
      domain: "admin",
      notes: "Study menu management parity smoke",
    });

    await gotoAndWait("/MasterListsPage/studyMenuManagement");
    await ensureAuthenticatedShell();

    await expect(page.locator("#toggleShowChildren")).toBeVisible();
    await expect(page.getByRole("button", { name: /Submit/i })).toBeVisible();
  });
});
