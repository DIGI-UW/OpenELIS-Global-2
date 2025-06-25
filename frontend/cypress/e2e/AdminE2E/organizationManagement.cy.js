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
  it("Navigate to Admin Page", function () {
    homePage = loginPage.goToHomePage();
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
