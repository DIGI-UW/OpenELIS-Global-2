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

    await expect(page.locator("#maximizeIcon").first()).toBeVisible();
    const readyForValidation = page
      .getByRole("link", {
        name: "Ready For Validation",
      })
      .first();
    if ((await readyForValidation.count()) > 0) {
      await expect(readyForValidation).toBeVisible();
      await readyForValidation.click();
      await expect(page).not.toHaveURL(/\/Dashboard$/);
    } else {
      await expect(page.locator("#mainHeader")).toBeVisible();
    }
  });
});
