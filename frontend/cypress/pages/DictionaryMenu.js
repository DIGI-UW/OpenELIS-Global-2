class DictionaryMenuPage {
  constructor() {
    this.selectors = {
      title: "h2",
      modal: ".cds--modal-container",
      add: "[data-cy='addButton']",
      dictCategory: "#downshift-1-toggle-button",
      dictNumber: "#dictNumber",
      dictEntry: "#dictEntry",
      dictValue: "div",
      notActive: "#downshift-3-item-1",
      active: "#downshift-3-item-0",
      isActive: "#isActive",
      localAbbreviation: "#localAbbrev",
      searchByDictEntry: "#dictionary-entry-search",
      cancelButton: ".cds--btn--secondary",
      updateButton: ".cds--btn--primary",
      addbutton: "div.cds--btn-set > button.cds--btn--primary",
      modify: "[data-cy='modifyButton']",
      deactivate: "[data-cy='deactivateButton']",
      checkBox: "td.cds--table-column-checkbox",
    };
  }

  verifyPageTitle() {
    cy.contains(this.selectors.title, "Dictionary Menu");
  }

  clickAddButton() {
    cy.get(this.selectors.add).click();
  }

  clickModifyButton() {
    cy.get(this.selectors.modify).click();
    cy.wait(2000);
  }

  clickUpdateButton() {
    cy.contains(this.selectors.updateButton, "Update").click();
  }

  clickDeactivateButton() {
    cy.get(this.selectors.deactivate).should("be.enabled").click();
  }

  addButton() {
    cy.get(this.selectors.addbutton).click();
    cy.wait(2000);
  }

  validateModal() {
    cy.get(this.selectors.modal).should("exist");
  }

  dictNumberDisabled() {
    cy.get(this.selectors.dictNumber).should("be.disabled");
  }

  dictCategory(value) {
    cy.get(this.selectors.dictCategory).click();
    cy.contains(this.selectors.dictValue, value).click();
  }

  dictEntry(value) {
    cy.get(this.selectors.dictEntry).clear().type(value);
  }

  isActive(value) {
    cy.get(this.selectors.isActive).click();
    cy.contains(this.selectors.active, value).click();
  }

  notActive(value) {
    cy.get(this.selectors.isActive).click();
    cy.contains(this.selectors.notActive, value).click();
  }

  localAbbreviation(value) {
    cy.get(this.selectors.localAbbreviation).clear().type(value);
  }

  clickCancelButton() {
    cy.contains(this.selectors.cancelButton, "Cancel").click();
  }

  clickAddButton() {
    cy.contains(this.selectors.add, "Add").click();
  }

  searchByDictionaryEntry(value) {
    cy.get(this.selectors.searchByDictEntry).clear().type(value);
  }

  clearSearch() {
    cy.get(this.selectors.searchByDictEntry).clear();
  }

  validateDictEntry(value) {
    cy.get("tr td").eq(2).should("contain", value);
  }

  validateDictStatus(value) {
    cy.get("tr td").eq(4).should("contain", value);
  }

  checkFirstDict() {
    cy.get(this.selectors.checkBox).first().click();
  }
}

export default DictionaryMenuPage;
