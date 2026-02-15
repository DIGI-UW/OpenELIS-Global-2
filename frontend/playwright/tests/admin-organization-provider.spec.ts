import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin organization and provider parity migration", () => {
  test("navigates to organization management and opens add form", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-012",
      riskTier: "P0",
      domain: "admin",
      notes: "M4b organization management parity smoke",
    });

    await gotoAndWait("/MasterListsPage");
    await ensureAuthenticatedShell();

    await page.locator('[data-cy="orgMgmnt"]').click();
    await expect(page).toHaveURL(/\/MasterListsPage\/organizationManagement/);
    await expect(
      page.getByRole("heading", { name: "Organization Management" }),
    ).toBeVisible();

    await page.locator('[data-cy="add-button"]').click();
    await expect(page.locator("#org-name")).toBeVisible();
    await expect(page.locator("#is-active")).toBeVisible();
    await expect(page.locator("#saveButton")).toBeVisible();
  });

  test("navigates to provider management and opens add provider modal", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-014",
      riskTier: "P0",
      domain: "admin",
      notes: "M4b provider management parity smoke",
    });

    await gotoAndWait("/MasterListsPage");
    await ensureAuthenticatedShell();

    await page.locator('[data-cy="providerMgmnt"]').click();
    await expect(page).toHaveURL(/\/MasterListsPage\/providerMenu/);
    await expect(
      page.getByRole("heading", { name: "Provider Management" }),
    ).toBeVisible();

    await page.locator('[data-cy="add-Button"]').first().click();
    await expect(page.locator("#lastName").first()).toBeVisible();
    await expect(page.locator("#firstName").first()).toBeVisible();
    await page.getByRole("button", { name: "Cancel" }).first().click();
  });
});
