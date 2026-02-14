import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Home and navbar navigation parity migration", () => {
  test("search, notifications, and help interactions work from dashboard", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-022",
      riskTier: "P0",
      domain: "core",
      notes: "M4a navbar interaction parity check",
    });

    await gotoAndWait("/Dashboard");
    await ensureAuthenticatedShell();

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

  test("key side navigation routes are reachable from menu actions", async ({
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

    await page.locator("#menu_sample").click();
    await page.locator("#menu_sample_add_nav").click();
    await expect(page).toHaveURL(/SampleEntry|Order|Add/i);

    await page.locator("#sidenav-menu-button").click();
    await page.locator("#menu_patient").click();
    await page.locator("#menu_patient_add_or_edit_nav").click();
    await expect(page).toHaveURL(/Patient/i);
  });
});
