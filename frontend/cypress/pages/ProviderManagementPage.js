class ProviderManagementPage {
  constructor() {
    this.selectors = {
      addButton: "[data-cy='add-Button']",
      modifyButton: "[data-cy='modify-Button']",
      deactivateButton: "[data-cy='deactivate-Button']",
      isActive: "#downshift-1-toggle-button",
      modifyStatus: "#downshift-3-toggle-button",
      activeStatus: ".cds--list-box__menu-item__option",
      lastNameInput: "#lastName",
      firstNameInput: "#firstName",
      activeDropdown: "#isActive",
      provderSearchBar: "#provider-search-bar",
      modalAddButton: "div.cds--modal button:contains('Add')",
      updateButton: "div.cds--modal button:contains('Update')",
    };
  }

  clickAddProviderButton() {
    cy.get(this.selectors.addButton).should("be.visible").click();
    cy.wait(200);
  }

  enterProviderLastName(value) {
    cy.get(this.selectors.lastNameInput).type(value);
  }

  enterProviderFirstName(value) {
    cy.get(this.selectors.firstNameInput).type(value);
  }

  activeStatus(value) {
    cy.get(this.selectors.isActive).click();
    cy.contains(this.selectors.activeStatus, value).click();
  }

  modifyStatus(value) {
    cy.get(this.selectors.modifyStatus).click();
    cy.contains(this.selectors.activeStatus, value).click();
  }

  addProvider() {
    cy.get(this.selectors.modalAddButton).click();
    cy.wait(200);
  }

  updateProvider() {
    cy.get(this.selectors.updateButton).click();
    cy.wait(200);
  }

  searchProvider(value) {
    cy.get(this.selectors.provderSearchBar).clear().type(value);
  }

  confirmProvider(value) {
    // Testing Roadmap: Use element readiness checks, wait for table to render
    // Wait for table to be visible first, then find the value
    cy.get("table", { timeout: 10000 }).should("be.visible");
    cy.get("table tbody tr", { timeout: 10000 })
      .should("have.length.greaterThan", 0)
      .contains("td", value)
      .should("exist")
      .should("be.visible");
  }

  checkProvider(value) {
    cy.contains("td", value).should("exist").click();
  }

  modifyProvider() {
    cy.get(this.selectors.modifyButton).click();
  }

  deactivateProvider() {
    cy.get(this.selectors.deactivateButton).click();
  }
}

export default ProviderManagementPage;
