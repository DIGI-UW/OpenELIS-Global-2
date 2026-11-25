/**
 * Page Object for Admin > Site Information configuration toggle.
 *
 * Used by e-sig E2E tests to enable/disable the electronic signature
 * feature via the admin UI — the same way an admin would.
 */
import { Locator, Page, expect } from "@playwright/test";
import { UI_TIMEOUT, NAV_TIMEOUT } from "../helpers/timeouts";

/**
 * The admin settings menus that share the same table + Modify + Save screen.
 * Each shows one site_information domain: Site Information holds the identity
 * settings (electronic signature), Result Entry Configuration the result ones.
 */
export type SettingsMenu =
  | "SiteInformationMenu"
  | "ResultConfigurationMenu"
  | "SampleEntryConfigurationMenu";

const SETTINGS_MENU_HEADING: Record<SettingsMenu, RegExp> = {
  SiteInformationMenu: /site information/i,
  ResultConfigurationMenu:
    /result entry configuration|result & validation configuration/i,
  SampleEntryConfigurationMenu: /order entry configuration/i,
};

export class SiteInformationPage {
  constructor(
    private page: Page,
    private menu: SettingsMenu = "SiteInformationMenu",
  ) {}

  /** Navigate to the admin settings menu this page object was built for. */
  async goto() {
    await this.page.goto(`/MasterListsPage/${this.menu}`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      this.page.getByRole("heading", {
        name: SETTINGS_MENU_HEADING[this.menu],
      }),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
  }

  /**
   * Toggle a boolean site_information setting via the admin UI.
   *
   * 1. Find the row by name
   * 2. Select it (radio button)
   * 3. Click Modify
   * 4. Set the boolean value via radio button
   * 5. Save
   */
  /**
   * OGC-1364 v3: Result & Validation Configuration shows grouped switches with a
   * Save per group instead of the table. Returns the setting's block on that
   * layout, or null on a table menu. It waits for either layout first: deciding
   * before the page rendered sent a grouped page down the table path.
   */
  private async groupedSetting(settingName: string) {
    const grouped = this.page.getByTestId("result-validation-settings");
    await expect(
      grouped.or(
        this.page.getByRole("button", { name: "Modify", exact: true }),
      ),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
    if (!(await grouped.isVisible())) {
      return null;
    }
    const block = grouped
      .locator('[data-testid^="setting-"]', { hasText: settingName })
      .first();
    await expect(block).toBeVisible({ timeout: UI_TIMEOUT });
    return block;
  }

  private async saveGroupOf(block: Locator) {
    const group = block.locator(
      'xpath=ancestor::*[starts-with(@data-testid,"settings-group-")][1]',
    );
    await group.locator('[data-testid^="save-group-"]').click();
    await expect(
      this.page.locator(".cds--toast-notification").first(),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
  }

  async setBooleanSetting(settingName: string, value: boolean) {
    const block = await this.groupedSetting(settingName);
    if (block) {
      const toggle = block.getByRole("switch");
      if ((await toggle.getAttribute("aria-checked")) !== String(value)) {
        await block
          .locator(`label[for="${await toggle.getAttribute("id")}"]`)
          .click();
        await this.saveGroupOf(block);
      }
      return;
    }
    // Find the row containing the setting name and click its radio label
    const row = this.page.locator("tr", { hasText: settingName });
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
    // Carbon TableSelectRow renders radio inside a label — click the label
    await row.locator("label").first().click();

    // Click Modify
    await this.page
      .getByRole("button", { name: "Modify", exact: true })
      .click();

    // Wait for the edit form to load
    const radioGroup = this.page.locator(".cds--radio-button-group");
    await expect(radioGroup).toBeVisible({ timeout: UI_TIMEOUT });

    // Select the target value
    const targetLabel = value ? "True" : "False";
    await this.page
      .locator(".cds--radio-button__label")
      .filter({ hasText: targetLabel })
      .click();

    // Save — the admin page reloads after a successful save
    // (GenericConfigEdit calls window.location.reload() on 200),
    // so wait for the reload to complete and the list view to reappear.
    await this.page.getByRole("button", { name: "Save", exact: true }).click();
    await this.page.waitForLoadState("domcontentloaded");

    // The list view shows the "Modify" button — its presence confirms
    // we've left the edit form and the save + reload succeeded.
    await expect(
      this.page
        .getByRole("button", { name: "Modify", exact: true })
        .or(this.page.getByTestId("result-validation-settings")),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
  }

  /**
   * Set a text site_information setting via the admin UI: select its row,
   * Modify, replace the value, Save.
   */
  async setTextSetting(settingName: string, value: string) {
    const block = await this.groupedSetting(settingName);
    if (block) {
      await block.getByRole("textbox").fill(value);
      await this.saveGroupOf(block);
      return;
    }
    const row = this.page.locator("tr", { hasText: settingName });
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
    await row.locator("label").first().click();
    await this.page
      .getByRole("button", { name: "Modify", exact: true })
      .click();
    const field = this.page.getByRole("textbox", { name: "Value" });
    await expect(field).toBeVisible({ timeout: UI_TIMEOUT });
    await field.fill(value);
    await this.page.getByRole("button", { name: "Save", exact: true }).click();
    await this.page.waitForLoadState("domcontentloaded");
    await expect(
      this.page
        .getByRole("button", { name: "Modify", exact: true })
        .or(this.page.getByTestId("result-validation-settings")),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
  }

  /** Read the current value of a setting from the table. */
  async getSettingValue(settingName: string): Promise<string> {
    const block = await this.groupedSetting(settingName);
    if (block) {
      const toggle = block.getByRole("switch");
      return (await toggle.count())
        ? (await toggle.getAttribute("aria-checked")) || ""
        : await block.getByRole("textbox").inputValue();
    }
    const row = this.page.locator("tr", { hasText: settingName });
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
    // Value is in the third cell (after select radio and name)
    const cells = row.locator("td");
    const valueCell = cells.nth(3); // select, name, description, value
    return (await valueCell.textContent()) || "";
  }
}

/**
 * Which admin menu each boolean setting is edited on: the e-signature flag
 * lives in site identity, the patient sex and age rules in sample entry.
 */
const SETTING_MENU: Record<string, SettingsMenu> = {
  electronicSignatureEnabled: "SiteInformationMenu",
  "Patient sex required": "SampleEntryConfigurationMenu",
  "Patient age required": "SampleEntryConfigurationMenu",
};

/** Read a boolean site_information setting off its admin menu. */
export async function isSettingOn(
  page: Page,
  setting: string,
): Promise<boolean> {
  const menu = new SiteInformationPage(page, SETTING_MENU[setting]);
  await menu.goto();
  return /true/i.test(await menu.getSettingValue(setting));
}

/** Set a boolean site_information setting through its admin menu. */
export async function setSetting(
  page: Page,
  setting: string,
  on: boolean,
): Promise<void> {
  const menu = new SiteInformationPage(page, SETTING_MENU[setting]);
  await menu.goto();
  await menu.setBooleanSetting(setting, on);
}
