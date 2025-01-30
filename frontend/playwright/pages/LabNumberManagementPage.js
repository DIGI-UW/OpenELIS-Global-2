import { expect } from '@playwright/test';

class LabNumberManagementPage {
  constructor(page) {
    this.page = page;
  }

  // Verify the page is loaded by checking for a unique element
  async verifyPageLoaded() {
    await expect(this.page.locator('text=Lab Number Management')).toBeVisible();
  }

  // Select the lab number type from a dropdown
  async selectLabNumber(labNumberType) {
    const dropdown = this.page.locator('#lab_number_type');
    await expect(dropdown).toBeVisible(); // Ensure the dropdown is visible
    await dropdown.selectOption({ label: labNumberType }); // Select the lab number type passed as an argument
  }

  // Check the checkbox
  async checkPrefixCheckBox() {
    const checkbox = this.page.locator('#usePrefix');
    await checkbox.check({ force: true }); // Check the checkbox
  }

  // Type the prefix after checking the checkbox
  async typePrefix(prefix) {
    await this.checkPrefixCheckBox();
    
    const input = this.page.locator('#alphanumPrefix');
    await expect(input).not.toBeDisabled(); // Ensure the input is enabled
    await input.type(prefix);
  }

  // Click the submit button
  async clickSubmitButton() {
    const submitButton = this.page.locator("button.cds--btn.cds--btn--primary[type='submit']");
    await expect(submitButton).toBeVisible(); // Ensure the button is visible
    await submitButton.click(); // Click the submit button
  }
}

export default LabNumberManagementPage;
