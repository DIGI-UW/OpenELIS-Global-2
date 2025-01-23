import LoginPage from "../../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let globalMenuConfigPage = null;

before(() => {
  // Initialize LoginPage object and navigate to Admin Page
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
});

describe("Global Menu Configuration", function () {
  it("User navigates to the Global Menu Configuration page", function () {
    globalMenuConfigPage = adminPage.goToGlobalMenuConfigPage();
  });

  it("User turns 0ff the toggle switch and submits", function () {
    globalMenuConfigPage.turnOffToggleSwitch();
    globalMenuConfigPage.submitButton();
  });

  it("User turns on the toggle switch", function () {
    globalMenuConfigPage.turnOnToggleSwitch();
  });
  it("User checks the menu items and submits", function () {
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
  it("User relogs in to verify the menu changes", function () {
    // Initialize LoginPage object and navigate to the menu
    loginPage = new LoginPage();
    loginPage.visit();

    homePage = loginPage.goToHomePage();
    globalMenuConfigPage = homePage.openNavigationMenu();
  });
});
