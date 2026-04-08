class ProviderManagementPage {
  constructor() {
    this.selectors = {
      addButton: "[data-cy='add-Button']",
      modifyButton: "[data-cy='modify-Button']",
      deactivateButton: "[data-cy='deactivate-Button']",
      addIsActive: "#addIsActive .cds--list-box__field",
      modifyStatus: "#updateIsActive .cds--list-box__field",
      activeStatus: ".cds--list-box__menu-item__option",
      lastNameInput: "#addLastName",
      firstNameInput: "#addFirstName",
      providerSearchBar: "#provider-search-bar",
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
    cy.get(this.selectors.addIsActive).should("be.visible").click();
    cy.contains(this.selectors.activeStatus, value).click();
  }

  modifyStatus(value) {
    cy.get(this.selectors.modifyStatus).should("be.visible").click();
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
    cy.get(this.selectors.providerSearchBar).clear().type(value);
  }

  confirmProvider(value) {
    cy.contains("td", value).should("exist");
    cy.wait(200);
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
