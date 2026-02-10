import LoginPage from "../../pages/LoginPage";

let homePage = null;
let loginPage = null;
let adminPage = null;
let organizationManagement = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Add Organization and Institute", function () {
  beforeEach(() => {
    // Intercept Organization save API to capture response details
    cy.intercept("POST", "**/rest/Organization*").as("saveOrg");
  });

  it("Navigate to Admin Page", function () {
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPageProgram();
    cy.screenshot("01-admin-page-loaded");
  });

  it("Navigate to organisation Management", function () {
    organizationManagement = adminPage.goToOrganizationManagement();
    cy.screenshot("02-org-management-loaded");
  });

  it("Add organisation/site details", function () {
    organizationManagement.clickAddOrganization();
    organizationManagement.addOrgName();
    organizationManagement.activateOrganization();
    organizationManagement.addPrefix();
    organizationManagement.addParentOrg();
    organizationManagement.checkReferringClinic();
    cy.screenshot("03-org-form-filled");

    organizationManagement.saveOrganization();

    // Wait for the save API call and log its response
    cy.wait("@saveOrg").then((interception) => {
      const { statusCode, body } = interception.response;
      cy.log(`**Save Org API Response:** status=${statusCode}`);
      cy.log(`**Response body:** ${JSON.stringify(body).substring(0, 500)}`);

      // Screenshot after save completes
      cy.screenshot("04-after-org-save", {
        capture: "fullPage",
      });

      // Fail loudly if server returned an error
      expect(statusCode).to.be.lessThan(400);
    });
  });

  it("Validate added site/organization", function () {
    organizationManagement = adminPage.goToOrganizationManagement();
    organizationManagement.searchOrganzation();
    cy.screenshot("05-org-search-results");
    organizationManagement.confirmOrganization();
    cy.screenshot("06-org-confirmed");
  });

  it("Add institute details", function () {
    organizationManagement.clickAddOrganization();
    organizationManagement.addInstituteName();
    organizationManagement.activateOrganization();
    //organizationManagement.addInstitutePrefix();
    organizationManagement.addParentOrg();
    organizationManagement.checkReferalLab();
    cy.screenshot("07-institute-form-filled");

    organizationManagement.saveOrganization();

    // Wait for the save API call and log its response
    cy.wait("@saveOrg").then((interception) => {
      const { statusCode, body } = interception.response;
      cy.log(`**Save Institute API Response:** status=${statusCode}`);
      cy.log(`**Response body:** ${JSON.stringify(body).substring(0, 500)}`);

      cy.screenshot("08-after-institute-save", {
        capture: "fullPage",
      });

      expect(statusCode).to.be.lessThan(400);
    });
  });

  it("Validate added institute", function () {
    organizationManagement = adminPage.goToOrganizationManagement();
    organizationManagement.searchInstitute();
    cy.screenshot("09-institute-search-results");
    organizationManagement.confirmInstitute();
    cy.screenshot("10-institute-confirmed");
  });
});
