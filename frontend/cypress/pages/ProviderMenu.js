class ProviderMenuPage {
  get pageHeading() {
    return cy.get("h1");
  }
  get searchInput() {
    return cy.get("input#provider-search-bar");
  }
  get addButton() {
    return cy.contains("Add");
  }
  get modifyButton() {
    return cy.contains("Modify");
  }
  get deactivateButton() {
    return cy.contains("Deactivate");
  }
  get lastNameInput() {
    return cy.get("input#lastName");
  }
  get firstNameInput() {
    return cy.get("input#firstName");
  }
  get telephoneInput() {
    return cy.get("input#telephone");
  }
  get faxInput() {
    return cy.get("input#fax");
  }
  get activeDropdown() {
    return cy.get("div.dropdown-list");
  }
  get tableRows() {
    return cy.get("table tbody tr");
  }
  get paginationNextButton() {
    return cy.contains("Next");
  }
  get paginationPreviousButton() {
    return cy.contains("Previous");
  }

  openAddModal() {
    this.addButton.click();
  }

  closeAddModal() {
    cy.get('button[aria-label="Cancel"]').click();
  }

  openUpdateModal(providerId) {
    cy.get(`tr[data-id="${providerId}"]`).click();
  }

  closeUpdateModal() {
    cy.get('button[aria-label="Cancel"]').click();
  }

  addProvider(lastName, firstName, telephone, fax, isActive) {
    this.lastNameInput.type(lastName);
    this.firstNameInput.type(firstName);
    this.telephoneInput.type(telephone);
    this.faxInput.type(fax);
    this.activeDropdown.select(isActive);
    cy.get('button[type="submit"]').click();
  }

  updateProvider(lastName, firstName, telephone, fax, isActive) {
    this.lastNameInput.clear().type(lastName);
    this.firstNameInput.clear().type(firstName);
    this.telephoneInput.clear().type(telephone);
    this.faxInput.clear().type(fax);
    this.activeDropdown.select(isActive);
    cy.get('button[type="submit"]').click();
  }

  searchProvider(searchTerm) {
    this.searchInput.type(searchTerm);
    cy.wait(1000);
  }

  selectRowById(providerId) {
    cy.get(`tr[data-id="${providerId}"]`).click();
  }

  verifyTableRowContent(row, expectedValues) {
    row.find("td").each((cell, index) => {
      cy.wrap(cell).should("contain.text", expectedValues[index]);
    });
  }

  navigateToNextPage() {
    this.paginationNextButton.click();
  }

  navigateToPreviousPage() {
    this.paginationPreviousButton.click();
  }
}

export default new ProviderMenuPage();
