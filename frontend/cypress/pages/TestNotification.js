class NotificationConfigPage {
  verifyPageLoaded() {
    // Wait for the page URL to ensure we’re on the right page
    cy.url().should("include", "testNotificationConfigMenu");

    // Handle possible loading spinner
    cy.get(".loading-spinner", { timeout: 10000 }).should("not.exist");

    // Double-check for iframe or shadow DOM
    cy.document().then((doc) => {
      if (doc.querySelector("iframe")) {
        cy.get("iframe").its("0.contentDocument.body").should("not.be.empty");
      }
    });

    // Improved container check with better timeout
    cy.get("body", { timeout: 10000 }).then(($body) => {
      if ($body.find(".adminPageContent").length) {
        cy.get(".adminPageContent").should("be.visible");
      } else {
        throw new Error("adminPageContent not found");
      }
    });

    // Confirm the header is visible
    cy.contains("h1", "Test Notification Configuration", {
      timeout: 10000,
    }).should("be.visible");
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
