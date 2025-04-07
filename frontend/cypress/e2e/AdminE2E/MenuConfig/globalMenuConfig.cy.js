import LoginPage from "../../../pages/LoginPage";

describe("Global Menu Configuration", function () {
  let loginPage, homePage, adminPage, globalMenuConfigPage;

  beforeEach(() => {
    // Initialize LoginPage object and navigate to Admin Page
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPage();
    globalMenuConfigPage = adminPage.goToGlobalMenuConfigPage();
  });

  it("User can turn off the toggle switch and submit", function () {
    globalMenuConfigPage.turnOffToggleSwitch();
    globalMenuConfigPage.submitButton();
  });

  it("User can turn on the toggle switch and submit", function () {
    // Setting up the test state first
    globalMenuConfigPage.turnOnToggleSwitch();
    globalMenuConfigPage.checkMenuItem("home");
    globalMenuConfigPage.checkMenuItem("order");
    globalMenuConfigPage.checkMenuItem("billing");
    globalMenuConfigPage.checkMenuItem("immunoChem");
    globalMenuConfigPage.checkMenuItem("cytology");
    globalMenuConfigPage.checkMenuItem("results");
    globalMenuConfigPage.checkMenuItem("validation");
    globalMenuConfigPage.checkMenuItem("patient");
    globalMenuConfigPage.checkMenuItem("pathology");
    globalMenuConfigPage.checkMenuItem("workplan");
    globalMenuConfigPage.checkMenuItem("nonConform");
    globalMenuConfigPage.checkMenuItem("reports");
    globalMenuConfigPage.checkMenuItem("study");
    globalMenuConfigPage.checkMenuItem("admin");
    globalMenuConfigPage.checkMenuItem("help");
    globalMenuConfigPage.submitButton();
  });

  it("Verifies menu changes persist after relogging in", function () {
    // First set up the menu configuration
    globalMenuConfigPage.turnOnToggleSwitch();
    globalMenuConfigPage.checkMenuItem("home");
    globalMenuConfigPage.checkMenuItem("order");
    globalMenuConfigPage.checkMenuItem("billing");
    // Check necessary menu items
    globalMenuConfigPage.submitButton();

    // Re-login to verify changes
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    const navigationMenu = homePage.openNavigationMenu();

    // Add verification assertions here
    // e.g., cy.get('[data-testid="home-menu-item"]').should('be.visible');
  });
});
