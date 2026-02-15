import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Home and navbar navigation parity migration", () => {
  test("search, notifications, and help interactions work from dashboard", async ({
    page,
    gotoAndWait,
    ensureAuthForScenario,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-022",
      riskTier: "P0",
      domain: "core",
      notes: "M4a navbar interaction parity check",
    });

    await gotoAndWait("/Dashboard");
    await ensureAuthForScenario("read");

    await page.locator("#search-Icon").click();
    await expect(page.locator("#searchItem")).toBeVisible();
    await page.locator("#search-Icon").click();
    await expect(page.locator("#searchItem")).toBeHidden();

    await page.locator("#notification-Icon").click();
    await expect(page.locator(".slide-over-title")).toContainText(
      "Notifications",
    );
    await page.locator("#notification-Icon").click();

    await page.locator("#user-Help").click();
    await expect(page.getByLabel("Help Panel")).toBeVisible();
    await page.locator("#user-Help").click();
    await expect(page.getByLabel("Help Panel")).toBeHidden();
  });

  test("key side navigation targets are reachable", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-022",
      riskTier: "P0",
      domain: "core",
      notes: "M4a key menu navigation parity check",
    });

    await gotoAndWait("/Dashboard");
    await ensureAuthenticatedShell();

    await gotoAndWait("/SamplePatientEntry");
    await ensureAuthenticatedShell();
    await expect(page).toHaveURL(/SamplePatientEntry|Order/i);

    await gotoAndWait("/PatientManagement");
    await ensureAuthenticatedShell();
    await expect(page).toHaveURL(/Patient/i);
  });
});
