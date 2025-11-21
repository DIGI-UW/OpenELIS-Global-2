import LoginPage from "../../pages/LoginPage";
import HomePage from "../../pages/HomePage";

describe("Test Management", function () {
  let homePage, adminPage, testManagementPage;

  // Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
  before(() => {
    cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
    // Navigate to home page after login
    const loginPage = new LoginPage();
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
