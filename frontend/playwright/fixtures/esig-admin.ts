/**
 * Page Object for Admin > Site Information configuration toggle.
 *
 * Used by e-sig E2E tests to enable/disable the electronic signature
 * feature via the admin UI — the same way an admin would.
 */
import { Page, expect } from "@playwright/test";
import { UI_TIMEOUT, NAV_TIMEOUT } from "../helpers/timeouts";

export class SiteInformationPage {
  constructor(private page: Page) {}

  /** Navigate to Admin > Site Information */
  async goto() {
    await this.page.goto("/MasterListsPage/SiteInformationMenu", {
      waitUntil: "domcontentloaded",
    });
    await expect(
      this.page.getByRole("heading", { name: /site information/i }),
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
  async setBooleanSetting(settingName: string, value: boolean) {
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
      this.page.getByRole("button", { name: "Modify", exact: true }),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
  }

  /** Read the current value of a setting from the table. */
  async getSettingValue(settingName: string): Promise<string> {
    const row = this.page.locator("tr", { hasText: settingName });
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
    // Value is in the third cell (after select radio and name)
    const cells = row.locator("td");
    const valueCell = cells.nth(3); // select, name, description, value
    return (await valueCell.textContent()) || "";
  }
}
