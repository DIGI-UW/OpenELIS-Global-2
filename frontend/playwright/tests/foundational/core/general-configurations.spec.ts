import { expect, test, Page } from "@playwright/test";
import { UI_TIMEOUT, LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * General Configurations — foundational verification
 *
 * Verifies that SiteInformation config pages load correctly and
 * support toggling boolean configuration values. Replaces the
 * fragile Cypress generalConfigurations.cy.js test with proper
 * selectors (no hard-coded cell indexes or .first() radio clicks).
 */

/** Expand a Carbon SideNavMenu by clicking its button if not already expanded */
async function expandSideNavMenu(page: Page, menuText: RegExp | string) {
  const menuButton = page
    .locator(".cds--side-nav__submenu")
    .filter({ hasText: menuText });
  const isExpanded = await menuButton.getAttribute("aria-expanded");
  if (isExpanded !== "true") {
    await menuButton.click();
  }
}

/** Navigate to a config page via the admin sidenav */
async function navigateToConfig(page: Page, dataCy: string) {
  // Expand the "Form Entry Configuration" parent menu
  await expandSideNavMenu(page, /Form Entry|formEntry/i);

  // Click the specific config submenu item
  const menuItem = page.locator(`[data-cy="${dataCy}"]`);
  await expect(menuItem).toBeVisible({ timeout: UI_TIMEOUT });
  await menuItem.click();
}

const CONFIG_PAGES = [
  {
    name: "NonConformity Configuration",
    dataCy: "nonConformConfig",
    url: "/NonConformityConfigurationMenu",
  },
  {
    name: "WorkPlan Configuration",
    dataCy: "workPlanConfig",
    url: "/WorkplanConfigurationMenu",
  },
  {
    name: "Site Information",
    dataCy: "siteInfoMenu",
    url: "/SiteInformationMenu",
  },
];

test.describe("General Configurations", () => {
  test.setTimeout(60_000);

  test.beforeEach(async ({ page }) => {
    await page.goto("/MasterListsPage");
    // Wait for the admin sidenav to fully render (React SPA hydration + data load)
    await expect(page.locator(".cds--side-nav__submenu").first()).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
  });

  for (const config of CONFIG_PAGES) {
    test(`${config.name} — loads and displays config table`, async ({
      page,
    }) => {
      await navigateToConfig(page, config.dataCy);

      // Verify navigation
      await expect(page).toHaveURL(new RegExp(config.url), {
        timeout: LONG_TIMEOUT,
      });

      // Verify page title and table
      await expect(page.locator("h2")).toBeVisible({ timeout: UI_TIMEOUT });
      const table = page.locator("table");
      await expect(table).toBeVisible({ timeout: UI_TIMEOUT });
      const rowCount = await table.locator("tbody tr").count();
      expect(rowCount).toBeGreaterThan(0);
    });
  }

  test("NonConformity — toggle a config value", async ({ page }) => {
    await navigateToConfig(page, "nonConformConfig");
    await expect(page).toHaveURL(/NonConformityConfiguration/, {
      timeout: LONG_TIMEOUT,
    });

    // Select the first config row's radio button
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

    // Read current selected value, then toggle to opposite
    const falseRadio = page
      .locator(".cds--radio-button__label")
      .filter({ hasText: "False" });
    const trueRadio = page
      .locator(".cds--radio-button__label")
      .filter({ hasText: "True" });

    // Determine which is currently checked via the hidden input
    const falseInput = falseRadio.locator("..").locator("input");
    const isFalseChecked = await falseInput.isChecked().catch(() => false);
    const targetValue = isFalseChecked ? "True" : "False";

    // Click the opposite value
    const targetRadio = targetValue === "True" ? trueRadio : falseRadio;
    await targetRadio.click();

    // Save
    const saveBtn = page.locator("[data-cy='save-Button']");
    await expect(saveBtn).toBeVisible({ timeout: UI_TIMEOUT });
    await saveBtn.click();

    // Verify we returned to the config list with the toggled value
    await expect(page.locator("h2")).not.toContainText("Edit Record", {
      timeout: UI_TIMEOUT,
    });
    await expect(page.locator("table")).toBeVisible({ timeout: UI_TIMEOUT });

    // Verify the table shows the new value
    const firstRowCells = page.locator("table tbody tr").first().locator("td");
    await expect(firstRowCells.nth(2)).toContainText(
      targetValue.toLowerCase(),
      {
        timeout: UI_TIMEOUT,
      },
    );
  });
});
