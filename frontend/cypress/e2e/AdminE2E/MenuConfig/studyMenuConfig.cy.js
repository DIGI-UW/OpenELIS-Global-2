import LoginPage from "../../../pages/LoginPage";
import HomePage from "../../../pages/HomePage";

let homePage = null;
let adminPage = null;
let menuConfigPage = null;

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
before(() => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  const loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
});

describe("Study Menu Configuration", function () {
  it("User navigates to the Study Menu Configuration page", function () {
    menuConfigPage = adminPage.goToStudyConfigPage();
  });

  describe("Deactivate Study Menu", () => {
    it("Deactivate Study Menu and submit", function () {
      menuConfigPage.validateToggleStatus("Off");
      menuConfigPage.checkMenuItem("studyConfig");
      menuConfigPage.submitButton();
      cy.reload();
    });

    it("Validate Study is Deactivated", () => {
      menuConfigPage.navigateToMainMenu();
    });
  });

  describe("Activate Study Menu", () => {
    it("Navigate to Study Menu Page", () => {
      menuConfigPage.navigateToMainMenu();
      menuConfigPage = adminPage.goToStudyConfigPage();
    });

    it("User turns on the toggle switch", function () {
      menuConfigPage.turnOnToggleSwitch();
      menuConfigPage.validateToggleStatus("On");
    });

    it("User checks the menu items and submits", function () {
      menuConfigPage.checkMenuItem("studyConfig");
      menuConfigPage.checkMenuItem("studySample");
      menuConfigPage.checkMenuItem("studyReports");
      menuConfigPage.submitButton();
      cy.reload();
    });

    it("Verify menu changes", function () {
      menuConfigPage.navigateToMainMenu();
    });
  });
});
