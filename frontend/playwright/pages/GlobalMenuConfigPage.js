class GlobalMenuConfigPage {
  constructor(page) {
    this.page = page;
  }

  // Navigate to the Global Menu Configuration Page
  async visit() {
    await this.page.goto('/administration#globalMenuManagement');
  }

  async turnOffToggleSwitch() {
    await this.page.locator('div.cds--toggle__switch').click();
  }

  async turnOnToggleSwitch() {
    const toggleSwitch = this.page.locator('div.cds--toggle label div > div');
    await toggleSwitch.waitFor();
    await toggleSwitch.click();
  }

  async checkMenuItem(menuItem) {
    // Map of menu items to their respective checkboxes
    const menuItems = {
      home: '#menu_home_checkbox',
      order: '#menu_sample_checkbox',
      immunoChem: '#menu_immunochem_checkbox',
      cytology: '#menu_cytology_checkbox',
      results: '#menu_results_checkbox',
      validation: '#menu_resultvalidation_checkbox',
      reports: '#menu_reports_checkbox',
      study: '#menu_reports_study_checkbox',
      billing: '#menu_billing_checkbox',
      admin: '#menu_administration_checkbox',
      help: '#menu_help_checkbox',
      patient: '#menu_patient_checkbox',
      nonConform: '#menu_nonconformity_checkbox',
      workplan: '#menu_workplan_checkbox',
      pathology: '#menu_pathology_checkbox',
    };

    // Get the corresponding checkbox selector based on the passed menuItem
    const checkboxSelector = menuItems[menuItem];

    if (checkboxSelector) {
      // Perform the check action, forcing it to check even if covered
      await this.page.locator(checkboxSelector).check({ force: true });
    } else {
      console.error('Invalid menu item:', menuItem);
    }
  }

  async submitButton() {
    await this.page.getByRole('button', { name: 'Submit' }).click();
  }
}

export default GlobalMenuConfigPage;
