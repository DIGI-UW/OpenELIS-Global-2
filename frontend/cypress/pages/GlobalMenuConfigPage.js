class GlobalMenuConfigPage {
  constructor() {}

  // This method is used to visit the page
  visit() {
    cy.visit("/administration#globalMenuManagement");
  }

  turnOffToggleSwitch() {
    cy.get("div.cds--toggle__switch").click();
  }

  turnOnToggleSwitch() {
    cy.get("div.cds--toggle label div > div").should("be.visible").click();
  }

  checkMenuItem = function (menuItem) {
    // Map of menu items to their respective checkboxes
    const menuItems = {
      home: "#menu_home_checkbox",
      order: "#menu_sample_checkbox",
      immunoChem: "#menu_immunochem_checkbox",
      cytology: "#menu_cytology_checkbox",
      results: "#menu_results_checkbox",
      validation: "#menu_resultvalidation_checkbox",
      reports: "#menu_reports_checkbox",
      study: "#menu_reports_study_checkbox",
      billing: "#menu_billing_checkbox",
      admin: "#menu_administration_checkbox",
      help: "#menu_help_checkbox",
      patient: "#menu_patient_checkbox",
      nonConform: "#menu_nonconformity_checkbox",
      workplan: "#menu_workplan_checkbox",
      pathology: "#menu_pathology_checkbox",
    };

    // Get the corresponding checkbox selector based on the passed menuItem
    const checkboxSelector = menuItems[menuItem];

    if (checkboxSelector) {
      // Perform the check action, forcing it to check even if covered
      cy.get(checkboxSelector).check({ force: true });
    } else {
      // If no valid menuItem is passed, log an error
      cy.log("Invalid menu item");
    }
  };

  submitButton() {
    cy.contains("button", "Submit").click();
  }
}

export default GlobalMenuConfigPage;
