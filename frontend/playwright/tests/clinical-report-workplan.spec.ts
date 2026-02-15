import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Clinical report and workplan parity migration", () => {
  test("loads workplan by panel shell", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-048",
      riskTier: "P0",
      domain: "clinical",
      notes: "M5 workplan shell parity smoke",
    });

    await gotoAndWait("/WorkplanByPanel");
    await ensureAuthenticatedShell();

    await expect(page).toHaveURL(/WorkplanByPanel/i);
    await expect(page.locator("#mainHeader")).toBeVisible();
  });

  test("loads routine reports navigation shell", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-030",
      riskTier: "P0",
      domain: "clinical",
      notes: "M5 routine report navigation shell parity smoke",
    });

    await gotoAndWait("/RoutineReports");
    await ensureAuthenticatedShell();

    await expect(page).toHaveURL(/RoutineReports/i);
    await expect(page.locator("#mainHeader")).toBeVisible();
  });
});
