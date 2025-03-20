class NotificationConfigPage {
  
  verifyPageLoaded() {
    cy.url().should("include", "testNotificationConfigMenu");

    // Wait for content and confirm the page header
    cy.get(".adminPageContent", { timeout: 15000 }).should("be.visible");
    cy.contains("h1", "Test Notification Configuration", { timeout: 15000 }).should("be.visible");
  }

  
  clickEditButton() {
    cy.get("button", { timeout: 10000 }).contains("Edit").first().click();
  }

  
  verifyEditPageLoaded() {
    cy.get("input#subject", { timeout: 10000 }).should("be.visible");
    cy.get("textarea#message", { timeout: 10000 }).should("be.visible");
  }

  
  toggleNotificationOption(selector) {
    cy.get(selector, { timeout: 10000 }).click({ force: true });
  }

  
  verifyToggleStatus(selector, expectedStatus) {
    cy.get(selector, { timeout: 10000 }).should(expectedStatus ? "be.checked" : "not.be.checked");
  }

  
  editSubjectAndMessage(subject, message) {
    cy.get("input#subject", { timeout: 10000 }).clear().type(subject);
    cy.get("textarea#message", { timeout: 10000 }).clear().type(message);
  }

  
  verifySubjectAndMessage(subject, message) {
    cy.get("input#subject", { timeout: 10000 }).should("have.value", subject);
    cy.get("textarea#message", { timeout: 10000 }).should("have.value", message);
  }

  
  clickSaveButton() {
    cy.get("button", { timeout: 10000 }).contains("Save").click();
  }

 
  verifySuccessNotification() {
    cy.get(".notification", { timeout: 10000 })
      .should("be.visible")
      .and("contain.text", "success");
  }

  
  verifyErrorNotification() {
    cy.get(".notification", { timeout: 10000 })
      .should("be.visible")
      .and("contain.text", "error");
  }

  
  clickExitButton() {
    cy.get("button", { timeout: 10000 }).contains("Exit").click();
  }
}

export default new NotificationConfigPage();
