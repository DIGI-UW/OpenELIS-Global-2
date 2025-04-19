class TestNotificationPage {
  verifyPageLoaded() {
    cy.get("h2").should("contain.text", "Notification Configuration");
  }

  verifyEditPageLoaded() {
    cy.get("h2").should("contain.text", "Edit Notification Configuration");
  }

  // Action methods
  clickEditButton() {
    cy.get("[data-testid='edit-button']").should("be.visible").click();
  }

  clickSaveButton() {
    cy.get("[data-testid='save-button']").should("be.visible").click();
  }

  clickExitButton() {
    cy.get("[data-testid='exit-button']").should("be.visible").click();
  }

  toggleNotificationOption(selector) {
    cy.get(selector).should("be.visible").click();
  }

  verifyToggleStatus(selector, expectedStatus) {
    if (expectedStatus) {
      cy.get(selector).should("be.checked");
    } else {
      cy.get(selector).should("not.be.checked");
    }
  }

  editSubjectAndMessage(subject, message) {
    cy.get("[data-testid='subject-input']")
      .should("be.visible")
      .clear()
      .type(subject);
    cy.get("[data-testid='message-input']")
      .should("be.visible")
      .clear()
      .type(message);
  }

  verifySubjectAndMessage(subject, message) {
    cy.get("[data-testid='subject-input']").should("have.value", subject);
    cy.get("[data-testid='message-input']").should("have.value", message);
  }

  verifySuccessNotification() {
    cy.get(".notification-success").should("be.visible");
    // Alternative: cy.contains("Configuration saved successfully").should("be.visible");
  }

  verifyErrorNotification() {
    cy.get(".notification-error").should("be.visible");
    // Alternative: cy.contains("Error saving configuration").should("be.visible");
  }
}

export default TestNotificationPage;
