class NonComformMenuPage {
  constructor() {}

  // This method is used to visit the page
  visit() {
    cy.visit("/administration#nonConformityMenuManagement");
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
      ReportNonComformingEvent: "#menu_non_conforming_report",
      ViewNewNonComformingEvent: "#menu_non_conforming_view",
      CorrectiveActions: "#menu_non_conforming_corrective_actions",
      NonComformMenuActive: "#menu_nonconformity",
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

export default NonComformMenuPage;
