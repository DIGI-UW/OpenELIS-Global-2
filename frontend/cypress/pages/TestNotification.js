class NotificationConfigPage {
  verifyPageLoaded() {
    cy.url().should("include", "testNotificationConfigMenu");
    cy.get(".adminPageContent", { timeout: 10000 }).should("be.visible");
    cy.contains("h1", "Test Notification Configuration").should("be.visible");
  }

  clickEditButton() {
    cy.get("button").contains("Edit").first().click();
  }

  verifyEditPageLoaded() {
    cy.get("input#subject").should("be.visible");
    cy.get("textarea#message").should("be.visible");
  }

  toggleNotificationOption(selector) {
    cy.get(selector).click({ force: true });
  }

  verifyToggleStatus(selector, expectedStatus) {
    cy.get(selector).should(expectedStatus ? "be.checked" : "not.be.checked");
  }

  editSubjectAndMessage(subject, message) {
    cy.get("input#subject").clear().type(subject);
    cy.get("textarea#message").clear().type(message);
  }

  verifySubjectAndMessage(subject, message) {
    cy.get("input#subject").should("have.value", subject);
    cy.get("textarea#message").should("have.value", message);
  }

  clickSaveButton() {
    cy.get("button").contains("Save").click();
  }

  verifySuccessNotification() {
    cy.get(".notification").should("be.visible").and("contain.text", "success");
  }

  verifyErrorNotification() {
    cy.get(".notification").should("be.visible").and("contain.text", "error");
  }

  clickExitButton() {
    cy.get("button").contains("Exit").click();
  }
}

export default new NotificationConfigPage();
