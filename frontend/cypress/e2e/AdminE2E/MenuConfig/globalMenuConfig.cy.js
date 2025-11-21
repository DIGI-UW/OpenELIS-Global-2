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

describe("Global Menu Configuration", function () {
  it("User navigates to the Global Menu Configuration page", function () {
    menuConfigPage = adminPage.goToGlobalMenuConfigPage();
  });

  it("User turns 0ff the toggle switch and submits", function () {
    menuConfigPage.turnOffToggleSwitch();
    menuConfigPage.submitButton();
  });

  it("User turns on the toggle switch", function () {
    menuConfigPage.turnOnToggleSwitch();
  });
  it("User checks the menu items and submits", function () {
    menuConfigPage.checkMenuItem("home");
    menuConfigPage.checkMenuItem("order");
    menuConfigPage.checkMenuItem("billing");
    menuConfigPage.checkMenuItem("immunoChem");
    menuConfigPage.checkMenuItem("cytology");
    menuConfigPage.checkMenuItem("results");
    menuConfigPage.checkMenuItem("validation");
    menuConfigPage.checkMenuItem("patient");
    menuConfigPage.checkMenuItem("pathology");
    menuConfigPage.checkMenuItem("workplan");
    menuConfigPage.checkMenuItem("nonConform");
    menuConfigPage.checkMenuItem("reports");
    menuConfigPage.checkMenuItem("study");
    menuConfigPage.checkMenuItem("admin");
    menuConfigPage.checkMenuItem("help");
    menuConfigPage.submitButton();
  });
  it("User relogs in to verify the menu changes", function () {
    // Relogin to verify menu changes (cy.session() will reuse cached session)
    cy.login(); // Uses cy.session() - will reuse cached session
    const loginPage = new LoginPage();
    homePage = loginPage.goToHomePage();
    menuConfigPage = homePage.openNavigationMenu();
  });
});
