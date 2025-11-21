import LoginPage from "../../pages/LoginPage";
import HomePage from "../../pages/HomePage";

describe("Batch Test Reassignment and Canelation", function () {
  let homePage, adminPage, batchTestPage;

  before(() => {
    // Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
    cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
    // Navigate to home page after login
    const loginPage = new LoginPage();
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPage();
  });

  it("Navigate to Batch Test Reassignment and Canelation Page", () => {
    batchTestPage = adminPage.goToBatchTestReassignmentandCanelationPage();
    batchTestPage.validatePageTitle();
  });

  describe("Enter Data and Cancel", () => {
    it("Select Sample and Tests", () => {
      batchTestPage.selectSampleType();
      batchTestPage.checkBoxes("currentTest");
      batchTestPage.checkBoxes("replaceWith");
      batchTestPage.selectTest("1");
      batchTestPage.selectTest("0");
    });

    it("Cancel Changes", () => {
      batchTestPage.clickCancel();
    });
  });

  describe("Enter Data and Save", () => {
    it("Select Sample and Tests", () => {
      batchTestPage.selectSampleType();
      batchTestPage.checkBoxes("currentTest");
      batchTestPage.checkBoxes("replaceWith");
      batchTestPage.selectTest("1");
      batchTestPage.selectTest("0");
    });

    it("Save Changes", () => {
      batchTestPage.clickOk();
    });
  });
});
