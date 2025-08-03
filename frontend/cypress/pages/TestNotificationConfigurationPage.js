class TestNotificationConfigurationPage {
  constructor() {
    this.selectors = {
      title: "h2",
      span: "span",
      carbonCopy: "#carbon-copy",
      save: "[data-cy='saveButton']",
      cancel: "[data-cy='cancelButton']",
    };
  }

  validatePageTitle() {
    cy.get(this.selectors.title)
      .should("be.visible")
      .and("contain.text", "Test Notification Configuration");
  }

  checkBoxes(boxName) {
    cy.get(`[for="checkbox-373-${boxName}"]`).click();
  }

  settingsCheckBoxes(boxName) {
    cy.get(`[for="${boxName}"]`).click();
  }

  settingsButton(buttonNum) {
    cy.get(`#tooltip-${buttonNum}`).click();
  }

  saveChanges() {
    cy.contains("button", "Save").should("be.enabled").click();
  }

  clickSave() {
    cy.get(this.selectors.save).should("be.enabled").click();
  }

  clickCancel() {
    cy.get(this.selectors.cancel).click();
  }

  enterSubject(index, text) {
    cy.get(`#subject-${index}`).clear().type(text);
  }

  enterMessage(index, text) {
    cy.get(`#subject-${index}`).clear().type(text);
  }

  enterBCC(text) {
    cy.get(this.selectors.carbonCopy).clear().type(text);
  }
}

export default TestNotificationConfigurationPage;
