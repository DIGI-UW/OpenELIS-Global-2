import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin general configuration parity migration", () => {
  test("core general configuration routes load with table and modify action", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-005",
      riskTier: "P1",
      domain: "admin",
      notes: "General configuration route parity smoke for core config pages",
    });

    const routes = [
      "/MasterListsPage/NonConformityConfigurationMenu",
      "/MasterListsPage/WorkPlanConfigurationMenu",
      "/MasterListsPage/ValidationConfigurationMenu",
    ];

    for (const route of routes) {
      await gotoAndWait(route);
      await ensureAuthenticatedShell();
      await expect(page.locator('[data-cy="modify-Button"]')).toBeVisible();
      await expect(page.getByRole("table").first()).toBeVisible();
    }
  });
});
