import LoginPage from "../../pages/LoginPage";

describe("Test Notification Configuration", function () {
  let homePage, loginPage, adminPage, testNotificationConfigPage;

  before(() => {
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToTestNotificationConfigPage();
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
    });

    it("Settings button and checkboxes", () => {
      testNotificationConfigPage.settingsButton("128");
      testNotificationConfigPage.settingsCheckBoxes("patientEmail");
      testNotificationConfigPage.settingsCheckBoxes("patientSMS");
      testNotificationConfigPage.settingsCheckBoxes("providerEmail");
      testNotificationConfigPage.settingsCheckBoxes("providerSMS");
      testNotificationConfigPage.clickSave();
    });
  });
});
