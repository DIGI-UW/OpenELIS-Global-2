import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin result reporting configuration parity migration", () => {
  test("loads reporting configuration controls", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-016",
      riskTier: "P2",
      domain: "admin",
      notes: "Result reporting configuration parity smoke",
    });

    await gotoAndWait("/MasterListsPage/resultReportingConfiguration");
    await ensureAuthenticatedShell();

    await expect(page.locator('input[id^="url-"]').first()).toBeVisible();
    await expect(
      page.locator('input[id^="enabled-"][id$="-yes"]').first(),
    ).toBeVisible();
    await expect(page.locator('[data-cy="saveButton"]')).toBeVisible();
    await expect(page.locator('[data-cy="cancelButton"]')).toBeVisible();
  });
});
