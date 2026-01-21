import LoginPage from "../../pages/LoginPage";

describe("Batch Test Reassignment and Canelation", function () {
  let homePage, loginPage, adminPage, batchTestPage;

  before(() => {
    loginPage = new LoginPage();
    loginPage.visit();
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
