import { expect, test, Page, Locator } from "../../../helpers/test-base";
import { UI_TIMEOUT, LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * General Configurations — foundational verification
 *
 * Verifies that SiteInformation config pages load via admin sidenav
 * navigation and support toggling boolean configuration values.
 *
 * All interactions are scoped to `.adminSideNav` (the admin page's
 * own sidenav) to avoid matching the header's main navigation.
 * Uses role-based and text-based selectors — no data-cy, no
 * hard-coded cell indexes, no .first() assumptions.
 */

/** Get the admin sidenav locator, wait for it to render */
async function getAdminNav(page: Page): Promise<Locator> {
  const adminNav = page.locator(".adminSideNav");
  await expect(adminNav).toBeVisible({ timeout: LONG_TIMEOUT });
  return adminNav;
}

/** Navigate to a config page via the admin sidenav */
async function navigateToConfig(page: Page, configName: RegExp) {
  const adminNav = await getAdminNav(page);

  // Expand the "General Configurations" parent menu
  // (admin.formEntryConfig i18n key resolves to "General Configurations")
  const formEntryMenu = adminNav.getByRole("button", {
    name: /General Configurations/i,
  });
  await expect(formEntryMenu).toBeVisible({ timeout: UI_TIMEOUT });
  const isExpanded = await formEntryMenu.getAttribute("aria-expanded");
  if (isExpanded !== "true") {
    await formEntryMenu.click();
  }

  const menuItem = adminNav.getByRole("link", { name: configName });
  await expect(menuItem).toBeVisible({ timeout: UI_TIMEOUT });
  await menuItem.click();
}

const CONFIG_PAGES = [
  {
    name: "NonConformity Configuration",
    menuText: /Non.?Conformity Configuration/i,
    url: /NonConformityConfigurationMenu/,
  },
  {
    name: "WorkPlan Configuration",
    menuText: /Work.?[Pp]lan Configuration/i,
    url: /Work[Pp]lanConfigurationMenu/,
  },
  {
    name: "Site Information",
    menuText: /Site Information$/i,
    url: /SiteInformationMenu/,
  },
];

test.describe("General Configurations", () => {
  test.setTimeout(90_000);

  test.beforeEach(async ({ page }) => {
    await page.goto("/MasterListsPage");
  });

  for (const config of CONFIG_PAGES) {
    test(`${config.name} — loads and displays config table`, async ({
      page,
    }) => {
      await navigateToConfig(page, config.menuText);

      await expect(page).toHaveURL(config.url, { timeout: LONG_TIMEOUT });

      // Verify page loaded with data table with rows
      const table = page.locator("table");
      await expect(table).toBeVisible({ timeout: UI_TIMEOUT });
      // Table must have at least one data row (tr with a radio button)
      await expect(
        table
          .locator("tr")
          .filter({ has: page.locator("input[type='radio']") })
          .first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    });
  }

  test("NonConformity — toggle a config value", async ({ page }) => {
    await navigateToConfig(page, /Non.?Conformity Configuration/i);
    await expect(page).toHaveURL(/NonConformityConfiguration/, {
      timeout: LONG_TIMEOUT,
    });

    // Select the first config row's radio button
    const firstRadio = page.locator(".cds--radio-button__label").first();
    await expect(firstRadio).toBeVisible({ timeout: UI_TIMEOUT });
    await firstRadio.click();

    // Click Modify
    const modifyBtn = page.getByRole("button", { name: /Modify/i });
    await expect(modifyBtn).toBeVisible({ timeout: UI_TIMEOUT });
    await modifyBtn.click();

    // Verify Edit Record page loaded
    await expect(
      page.getByRole("heading", { name: "Edit Record" }),
    ).toBeVisible({ timeout: UI_TIMEOUT });

    // Read current checked state, click opposite
    const falseRadio = page
      .locator(".cds--radio-button__label")
      .filter({ hasText: "False" });
    const trueRadio = page
      .locator(".cds--radio-button__label")
      .filter({ hasText: "True" });
    const falseInput = falseRadio.locator("..").locator("input");
    const isFalseChecked = await falseInput.isChecked().catch(() => false);
    const targetValue = isFalseChecked ? "True" : "False";

    const targetRadio = targetValue === "True" ? trueRadio : falseRadio;
    await targetRadio.click();

    // Save
    const saveBtn = page.getByRole("button", { name: /Save/i });
    await expect(saveBtn).toBeVisible({ timeout: UI_TIMEOUT });
    await saveBtn.click();

    // Verify we returned to the config list
    await expect(
      page.getByRole("heading", { name: "Edit Record" }),
    ).not.toBeVisible({ timeout: UI_TIMEOUT });
    await expect(page.locator("table")).toBeVisible({ timeout: UI_TIMEOUT });
  });
});
