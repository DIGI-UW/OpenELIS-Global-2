import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin program entry parity migration", () => {
  test("loads program management form shell controls", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-013",
      riskTier: "P1",
      domain: "admin",
      notes: "Program management shell parity smoke",
    });

    await gotoAndWait("/MasterListsPage/program");
    await ensureAuthenticatedShell();

    await expect(page.locator("#additionalQuestionsSelect")).toBeVisible();
    await expect(page.locator("#program\\.programName")).toBeVisible();
    await expect(page.locator("#program\\.code")).toBeVisible();
    await expect(page.locator("#test_section")).toBeVisible();
    await expect(page.locator("#submitProgram")).toBeVisible();
  });
});
