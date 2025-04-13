import LoginPage from "../../pages/LoginPage";

let loginPage;
let homePage;
let adminPage;
let notificationPage;

describe("Test Notification Configuration", () => {
  before(() => {
    // Cypress commands are async — chaining is required!
    loginPage = new LoginPage();

    cy.session("admin-login", () => {
      loginPage.visit();
      loginPage.fillUsername("admin"); // adjust as needed
      loginPage.fillPassword("admin"); // adjust as needed
      loginPage.submit();
      cy.url().should("include", "HomePage");
    });

    cy.visit("/HomePage");
  });

  beforeEach(() => {
    cy.session("admin-login");
    cy.visit("/HomePage");

    // Navigate through POM
    loginPage = new LoginPage();
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPage();
    notificationPage = adminPage.goToNotificationConfigPage();

    // Ensure page loaded
    notificationPage.verifyPageLoaded();
  });

  it("Loads the Notification Config Menu", () => {
    notificationPage.verifyPageLoaded();
  });

  it("Navigates to the Edit Page", () => {
    notificationPage.clickEditButton();
    cy.url().should("include", "/TestNotificationConfigEdit");
    notificationPage.verifyEditPageLoaded();
  });

  it("Toggles Notification Option", () => {
    notificationPage.clickEditButton();
    notificationPage.verifyEditPageLoaded();
    notificationPage.toggleNotificationOption("#patientEmail");
    notificationPage.verifyToggleStatus("#patientEmail", true);
    notificationPage.toggleNotificationOption("#patientEmail");
    notificationPage.verifyToggleStatus("#patientEmail", false);
  });

  it("Edits Subject and Message", () => {
    const subject = "Your Test Report";
    const message = "Dear patient, your test report is ready.";

    notificationPage.clickEditButton();
    notificationPage.verifyEditPageLoaded();
    notificationPage.editSubjectAndMessage(subject, message);
    notificationPage.verifySubjectAndMessage(subject, message);
  });

  it("Saves the Configuration", () => {
    cy.intercept("POST", "/rest/TestNotificationConfig").as("saveConfig");

    notificationPage.clickEditButton();
    notificationPage.verifyEditPageLoaded();
    notificationPage.clickSaveButton();

    cy.wait("@saveConfig").its("response.statusCode").should("eq", 200);
    notificationPage.verifySuccessNotification();
  });

  it("Handles Save Errors", () => {
    cy.intercept("POST", "/rest/TestNotificationConfig", {
      statusCode: 500,
      body: { error: "Internal Server Error" },
    }).as("saveError");

    notificationPage.clickEditButton();
    notificationPage.verifyEditPageLoaded();
    notificationPage.clickSaveButton();

    cy.wait("@saveError").its("response.statusCode").should("eq", 500);
    notificationPage.verifyErrorNotification();
  });

  it("Cancels and Returns to Menu", () => {
    notificationPage.clickEditButton();
    notificationPage.verifyEditPageLoaded();
    notificationPage.clickExitButton();
    cy.url().should("include", "testNotificationConfigMenu");
  });
});
