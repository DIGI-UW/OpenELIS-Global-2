import LoginPage from "../../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let patientMenuPage = null;

before(() => {
  // Initialize LoginPage object and navigate to Admin Page
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
});

describe("Patient Menu Config", function () {
  it("User navigate to the Patient Menu Config page", function () {
    patientMenuPage = adminPage.goToPatientMenuConfigPage();
  });

  it("User turns 0ff the toggle switch and submits", function () {
    patientMenuPage.turnOffToggleSwitch();
    patientMenuPage.submitButton();
  });

  it("User turns on the toggle switch", function () {
    patientMenuPage.turnOnToggleSwitch();
  });

  it("User checks the menu items and submits", function () {
    cy.get(".cds--form-item.cds--checkbox-wrapper")
      .contains("Patient Menu Active")
      .click();
    // Verify the checkbox is checked
    cy.get(".cds--form-item.cds--checkbox-wrapper")
      .contains("Patient Menu Active")
      .parent()
      .find('input[type="checkbox"]')
      .should("be.not.checked");
    patientMenuPage.submitButton();
  });
  it("User relogs in to verify the menu changes", function () {
    // Initialize LoginPage object and navigate to the menu
    loginPage = new LoginPage();
    loginPage.visit();

    homePage = loginPage.goToHomePage();
    patientMenuPage = homePage.openNavigationMenu();
  });
});
