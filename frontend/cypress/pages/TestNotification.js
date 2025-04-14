class NotificationConfigPage {
  verifyPageLoaded() {
    cy.contains("Test Notification Configuration").should("be.visible");
  }

  clickEditButton() {
    cy.contains("button", "Edit").click();
  }

  verifyEditPageLoaded() {
    cy.contains("Edit Test Notification Configuration").should("be.visible");
  }

  toggleNotificationOption(selector) {
    cy.get(selector).click({ force: true });
  }

  verifyToggleStatus(selector, expectedState) {
    cy.get(selector).should(expectedState ? "be.checked" : "not.be.checked");
  }

  editSubjectAndMessage(subject, message) {
    cy.get("#subject").clear().type(subject);
    cy.get("#message").clear().type(message);
  }

  verifySubjectAndMessage(subject, message) {
    cy.get("#subject").should("have.value", subject);
    cy.get("#message").should("have.value", message);
  }

  clickSaveButton() {
    cy.contains("button", "Save").click();
  }

  clickExitButton() {
    cy.contains("button", "Exit").click();
  }

  verifySuccessNotification() {
    cy.contains("Saved successfully").should("be.visible");
  }

  verifyErrorNotification() {
    cy.contains("Error saving configuration").should("be.visible");
  }
}

export default NotificationConfigPage;
