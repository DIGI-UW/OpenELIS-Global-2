import NotificationConfigPage from "../pages/TestNotification";

describe("Test Notification Config E2E with Page Object Model", () => {
  before(() => {
    console.log(NotificationConfigPage);
  });

  beforeEach(() => {
    cy.visit("/MasterListsPage#testNotificationConfigMenu");
  });

  it("Loads the Notification Config Menu", () => {
    NotificationConfigPage.verifyPageLoaded();
  });

  it("Navigates to the Edit Page", () => {
    NotificationConfigPage.clickEditButton();
    cy.url().should("include", "/TestNotificationConfigEdit");
  });

  it("Loads Config Data in Edit Page", () => {
    NotificationConfigPage.clickEditButton();
    NotificationConfigPage.verifyEditPageLoaded();
  });

  it("Enables and Disables Notifications", () => {
    NotificationConfigPage.clickEditButton();
    NotificationConfigPage.toggleNotificationOption("#patientEmail");
    NotificationConfigPage.toggleNotificationOption("#patientEmail");
  });

  it("Edits Subject and Message Templates", () => {
    NotificationConfigPage.clickEditButton();
    NotificationConfigPage.editSubjectAndMessage("New Subject", "New Message");
  });

  it("Saves Notification Config", () => {
    NotificationConfigPage.clickEditButton();
    NotificationConfigPage.clickSaveButton();
    NotificationConfigPage.verifySuccessNotification();
  });

  it("Handles Errors on Save", () => {
    cy.intercept("POST", "/rest/TestNotificationConfig", { statusCode: 500 });
    NotificationConfigPage.clickEditButton();
    NotificationConfigPage.clickSaveButton();
    NotificationConfigPage.verifyErrorNotification();
  });

  it("Cancels and Returns to Menu", () => {
    NotificationConfigPage.clickEditButton();
    NotificationConfigPage.clickExitButton();
    cy.url().should("include", "testNotificationConfigMenu");
  });
});
