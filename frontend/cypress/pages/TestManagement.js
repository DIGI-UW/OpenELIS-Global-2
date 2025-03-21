class TestManagementPage {
<<<<<<< Updated upstream
  // Visit URLs
  visitCombinedPage() {
    cy.visit("/combined-page");
=======
  visitTestCatalog() {
    cy.visit("/MasterListsPage#TestCatalog");
>>>>>>> Stashed changes
  }

  getToggleButton() {
    return cy.get("#toggle");
  }

  getGuideSection() {
    return cy.get(".structured-list-wrapper");
  }

  getTestSectionMultiSelect() {
    return cy.get("#carbon-multiselect-example-3");
  }

  selectTestSection(section) {
    this.getTestSectionMultiSelect().click();
    cy.contains(section).click();
  }

  getSelectedTags() {
    return cy.get('[type="cyan"]');
  }

  getTabs() {
    return cy.get('[role="tablist"] button');
  }

  selectTab(section) {
    cy.contains('[role="tab"]', section).click();
  }

  getDataTable() {
    return cy.get(".bx--data-table");
  }

<<<<<<< Updated upstream
  openAddMethodModal() {
    cy.get("button").contains("Add Method").click();
=======
  getBreadcrumb() {
    return cy.get(".breadcrumb");
  }

  visitManageMethod() {
    cy.visit("/MasterListsPage#MethodManagment");
  }

  openAddMethodModal() {
    cy.get("button").contains("Add New Method").click();
>>>>>>> Stashed changes
  }

  fillMethodForm(english, french) {
    cy.get("#englishLabel").clear().type(english);
    cy.get("#frenchLabel").clear().type(french);
  }

  submitForm() {
    cy.get("button").contains("Save").click();
  }

  confirmAddMethod() {
    cy.get("button").contains("Accept").click();
  }

  getExistingMethods() {
    return cy.get("h4").contains("Existing Methods").parent().find("div > div");
  }

  getInactiveMethods() {
    return cy.get("h4").contains("Inactive Methods").parent().find("div > div");
  }
<<<<<<< Updated upstream

  verifyTestCatalogPage() {
    this.getToggleButton().should("be.visible");
    this.getTestSectionMultiSelect().should("be.visible");
    this.getTabs().should("be.visible");
  }

  verifyManageMethodPage() {
    cy.contains("Manage Method").should("be.visible");
    this.getExistingMethods().should("have.length.greaterThan", 0);
    this.getInactiveMethods().should("have.length.greaterThan", 0);
  }
}

export default TestManagementPage;
=======
}

export default new TestManagementPage();
>>>>>>> Stashed changes
