class TestManagementPage {
  visitLogin() {
    cy.visit("/login");
  }

  login(username, password) {
    cy.get("#username").type(username);
    cy.get("#password").type(password);
    cy.get("button[type='submit']").click();
  }

  goToHomePage() {
    cy.url().should("include", "/home");
  }

  goToAdminPage() {
    cy.get("#menu_admin").click();
    cy.url().should("include", "/admin");
  }

  goToBarcodeConfigPage() {
    cy.get("#menu_barcode").click();
    cy.url().should("include", "/admin/barcode");
  }

  captureDefaultOrder() {
    cy.get("#defaultOrderLabel").clear().type("Default Order");
  }

  captureDefaultSpecimen() {
    cy.get("#defaultSpecimenLabel").clear().type("Default Specimen");
  }

  captureMaxOrder() {
    cy.get("#maxOrderLabel").clear().type("Max Order");
  }

  captureMaxSpecimen() {
    cy.get("#maxSpecimenLabel").clear().type("Max Specimen");
  }

  uncheckCheckBoxes() {
    cy.get("#optionalElements").uncheck();
    cy.get("#preprintedBarcode").uncheck();
  }

  checkCheckBoxes() {
    cy.get("#optionalElements").check();
    cy.get("#preprintedBarcode").check();
  }

  dimensionsBarCodeLabel() {
    cy.get("#barcodeWidth").clear().type("100");
    cy.get("#barcodeHeight").clear().type("50");
  }

  saveChanges() {
    cy.get("#saveButton").click();
  }
}

export default new TestManagementPage();
