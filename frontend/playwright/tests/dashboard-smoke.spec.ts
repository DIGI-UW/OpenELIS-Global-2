import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Dashboard critical navigation smoke", () => {
  test("critical dashboard tiles are visible and navigable", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-020",
      riskTier: "P1",
      domain: "core",
      notes: "M4a dashboard tile parity smoke",
    });

    await gotoAndWait("/Dashboard");
    await ensureAuthenticatedShell();

    await expect(page.locator("#maximizeIcon")).toBeVisible();
    await expect(
      page.getByRole("link", { name: "Ready For Validation" }),
    ).toBeVisible();
    await expect(
      page.getByRole("link", { name: "Orders Completed Today" }),
    ).toBeVisible();

    await page.getByRole("link", { name: "Ready For Validation" }).click();
    await expect(page).not.toHaveURL(/\/Dashboard$/);
  });
});
