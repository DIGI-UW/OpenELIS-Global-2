import notificationConfigPage from "../pages/notificationConfigPage";

describe("Test Notification Config E2E with Page Object Model", () => {
  const longText = "A".repeat(1001);

  beforeEach(() => {
    notificationConfigPage.visitMenu();
  });

  it("Loads the Notification Config Menu", () => {
    notificationConfigPage.verifyMenuLoaded();
  });

  it("Navigates to the Edit Page", () => {
    notificationConfigPage.navigateToEditPage();
  });

  it("Loads Config Data in Edit Page", () => {
    notificationConfigPage.navigateToEditPage();
    notificationConfigPage.verifyEditPageLoaded();
  });

  it("Enables and Disables Notifications", () => {
    notificationConfigPage.navigateToEditPage();
    notificationConfigPage.toggleNotification("patientEmail");
  });

  it("Edits and Saves Subject and Message", () => {
    notificationConfigPage.navigateToEditPage();
    notificationConfigPage.editSubjectAndMessage("New Subject", "New Message");
    notificationConfigPage.saveConfig();
    notificationConfigPage.verifySuccessMessage();
  });

  it("Handles Errors on Save", () => {
    cy.intercept("POST", "/rest/TestNotificationConfig", { statusCode: 500 });
    notificationConfigPage.navigateToEditPage();
    notificationConfigPage.saveConfig();
    notificationConfigPage.verifyErrorMessage();
  });

  it("Cancels and Returns to Menu", () => {
    notificationConfigPage.navigateToEditPage();
    notificationConfigPage.cancelAndReturnToMenu();
  });

  it("Shows Error for Empty Inputs", () => {
    notificationConfigPage.navigateToEditPage();
    notificationConfigPage.handleEmptyInput();
    notificationConfigPage.verifyEmptyInputError();
  });

  it("Handles Long Inputs", () => {
    notificationConfigPage.navigateToEditPage();
    notificationConfigPage.handleLongInput(longText);
    notificationConfigPage.verifyLongInputError();
  });

  it("Resets to Default Values", () => {
    notificationConfigPage.navigateToEditPage();
    notificationConfigPage.resetConfig();
    notificationConfigPage.verifyDefaultValues(
      "Default Subject",
      "Default Message",
    );
  });
});
