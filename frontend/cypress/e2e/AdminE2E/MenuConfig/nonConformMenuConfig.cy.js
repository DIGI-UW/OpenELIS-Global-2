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

describe("Non-Conform Menu Configuration", function () {
  it("User navigates to the Non-Conform Menu Configuration page", function () {
    menuConfigPage = adminPage.goToNonConformConfigPage();
  });

  describe("Deactivate Non-Conform Menu", () => {
    it("Deactivate Non-Conform Menu and submit", function () {
      menuConfigPage.validateToggleStatus("Off");
      menuConfigPage.uncheckNonConform();
      menuConfigPage.submitButton();
    });

    it("Validate Non-Conform is Deactivated", () => {
      cy.reload();
      // Wait for page to load after reload
      cy.get("body").should("exist");
      menuConfigPage.navigateToMainMenu();
      menuConfigPage.validateNonConformOff();
    });
  });

  describe("Activate Non-Conform Menu", () => {
    it("Navigate to Non-Conform Menu Page", () => {
      menuConfigPage.navigateToMainMenu();
      menuConfigPage = adminPage.goToNonConformConfigPage();
    });

    it("User turns on the toggle switch", function () {
      menuConfigPage.turnOnToggleSwitch();
      menuConfigPage.validateToggleStatus("On");
    });

    it("User checks the menu items and submits", function () {
      menuConfigPage.checkMenuItem("nonConform");
      menuConfigPage.checkMenuItem("reportNCE");
      menuConfigPage.checkMenuItem("viewNCE");
      menuConfigPage.checkMenuItem("correctiveAction");
      menuConfigPage.submitButton();
    });

    it("Verify menu changes", function () {
      cy.reload();
      // Wait for page to load after reload
      cy.get("body").should("exist");
      menuConfigPage.navigateToMainMenu();
      menuConfigPage.validateNonConformOn();
    });
  });
});
