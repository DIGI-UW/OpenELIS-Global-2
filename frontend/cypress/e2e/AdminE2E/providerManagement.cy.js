import LoginPage from "../../pages/LoginPage";

let homePage = null;
let loginPage = null;
let adminPage = null;
let providerManagementPage = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Provider Management", function () {
  it("Navigate to Admin Page", function () {
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPageProgram();
  });

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
