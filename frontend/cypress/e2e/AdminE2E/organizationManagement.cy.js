import LoginPage from "../../pages/LoginPage";
import HomePage from "../../pages/HomePage";

let homePage = null;
let adminPage = null;
let organizationManagement = null;

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
before("login", () => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  const loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
});

describe("Add Organization and Institute", function () {
  it("Navigate to Admin Page", function () {
    adminPage = homePage.goToAdminPageProgram();
  });

  it("Navigate to organisation Management", function () {
    organizationManagement = adminPage.goToOrganizationManagement();
  });

  it("Add organisation/site details", function () {
    organizationManagement.clickAddOrganization();
    organizationManagement.addOrgName();
    organizationManagement.activateOrganization();
    organizationManagement.addPrefix();
    organizationManagement.addParentOrg();
    organizationManagement.checkReferringClinic();
    organizationManagement.saveOrganization();
  });

  it("Validate added site/organization", function () {
    organizationManagement = adminPage.goToOrganizationManagement();
    organizationManagement.searchOrganzation();
    organizationManagement.confirmOrganization();
  });

  it("Add institute details", function () {
    // Ensure we're on the organization management page and it's ready
    organizationManagement = adminPage.goToOrganizationManagement();
    // Wait for the page to be fully loaded before clicking add
    cy.get("[data-cy='add-button']", { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled");
    organizationManagement.clickAddOrganization();
    organizationManagement.addInstituteName();
    organizationManagement.activateOrganization();
    //organizationManagement.addInstitutePrefix();
    organizationManagement.addParentOrg();
    organizationManagement.checkReferalLab();
    organizationManagement.saveOrganization();
  });

  it("Validate added institute", function () {
    organizationManagement = adminPage.goToOrganizationManagement();
    organizationManagement.searchInstitute();
    organizationManagement.confirmInstitute();
  });
});
