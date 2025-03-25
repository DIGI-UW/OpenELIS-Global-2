import LoginPage from "../../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let nonComformMenuPage = null;

before(() => {
  // Initialize LoginPage object and navigate to Admin Page
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
});

describe("NonComformiy Menu Config", function () {
  it("User navigate to the NC Menu Config page", function () {
    nonComformMenuPage = adminPage.goToNonComformMenuConfigPage();
  });

  it("User turns 0ff the toggle switch and submits", function () {
    nonComformMenuPage.turnOffToggleSwitch();
    nonComformMenuPage.submitButton();
  });

  it("User turns on the toggle switch", function () {
    nonComformMenuPage.turnOnToggleSwitch();
  });

  it("User checks the menu items and submits", function () {
    cy.get(".cds--form-item.cds--checkbox-wrapper")
      .contains("Non-Conformity Menu Active")
      .click();
    // Verify the checkbox is checked
    cy.get(".cds--form-item.cds--checkbox-wrapper")
      .contains("Non-Conformity Menu Active")
      .parent()
      .find('input[type="checkbox"]')
      .should("be.checked");
    nonComformMenuPage.submitButton();
  });
  it("User relogs in to verify the menu changes", function () {
    // Initialize LoginPage object and navigate to the menu
    loginPage = new LoginPage();
    loginPage.visit();

    homePage = loginPage.goToHomePage();
    nonComformMenuPage = homePage.openNavigationMenu();
  });
});
