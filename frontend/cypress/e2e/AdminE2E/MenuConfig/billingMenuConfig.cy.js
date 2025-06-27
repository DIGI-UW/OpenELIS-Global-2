import LoginPage from "../../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let menuConfigPage = null;

before(() => {
  // Initialize LoginPage object and navigate to Admin Page
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
});

describe("Billing Menu Configuration", function () {
  it("User navigates to the Billing Menu Configuration page", function () {
    menuConfigPage = adminPage.goToBillingConfigPage();
  });

  describe("Deactivate Billing Menu", () => {
    it("Deactivate Billing Menu and submit", function () {
      menuConfigPage.uncheckBillingMenu();
      menuConfigPage.billingAddress(
        "https://united-nations-development-programme.odoo.com/odoo/accounting",
      );
      menuConfigPage.submitButton();
    });

    it("Validate Billing is Deactivated", () => {
      cy.reload();
      menuConfigPage.navigateToMainMenu();
      menuConfigPage.validateBillingMenuOff();
    });
  });

  describe("Activate Billing Menu", () => {
    it("Navigate to Billing Menu Page", () => {
      menuConfigPage.navigateToMainMenu();
      menuConfigPage = adminPage.goToBillingConfigPage();
    });

    it("User checks the menu items and submits", function () {
      menuConfigPage.checkMenuItem("billingMenu");
      menuConfigPage.submitButton();
      cy.reload();
    });

    it("Verify menu changes", function () {
      menuConfigPage.navigateToMainMenu();
      menuConfigPage.validateBillingMenuOn();
    });
  });
});
