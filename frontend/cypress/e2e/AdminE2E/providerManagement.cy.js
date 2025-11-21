import LoginPage from "../../pages/LoginPage";
import HomePage from "../../pages/HomePage";

let homePage = null;
let adminPage = null;
let providerManagementPage = null;

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
before("login", () => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  const loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPageProgram();
});

describe("Provider Management", function () {
  describe("Enter Provider", () => {
    it("Navigate to Provider Management Page", () => {
      providerManagementPage = adminPage.goToProviderManagementPage();
    });

    it("Enter First Provider details", function () {
      providerManagementPage.clickAddProviderButton();
      providerManagementPage.enterProviderLastName("Prime");
      providerManagementPage.enterProviderFirstName("Optimus");
      providerManagementPage.activeStatus("No");
      providerManagementPage.addProvider();
    });

    it("Enter Second Provider details", function () {
      providerManagementPage.clickAddProviderButton();
      providerManagementPage.enterProviderLastName("Jam");
      providerManagementPage.enterProviderFirstName("Jim");
      providerManagementPage.activeStatus("Yes");
      providerManagementPage.addProvider();
    });

    it("Validate added Providers", function () {
      providerManagementPage.searchProvider("Optimus");
      providerManagementPage.confirmProvider("false");
      providerManagementPage.searchProvider("Jim");
      providerManagementPage.confirmProvider("true");
    });
  });

  describe("Modify the first Provider", () => {
    it("Select and Modify Provider", () => {
      providerManagementPage.searchProvider("Optimus");
      providerManagementPage.checkProvider("Optimus");
      providerManagementPage.modifyProvider();
      providerManagementPage.modifyStatus("Yes");
      providerManagementPage.updateProvider();
    });

    it("Validate Active Status", () => {
      providerManagementPage.searchProvider("Optimus");
      providerManagementPage.confirmProvider("true");
    });
  });

  describe("Deactivate the second Provider", () => {
    it("Select and Deactivate Provider", () => {
      providerManagementPage.searchProvider("Jim");
      providerManagementPage.checkProvider("Jim");
      providerManagementPage.deactivateProvider();
    });

    it("Validate Active Status", () => {
      providerManagementPage.searchProvider("Jim");
      providerManagementPage.confirmProvider("false");
    });
  });
});
