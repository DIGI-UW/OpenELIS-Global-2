import LoginPage from "../../pages/LoginPage";

let homePage = null;
let loginPage = null;
let notificationPage = null;

const navigateToNotificationConfigPage = () => {
  homePage = loginPage.goToHomePage();
  notificationPage = adminPage.goToTestNotificationPage();
};

// before(() => {
//   cy.fixture("TestNotification").as("notificationData");
// });

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Test Notification Configuration", function () {
  before("navigate to Test Notification Page", function () {
    navigateToNotificationConfigPage();
  });

  it("User visits Notification Configuration Page", function () {
    notificationPage.verifyPageLoaded();
  });

  it("User navigates to Edit Page", function () {
    notificationPage.clickEditButton();
    cy.url().should("include", "/TestNotificationConfigEdit");
    notificationPage.verifyEditPageLoaded();
  });

  it("User toggles notification settings", function () {
    notificationPage.clickEditButton();
    notificationPage.verifyEditPageLoaded();

    // Make sure the element exists
    cy.get("#patientEmail").should("exist");

    // Enable and verify
    notificationPage.toggleNotificationOption("#patientEmail");
    notificationPage.verifyToggleStatus("#patientEmail", true);

    // Disable and verify
    notificationPage.toggleNotificationOption("#patientEmail");
    notificationPage.verifyToggleStatus("#patientEmail", false);
  });

  it("User edits subject and message templates", function () {
    notificationPage.clickEditButton();
    notificationPage.verifyEditPageLoaded();

    cy.fixture("TestNotification").then((notificationData) => {
      notificationPage.editSubjectAndMessage(
        notificationData.subject,
        notificationData.message,
      );
      notificationPage.verifySubjectAndMessage(
        notificationData.subject,
        notificationData.message,
      );
    });
  });

  it("User saves notification configuration", function () {
    cy.intercept("POST", "**/rest/TestNotificationConfig").as("saveConfig");

    notificationPage.clickEditButton();
    notificationPage.verifyEditPageLoaded();
    notificationPage.clickSaveButton();

    cy.wait("@saveConfig", { timeout: 10000 })
      .its("response.statusCode")
      .should("eq", 200);
    notificationPage.verifySuccessNotification();
  });

  it("System handles save errors correctly", function () {
    cy.intercept("POST", "**/rest/TestNotificationConfig", {
      statusCode: 500,
      body: { error: "Server Error" },
    }).as("saveConfigError");

    notificationPage.clickEditButton();
    notificationPage.verifyEditPageLoaded();
    notificationPage.clickSaveButton();

    cy.wait("@saveConfigError", { timeout: 10000 })
      .its("response.statusCode")
      .should("eq", 500);
    notificationPage.verifyErrorNotification();
  });

  it("User cancels and returns to menu", function () {
    notificationPage.clickEditButton();
    notificationPage.verifyEditPageLoaded();
    notificationPage.clickExitButton();
    cy.url().should("include", "testNotificationConfigMenu");
  });
});
