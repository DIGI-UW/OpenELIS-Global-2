import LoginPage from "../../pages/LoginPage";

describe("Test Management", function () {
  let homePage, loginPage, adminPage, testManagementPage;

  before(() => {
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPage();
  });

  it("Navigate to Test Management Page", () => {
    testManagementPage = adminPage.goToTestManagementPage();
    testManagementPage.validatePageTitle("Test Management");
  });

  describe("Spelling Corrections", () => {
    it("Rename Existing Test Names", () => {
      testManagementPage.clickTestName();
      testManagementPage.validatePageTitle("Test names");
      testManagementPage.clickButton("0");
      testManagementPage.button("Save");
      testManagementPage.button("Accept");
    });
  });

  describe("Test Organization", () => {
    it("Navigate to Test Management Page", () => {
      testManagementPage = adminPage.goToTestManagementPage();
      testManagementPage.validatePageTitle("Test Management");
    });

    it("View Test Catalog", () => {
      testManagementPage.clickTestCatalog();
      testManagementPage.toggleSwitch();
      testManagementPage.selectTests();
    });
  });
});
