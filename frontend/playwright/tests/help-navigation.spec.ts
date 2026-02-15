import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Help navigation parity migration", () => {
  test("opens help panel and renders help actions", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-021",
      riskTier: "P1",
      domain: "core",
      notes: "Help panel parity smoke for menu visibility and toggle behavior",
    });

    await gotoAndWait("/Dashboard");
    await ensureAuthenticatedShell();

    await page.locator("#user-Help").click();
    const helpPanel = page.getByLabel("Help Panel");
    await expect(helpPanel).toBeVisible();
    await expect(helpPanel.locator("button")).toHaveCount(3);

    await page.locator("#user-Help").click();
    await expect(helpPanel).toBeHidden();
  });
});
