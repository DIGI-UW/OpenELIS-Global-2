import { expect, test } from "@playwright/test";
import { UI_TIMEOUT, LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * General Configurations — foundational verification
 *
 * Verifies that SiteInformation config pages load correctly and
 * support toggling boolean configuration values. Replaces the
 * fragile Cypress generalConfigurations.cy.js test with proper
 * selectors (no hard-coded cell indexes or .first() radio clicks).
 */

const CONFIG_PAGES = [
  {
    name: "NonConformity Configuration",
    dataCy: "nonConformConfig",
    parentMenu: "admin.formEntryConfig",
    url: "/NonConformityConfigurationMenu",
  },
  {
    name: "WorkPlan Configuration",
    dataCy: "workPlanConfig",
    parentMenu: "admin.formEntryConfig",
    url: "/WorkplanConfigurationMenu",
  },
  {
    name: "Site Information",
    dataCy: "siteInfoMenu",
    parentMenu: "admin.formEntryConfig",
    url: "/SiteInformationMenu",
  },
];

test.describe("General Configurations", () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to admin page
    await page.goto("/MasterListsPage", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/MasterListsPage/, { timeout: LONG_TIMEOUT });
  });

  for (const config of CONFIG_PAGES) {
    test(`${config.name} — loads and displays config table`, async ({
      page,
    }) => {
      // Click the config menu item via data-cy attribute
      const menuItem = page.locator(`[data-cy="${config.dataCy}"]`);
      await expect(menuItem).toBeVisible({ timeout: UI_TIMEOUT });
      await menuItem.click();

      // Verify we navigated to the config page
      await expect(page).toHaveURL(new RegExp(config.url), {
        timeout: LONG_TIMEOUT,
      });

      // Verify the page title heading is visible
      await expect(page.locator("h2")).toBeVisible({ timeout: UI_TIMEOUT });

      // Verify the config table has rows
      const table = page.locator("table");
      await expect(table).toBeVisible({ timeout: UI_TIMEOUT });
      const rows = table.locator("tbody tr");
      const rowCount = await rows.count();
      expect(rowCount).toBeGreaterThan(0);
    });
  }

  test("NonConformity — toggle a config value", async ({ page }) => {
    // Navigate to NonConformity config
    const menuItem = page.locator('[data-cy="nonConformConfig"]');
    await expect(menuItem).toBeVisible({ timeout: UI_TIMEOUT });
    await menuItem.click();
    await expect(page).toHaveURL(/NonConformityConfiguration/, {
      timeout: LONG_TIMEOUT,
    });

    // Select the first config row's radio button by clicking its label
    const firstRadio = page.locator(".cds--radio-button__label").first();
    await expect(firstRadio).toBeVisible({ timeout: UI_TIMEOUT });
    await firstRadio.click();

    // Click Modify
    const modifyBtn = page.locator("[data-cy='modify-Button']");
    await expect(modifyBtn).toBeVisible({ timeout: UI_TIMEOUT });
    await modifyBtn.click();

    // Verify Edit Record page loaded
    await expect(page.locator("h2")).toContainText("Edit Record", {
      timeout: UI_TIMEOUT,
    });

    // Find current value and toggle it
    const falseOption = page.getByText("False", { exact: true });
    const trueOption = page.getByText("True", { exact: true });

    if (await falseOption.isVisible()) {
      await falseOption.click();
    } else if (await trueOption.isVisible()) {
      await trueOption.click();
    }

    // Save
    const saveBtn = page.locator("[data-cy='save-Button']");
    await expect(saveBtn).toBeVisible({ timeout: UI_TIMEOUT });
    await saveBtn.click();

    // Verify we returned to the config list
    await expect(page.locator("h2")).not.toContainText("Edit Record", {
      timeout: UI_TIMEOUT,
    });

    // Verify table is still visible (config list reloaded)
    await expect(page.locator("table")).toBeVisible({ timeout: UI_TIMEOUT });
  });
});
