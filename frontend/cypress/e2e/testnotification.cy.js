import NotificationConfigPage from "../pages/TestNotification";

describe("Test Notification Config E2E with Page Object Model", () => {
  before(() => {
    cy.log("Starting Notification Config E2E Tests 🚀");
  });

  beforeEach(() => {
    cy.visit("/MasterListsPage#testNotificationConfigMenu");
    NotificationConfigPage.verifyPageLoaded();
  });

  it("Loads the Notification Config Menu", () => {
    NotificationConfigPage.verifyPageLoaded();
  });

  it("Navigates to the Edit Page", () => {
    NotificationConfigPage.clickEditButton();
    cy.url().should("include", "/TestNotificationConfigEdit");
    NotificationConfigPage.verifyEditPageLoaded();
  });

  it("Loads Config Data in Edit Page", () => {
    NotificationConfigPage.clickEditButton();
    NotificationConfigPage.verifyEditPageLoaded();
  });

  it("Enables and Disables Notifications", () => {
    NotificationConfigPage.clickEditButton();
    NotificationConfigPage.verifyEditPageLoaded();

    // Enable and verify
    NotificationConfigPage.toggleNotificationOption("#patientEmail");
    NotificationConfigPage.verifyToggleStatus("#patientEmail", true);

    // Disable and verify
    NotificationConfigPage.toggleNotificationOption("#patientEmail");
    NotificationConfigPage.verifyToggleStatus("#patientEmail", false);
  });

  it("Edits Subject and Message Templates", () => {
    NotificationConfigPage.clickEditButton();
    NotificationConfigPage.verifyEditPageLoaded();

    const subject = "New Subject";
    const message = "New Message";

    NotificationConfigPage.editSubjectAndMessage(subject, message);
    NotificationConfigPage.verifySubjectAndMessage(subject, message);
  });

  it("Saves Notification Config", () => {
    cy.intercept("POST", "/rest/TestNotificationConfig").as("saveConfig");

    NotificationConfigPage.clickEditButton();
    NotificationConfigPage.verifyEditPageLoaded();

    NotificationConfigPage.clickSaveButton();
    cy.wait("@saveConfig").its("response.statusCode").should("eq", 200);

    NotificationConfigPage.verifySuccessNotification();
  });

  it("Handles Errors on Save", () => {
    cy.intercept("POST", "/rest/TestNotificationConfig", {
      statusCode: 500,
    }).as("saveConfigError");

    NotificationConfigPage.clickEditButton();
    NotificationConfigPage.verifyEditPageLoaded();

    NotificationConfigPage.clickSaveButton();
    cy.wait("@saveConfigError").its("response.statusCode").should("eq", 500);
    NotificationConfigPage.verifyErrorNotification();
  });

  it("Cancels and Returns to Menu", () => {
    NotificationConfigPage.clickEditButton();
    NotificationConfigPage.verifyEditPageLoaded();
    NotificationConfigPage.clickExitButton();
    cy.url().should("include", "testNotificationConfigMenu");
  });
});
