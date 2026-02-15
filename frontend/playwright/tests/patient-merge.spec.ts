import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Patient merge parity migration", () => {
  test("loads patient merge wizard selection step shell", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-029",
      riskTier: "P1",
      domain: "clinical",
      notes: "Patient merge wizard shell parity smoke (selection step)",
    });

    await gotoAndWait("/PatientMerge");
    await ensureAuthenticatedShell();

    await expect(page.locator("#patient1-patientId")).toBeVisible();
    await expect(page.locator("#patient1-firstName")).toBeVisible();
    await expect(page.locator("#patient1-lastName")).toBeVisible();
    await expect(page.locator("#patient2-patientId")).toBeVisible();
    await expect(page.locator("#patient2-firstName")).toBeVisible();
    await expect(page.locator("#patient2-lastName")).toBeVisible();

    const navButtons = page.locator(".patientMergeNavigation button");
    const navButtonCount = await navButtons.count();
    expect(navButtonCount).toBeGreaterThanOrEqual(2);
  });
});
