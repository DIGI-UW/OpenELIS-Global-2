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

    await expect(page.locator("#select-1")).toBeVisible();
    await expect(
      page.locator('[data-cy="workplanResultsTable"]'),
    ).toBeVisible();
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

    await expect(
      page.locator(
        'a[href*="/RoutineReport?type=patient&report=patientCILNSP_vreduit"]',
      ),
    ).toBeVisible();
  });
});
