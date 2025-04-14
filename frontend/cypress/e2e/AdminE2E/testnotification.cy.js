import LoginPage from "../../pages/LoginPage";
import NotificationConfigPage from "../../pages/TestNotificationPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let notificationConfigPage = null;

before(() => {
  loginPage = new LoginPage();

  cy.session("admin-login", () => {
    loginPage.visit();
    loginPage.fillUsername("admin");
    loginPage.fillPassword("admin");
    loginPage.submit();
    cy.url().should("include", "HomePage");
  });

  // Post login navigation
  cy.visit("/HomePage");
  loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
});

describe("Test Notification Configuration", () => {
  it("Navigates to Notification Config Page", () => {
    notificationConfigPage = adminPage.goToNotificationConfigPage();
    notificationConfigPage.verifyPageLoaded();
  });

  it("Loads and Navigates to Edit Page", () => {
    notificationConfigPage.clickEditButton();
    cy.url().should("include", "TestNotificationConfigEdit");
    notificationConfigPage.verifyEditPageLoaded();
  });

  it("Toggles Notification", () => {
    notificationConfigPage.clickEditButton();
    notificationConfigPage.verifyEditPageLoaded();

    notificationConfigPage.toggleNotificationOption("#patientEmail");
    notificationConfigPage.verifyToggleStatus("#patientEmail", true);
    notificationConfigPage.toggleNotificationOption("#patientEmail");
    notificationConfigPage.verifyToggleStatus("#patientEmail", false);
  });

  it("Edits Subject and Message", () => {
    notificationConfigPage.clickEditButton();
    notificationConfigPage.verifyEditPageLoaded();

    const subject = "Automated Subject";
    const message = "Automated Message";
    notificationConfigPage.editSubjectAndMessage(subject, message);
    notificationConfigPage.verifySubjectAndMessage(subject, message);
  });

  it("Saves the Configuration", () => {
    cy.intercept("POST", "/rest/TestNotificationConfig").as("saveConfig");

    notificationConfigPage.clickEditButton();
    notificationConfigPage.verifyEditPageLoaded();
    notificationConfigPage.clickSaveButton();

    cy.wait("@saveConfig").its("response.statusCode").should("eq", 200);
    notificationConfigPage.verifySuccessNotification();
  });

  it("Handles Save Errors", () => {
    cy.intercept("POST", "/rest/TestNotificationConfig", {
      statusCode: 500,
    }).as("saveError");

    notificationConfigPage.clickEditButton();
    notificationConfigPage.verifyEditPageLoaded();
    notificationConfigPage.clickSaveButton();

    cy.wait("@saveError").its("response.statusCode").should("eq", 500);
    notificationConfigPage.verifyErrorNotification();
  });

  it("Cancels and Returns to Menu", () => {
    notificationConfigPage.clickEditButton();
    notificationConfigPage.verifyEditPageLoaded();
    notificationConfigPage.clickExitButton();
    cy.url().should("include", "testNotificationConfigMenu");
  });
});
