import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Admin user management parity migration", () => {
  test("navigates to user management from admin hub", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-018",
      riskTier: "P0",
      domain: "admin",
      notes: "M4b user management navigation parity smoke",
    });

    await gotoAndWait("/MasterListsPage");
    await ensureAuthenticatedShell();

    await page.locator('[data-cy="userMgmnt"]').click();
    await expect(page).toHaveURL(/\/MasterListsPage\/userManagement/);
    await expect(
      page.getByRole("heading", { name: "User Management" }),
    ).toBeVisible();
  });

  test("opens add user form from user management page", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-018",
      riskTier: "P0",
      domain: "admin",
      notes: "M4b add-user form parity smoke",
    });

    await gotoAndWait("/MasterListsPage/userManagement");
    await ensureAuthenticatedShell();

    await page.locator('[data-cy="add-button"]').click();
    await expect(page.getByRole("heading", { name: "Add User" })).toBeVisible();
    await expect(page.locator("#login-name")).toBeVisible();
    await expect(page.locator("#first-name")).toBeVisible();
    await expect(page.locator("#last-name")).toBeVisible();
    await page.locator('[data-cy="exitButton"]').click();
  });
});
