class NotificationConfigPage {
  visitMenu() {
    cy.visit("/MasterListsPage#testNotificationConfigMenu");
  }

  verifyMenuLoaded() {
    cy.get(".adminPageContent").should("be.visible");
    cy.contains("Test Notification Configuration").should("be.visible");
  }

  navigateToEditPage() {
    cy.get("button").contains("Edit").first().click();
    cy.url().should("include", "/TestNotificationConfigEdit");
  }

  verifyEditPageLoaded() {
    cy.get("input#subject").should("be.visible");
    cy.get("textarea#message").should("be.visible");
  }

  toggleNotification(optionId) {
    const selector = `input#${optionId}`;
    cy.get(selector).check().should("be.checked");
    cy.get(selector).uncheck().should("not.be.checked");
  }

  editSubjectAndMessage(subject, message) {
    cy.get("button").contains("Edit").click();
    cy.get("#subject").clear().type(subject);
    cy.get("#message").clear().type(message);
  }

  saveConfig() {
    cy.get("button").contains("Save").click();
  }

  verifySuccessMessage() {
    cy.contains("Configuration saved successfully").should("be.visible");
  }

  verifyErrorMessage() {
    cy.contains("Error saving configuration").should("be.visible");
  }

  cancelAndReturnToMenu() {
    cy.get("button").contains("Exit").click();
    cy.url().should("include", "testNotificationConfigMenu");
  }

  handleEmptyInput() {
    cy.get("#subject").clear();
    cy.get("#message").clear();
    cy.get("button").contains("Save").click();
  }

  verifyEmptyInputError() {
    cy.contains("Subject and message cannot be empty").should("be.visible");
  }

  handleLongInput(longText) {
    cy.get("#subject").clear().type(longText);
    cy.get("#message").clear().type(longText);
    cy.get("button").contains("Save").click();
  }

  verifyLongInputError() {
    cy.contains("Input too long").should("be.visible");
  }

  resetConfig() {
    cy.get("button").contains("Reset").click();
  }

  verifyDefaultValues(subject, message) {
    cy.get("#subject").should("have.value", subject);
    cy.get("#message").should("have.value", message);
  }
}

export default new NotificationConfigPage();
