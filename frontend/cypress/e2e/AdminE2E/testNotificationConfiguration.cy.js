import LoginPage from "../../pages/LoginPage";

describe("Test Notification Configuration", function () {
  let homePage, loginPage, adminPage, testNotificationConfigPage;

  before(() => {
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPage();
  });

  it("Navigate to Test Notification Configuration Page", () => {
    testNotificationConfigPage = adminPage.goToTestNotificationConfigPage();
    testNotificationConfigPage.validatePageTitle();
  });

  describe("Enter Data and Save", () => {
    it("Checkboxes", () => {
      testNotificationConfigPage.checkBoxes("patientEmail");
      testNotificationConfigPage.checkBoxes("patientSMS");
      testNotificationConfigPage.checkBoxes("providerEmail");
      testNotificationConfigPage.checkBoxes("providerSMS");
      testNotificationConfigPage.saveChanges();
      cy.wait(1000);
    });

    it("Click Settings, checkboxes and Save", () => {
      testNotificationConfigPage.editTestNotificationButton("373");
      testNotificationConfigPage.settingsCheckBoxes("patientEmail");
      testNotificationConfigPage.settingsCheckBoxes("patientSMS");
      testNotificationConfigPage.settingsCheckBoxes("providerEmail");
      testNotificationConfigPage.settingsCheckBoxes("providerSMS");
      testNotificationConfigPage.enterSubject("1", "Test Subject");
      testNotificationConfigPage.enterMessage("1", "Test Message");
      testNotificationConfigPage.enterBCC("Test BCC");
      testNotificationConfigPage.enterSubject("2", "Test Subject");
      testNotificationConfigPage.enterMessage("2", "Test Message");
      testNotificationConfigPage.clickSave();
    });
  });
});
