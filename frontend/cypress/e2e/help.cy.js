import LoginPage from "../pages/LoginPage";

describe("Interacts with Help options", function () {
let loginPage, homePage, helpPage;

beforeEach(() => {
  // Initialize LoginPage object and navigate to Home Page
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  helpPage = homePage.goToHelp();
});

  it("User navigates to User Manual", function () {
    helpPage.clickUserManual();
  });

  describe("User navigates to Process Documentation", function () {
    beforeEach(() => {
      helpPage.clickProcessDocumentation();
    });

    it("User navigates to VL Form", function () {
      helpPage.clickVLForm();
    });

    it("User navigates to DBS Form", function () {
      helpPage.clickDBSForm();
    });
  });
});
