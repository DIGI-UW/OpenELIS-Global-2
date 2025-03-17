import NotificationConfigPage from "../pages/TestNotification";

describe("Test Notification Config E2E with Page Object Model", () => {
  before(() => {
    console.log(NotificationConfigPage);
  });

  beforeEach(() => {
    cy.visit("/MasterListsPage#testNotificationConfigMenu");
  });

  it("Loads the Notification Config Menu", () => {
    notificationConfigPage.verifyPageLoaded();
  });

  it("Navigates to the Edit Page", () => {
    notificationConfigPage.clickEditButton();
    cy.url().should("include", "/TestNotificationConfigEdit");
  });

  it("Loads Config Data in Edit Page", () => {
    notificationConfigPage.clickEditButton();
    notificationConfigPage.verifyEditPageLoaded();
  });

  it("Enables and Disables Notifications", () => {
    notificationConfigPage.clickEditButton();
    notificationConfigPage.toggleNotificationOption("#patientEmail");
    notificationConfigPage.toggleNotificationOption("#patientEmail");
  });

  it("Edits Subject and Message Templates", () => {
    notificationConfigPage.clickEditButton();
    notificationConfigPage.editSubjectAndMessage("New Subject", "New Message");
  });

  it("Saves Notification Config", () => {
    notificationConfigPage.clickEditButton();
    notificationConfigPage.clickSaveButton();
    notificationConfigPage.verifySuccessNotification();
  });

  it("Handles Errors on Save", () => {
    cy.intercept("POST", "/rest/TestNotificationConfig", { statusCode: 500 });
    notificationConfigPage.clickEditButton();
    notificationConfigPage.clickSaveButton();
    notificationConfigPage.verifyErrorNotification();
  });

  it("Cancels and Returns to Menu", () => {
    notificationConfigPage.clickEditButton();
    notificationConfigPage.clickExitButton();
    cy.url().should("include", "testNotificationConfigMenu");
  });
});
