import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin batch reassignment parity migration", () => {
  test("loads reassignment shell with core selectors and actions", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-002",
      riskTier: "P1",
      domain: "admin",
      notes: "Batch test reassignment shell parity smoke",
    });

    await gotoAndWait("/MasterListsPage/batchTestReassignment");
    await ensureAuthenticatedShell();

    await expect(page.locator("#selectSampleType")).toBeVisible();
    await expect(page.locator("#selectSampleType1")).toBeVisible();
    await expect(page.locator("#selectSampleType0")).toBeVisible();
    await expect(page.locator('[data-cy="okButton"]')).toBeVisible();
    await expect(page.locator('[data-cy="cancelButton"]')).toBeVisible();
  });
});
