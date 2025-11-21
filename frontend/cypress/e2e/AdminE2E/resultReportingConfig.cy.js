import LoginPage from "../../pages/LoginPage";
import HomePage from "../../pages/HomePage";

describe("Result Reporting Configuration", function () {
  let homePage, adminPage, resultReportConfigPage;

  // Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
  before(() => {
    cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
    // Navigate to home page after login
    const loginPage = new LoginPage();
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPage();
  });

  it("Navigate to Result Reporting Configuration Page", () => {
    resultReportConfigPage = adminPage.goToResultReportingConfigurationPage();
    resultReportConfigPage.validatePageTitle();
  });

  it("Enable and Enter URL", () => {
    resultReportConfigPage.clickEnable("0");
    resultReportConfigPage.typeURL("0");
    resultReportConfigPage.clickEnable("1");
    resultReportConfigPage.typeURL("1");
    resultReportConfigPage.clickEnable("1");
    resultReportConfigPage.typeURL("2");
  });

  it("Save Entry", () => {
    resultReportConfigPage.saveEntry();
  });
});
