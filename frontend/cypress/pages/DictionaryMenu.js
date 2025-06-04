class DictionaryMenuPage {
  constructor() {
    this.selectors = {
      title: "h2",
      modal: ".cds--modal--container",
      add: "[data-cy='addButton']",
      dictCategory: "#downshift-1-toggle-button",
      dictNumber: "#dictNumber",
      dictEntry: "#dictEntry",
      isActive: "#downshift-3-toggle-button",
      localAbbreviation: "#localAbbrev",
      searchByDictEntry: "#dictionary-entry-search",
      cancelButton: ".cds--btn--secondary",
      addButton: ".cds--btn--primary",
      modify: "[data-cy='modifyButton']",
      deactivate: "[data-cy='deactivateButton']",
    };
  }

  verifyPageTitle() {
    cy.contains(this.selectors.title, "Dictionary Menu");
  }

  clickAddButton() {
    cy.get(this.selectors.add).click();
  }

  validateModal() {
    cy.get(this.selectors.modal).should("be.visible");
  }

  dictNumberDisabled() {
    cy.get(this.selectors.dictNumber).should("be.disabled");
  }

  dictCategory(value) {
    cy.get(this.selectors.dictCategory).select(value);
  }

  dictEntry(value) {
    cy.get(this.selectors.dictEntry).type(value);
  }

  isActive(value) {
    cy.get(this.selectors.isActive).select(value);
  }

  localAbbreviation(value) {
    cy.get(this.selectors.localAbbreviation).type(value);
  }

  clickCancelButton() {
    cy.contains(this.selectors.cancelButton, "Cancel").click();
  }

  clickAddButton() {
    cy.contains(this.selectors.addButton, "Add").click();
  }

  searchByDictionaryEntry(value){
    cy.get(this.selectors.searchByDictEntry).type(value);
  }

  validateDictEntry(value){
    cy.contains("td", value);
  }
}

export default DictionaryMenuPage;
