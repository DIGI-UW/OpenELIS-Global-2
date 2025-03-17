class NotificationConfigPage {
  verifyPageLoaded() {
    cy.get(".adminPageContent").should("be.visible");
    cy.get("h1")
      .contains("Test Notification Configuration")
      .should("be.visible");
  }

  clickEditButton() {
    cy.get("button").contains("Edit").first().click();
  }

  verifyEditPageLoaded() {
    cy.get("input#subject").should("be.visible");
    cy.get("textarea#message").should("be.visible");
  }

  toggleNotificationOption(selector) {
    cy.get(selector).check().should("be.checked");
    cy.get(selector).uncheck().should("not.be.checked");
  }

  editSubjectAndMessage(subject, message) {
    cy.get("input#subject").clear().type(subject);
    cy.get("textarea#message").clear().type(message);
  }

  clickSaveButton() {
    cy.get("button").contains("Save").click();
  }

  verifySuccessNotification() {
    cy.get(".notification").should("contain", "success");
  }

  verifyErrorNotification() {
    cy.get(".notification").should("contain", "error");
  }

  clickExitButton() {
    cy.get("button").contains("Exit").click();
  }
}

export default new NotificationConfigPage();
