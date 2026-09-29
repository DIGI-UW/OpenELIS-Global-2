class MenuConfigPage {
  constructor() {
    this.selectors = {
      menuButton: "[data-cy='menuButton']",
      nonConformMenu: "#menu_nonconformity",
      nonConformCheck: "Non-Conformity Menu Active",
      nonConformReport: "#menu_non_conforming_report",
      nonConformView: "#menu_non_conforming_view",
      correctiveAction: "#menu_non_conforming_corrective_actions",
      patientMenu: "#menu_patient",
      addEditPatient: "#menu_patient_add_or_edit",
      patientHistory: "#menu_patienthistory",
      studyPatient: "#menu_patient_create",
      patientCheck: "Patient Menu Active",
      toggleText: ".cds--toggle__text",
      toggleOn: "div.cds--toggle__switch",
    };
  }

  // This method is used to visit the page
  visit() {
    cy.visit("/administration#globalMenuManagement");
  }

  navigateToMainMenu() {
    cy.wait(5000);
    cy.get("body").then(($body) => {
      if ($body.find("[data-testid='admin-back-to-main-nav']").length > 0) {
        cy.get("[data-testid='admin-back-to-main-nav']").click();
        cy.location("pathname").should("eq", "/Dashboard");
      }
    });
    cy.get("body").then(($body) => {
      if ($body.find(".cds--side-nav--expanded").length === 0) {
        cy.get(this.selectors.menuButton).click();
      }
    });
  }

  turnOnToggleSwitch() {
    cy.get(this.selectors.toggleOn)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
  }

  validateToggleStatus(value) {
    cy.contains(this.selectors.toggleText, value);
  }

  uncheckNonConform() {
    cy.contains(".cds--checkbox-label-text", this.selectors.nonConformCheck)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
  }

  validateNonConformOff() {
    cy.get(this.selectors.nonConformMenu).should("not.exist");
  }

  validateNonConformOn() {
    cy.get(this.selectors.nonConformMenu)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get(this.selectors.nonConformReport).should("exist");
    cy.get(this.selectors.nonConformView).should("exist");
    cy.get(this.selectors.correctiveAction).should("exist");
  }

  uncheckPatientMenu() {
    cy.contains(".cds--checkbox-label-text", this.selectors.patientCheck)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
  }

  validatePatientMenuOff() {
    cy.get(this.selectors.patientMenu).should("not.exist");
  }

  validatePatientMenuOn() {
    cy.get(this.selectors.patientMenu)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
    cy.get(this.selectors.addEditPatient).should("exist");
    cy.get(this.selectors.patientHistory).should("exist");
    cy.get(this.selectors.studyPatient).should("exist");
  }

  submitButton() {
    cy.contains("button", "Submit")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
  }

  checkMenuItem = function (menuItem) {
    // Map of menu items to their respective checkboxes
    const menuItems = {
      addEditPatient: "#menu_patient_add_or_edit_checkbox",
      patientHistory: "#menu_patienthistory_checkbox",
      studyPatient: "#menu_patient_create_checkbox",
      correctiveAction: "#menu_non_conforming_corrective_actions_checkbox",
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
}

export default MenuConfigPage;
