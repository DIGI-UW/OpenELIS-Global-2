import LoginPage from "../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let billingMenuManagementPage = null;

before(() => {
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
  billingMenuManagementPage = adminPage.goToBillingMenuManagementPage();
});

describe("Billing Menu Management", function () {
  it("User navigates to the Billing Menu Management page", function () {
    billingMenuManagementPage.visit();
  });

  it("User updates the billing address and submits", function () {
    billingMenuManagementPage.enterBillingAddress(
      "https://example.com/billing",
    );
    billingMenuManagementPage.submitButton();
  });

  it("User activates the billing menu and submits", function () {
    billingMenuManagementPage.toggleBillingActive(true);
    billingMenuManagementPage.submitButton();
  });

  it("User deactivates the billing menu and submits", function () {
    billingMenuManagementPage.toggleBillingActive(false);
    billingMenuManagementPage.submitButton();
  });

  it("User relogs in to verify the menu changes", function () {
    loginPage = new LoginPage();
    loginPage.visit();

    homePage = loginPage.goToHomePage();
    billingMenuManagementPage = homePage.openNavigationMenu();
  });
});
