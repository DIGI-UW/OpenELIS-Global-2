import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Clinical patient and order parity migration", () => {
  test("loads batch order entry setup shell", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-019",
      riskTier: "P0",
      domain: "clinical",
      notes: "M5 batch order entry shell parity smoke",
    });

    await gotoAndWait("/SampleBatchEntrySetup");
    await ensureAuthenticatedShell();

    await expect(page).toHaveURL(/SampleBatchEntrySetup/i);
    await expect(page.locator("#siteName").first()).toBeVisible();
  });

  test("loads sample patient entry shell", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-027",
      riskTier: "P0",
      domain: "clinical",
      notes: "M5 order entry shell parity smoke",
    });

    await gotoAndWait("/SamplePatientEntry");
    await ensureAuthenticatedShell();

    await expect(page).toHaveURL(/SamplePatientEntry/i);
    await expect(page.locator("#mainHeader")).toBeVisible();
  });

  test("loads patient management shell", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-028",
      riskTier: "P0",
      domain: "clinical",
      notes: "M5 patient management shell parity smoke",
    });

    await gotoAndWait("/PatientManagement");
    await ensureAuthenticatedShell();

    await expect(page.locator("#searchPatient")).toBeVisible();
    await expect(page.locator("#newPatient")).toBeVisible();
  });

  test("loads modify-order search shell", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-025",
      riskTier: "P0",
      domain: "clinical",
      notes: "M5 modify-order search parity smoke",
    });

    await gotoAndWait("/SampleEdit");
    await ensureAuthenticatedShell();

    await expect(page.locator("#labNumber").first()).toBeVisible();
    await expect(page.locator('[data-cy="submit-button"]')).toBeVisible();
  });
});
