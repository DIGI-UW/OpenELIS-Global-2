import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin test management parity migration", () => {
  test("loads test management hub with core tiles", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-017",
      riskTier: "P2",
      domain: "admin",
      notes: "Test management hub parity smoke",
    });

    await gotoAndWait("/MasterListsPage/testManagementConfigMenu");
    await ensureAuthenticatedShell();

    await expect(page.locator("#TestRenameEntry")).toBeVisible();
    await expect(page.locator("#TestCatalog")).toBeVisible();
    await expect(page.locator("#reflex")).toBeVisible();
    await expect(page.locator("#calculatedValue")).toBeVisible();
  });
});
