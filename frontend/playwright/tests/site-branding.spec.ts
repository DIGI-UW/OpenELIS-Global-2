import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Site branding parity migration", () => {
  test("loads branding configuration with core controls", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-032",
      riskTier: "P1",
      domain: "core",
      notes: "Site branding page access and control visibility parity smoke",
    });

    await gotoAndWait("/MasterListsPage/SiteBrandingMenu");
    await ensureAuthenticatedShell();

    await expect(
      page.getByRole("heading", { name: /Site Branding/i }),
    ).toBeVisible();
    await expect(page.getByTestId("branding-cancel-button")).toBeVisible();
    await expect(page.getByTestId("branding-reset-button")).toBeVisible();
    await expect(page.getByTestId("color-preview").first()).toBeVisible();
    await expect(page.locator("#use-header-logo-for-login")).toBeVisible();
  });
});
