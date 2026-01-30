class GeneralConfigurationsPage {
  constructor() {
    this.selectors = {
      title: "h2",
      modifyButton: "[data-cy='modify-Button']",
      radioButton: ".cds--radio-button__label",
      saveButton: "[data-cy='save-Button']",
      exitButton: "[data-cy='exit-Button']",
      textInPut: "#textInput",
    };
  }

  validatePageTitle(value) {
    cy.get(this.selectors.title)
      .should("be.visible")
      .and("contain.text", value);
  }

  selectItem() {
    cy.get(this.selectors.radioButton).first().click();
  }

  clickModifyButton() {
    cy.get(this.selectors.modifyButton).click();
  }

  checkValue(value) {
    cy.contains("span", value).click();
  }

  saveChanges() {
    cy.get(this.selectors.saveButton).click();
    cy.wait(2000);
  }

  exitChanges() {
    cy.get(this.selectors.exitButton).click();
  }

  validateStatus(value) {
    cy.get("tr td").eq(3).should("contain", value);
  }

  typeValue(value) {
    cy.get(this.selectors.textInPut).clear().type(value);
  }
}

export default GeneralConfigurationsPage;
