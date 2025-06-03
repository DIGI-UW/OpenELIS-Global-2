import LoginPage from "../pages/LoginPage";
import ProviderManagementPage from "../pages/ProviderManagementPage";
import OrganizationManagementPage from "../pages/OrganizationManagementPage";
import AdminPage from "../pages/AdminPage";

let homePage = null;
let loginPage = null;
let adminPage = new AdminPage();
let orderEntityPage = null;
let patientEntryPage = null;
let providerManagementPage = new ProviderManagementPage();
let orgMgmnt = new OrganizationManagementPage();

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Add requester, institute and site first", function () {
  it("Navidates to Admin", function () {
    homePage = loginPage.goToHomePage();
    orderEntityPage = homePage.goToAdminPageProgram();
    orderEntityPage = adminPage.goToProviderManagementPage();
  });

  it("Adds and saves requester", function () {
    providerManagementPage.clickAddProviderButton();
    providerManagementPage.enterProviderLastName();
    providerManagementPage.enterProviderFirstName();
    providerManagementPage.clickActiveDropdown();
    providerManagementPage.addProvider();
    cy.reload();
  });

  it("Navidate to organisation Management", function () {
    orderEntityPage = adminPage.goToOrganizationManagement();
  });

  it("Add organisation/site details", function () {
    orgMgmnt.clickAddOrganization();
    orgMgmnt.addOrgName();
    orgMgmnt.activateOrganization();
    orgMgmnt.addPrefix();
    orgMgmnt.addParentOrg();
    orgMgmnt.checkReferringClinic();
    orgMgmnt.saveOrganization();
  });

  it("Validates the added site/organization", function () {
    orderEntityPage = adminPage.goToOrganizationManagement();
    orgMgmnt.searchOrganzation();
    orgMgmnt.confirmOrganization();
  });

  it("Add institute details", function () {
    orgMgmnt.clickAddOrganization();
    orgMgmnt.addInstituteName();
    orgMgmnt.activateOrganization();
    //orgMgmnt.addInstitutePrefix();
    orgMgmnt.addParentOrg();
    orgMgmnt.checkReferalLab();
    orgMgmnt.saveOrganization();
  });

  it("Validates the added institute", function () {
    orderEntityPage = adminPage.goToOrganizationManagement();
    orgMgmnt.searchInstitute();
    orgMgmnt.confirmInstitute();
  });
});

describe("Order Entity", function () {
  it("Navigate to Home Page then to Order entity Page ", function () {
    homePage = loginPage.goToHomePage();
    orderEntityPage = homePage.goToOrderPage();
  });

  it("Search patient in the search box", function () {
    patientEntryPage = orderEntityPage.getPatientPage();
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientEntryPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.lastName,
      );
      patientEntryPage.clickSearchPatientButton();
      patientEntryPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
      patientEntryPage.selectPatientFromSearchResults();
      cy.wait(300);
      patientEntryPage.getFirstName().should("have.value", patient.firstName);
      patientEntryPage.getLastName().should("have.value", patient.lastName);
    });
    orderEntityPage.clickNextButton();
  });

  it("Navigate to program selection", function () {
    orderEntityPage.selectCytology();
    cy.wait(200);
    orderEntityPage.clickNextButton();
  });

  it("Select sample type", function () {
    cy.fixture("Order").then((order) => {
      order.samples.forEach((sample) => {
        orderEntityPage.selectSampleTypeOption(sample.sampleType);
        orderEntityPage.checkPanelCheckBoxField();
        orderEntityPage.collectionDate(sample.collectionDate);
      });
    });
    orderEntityPage.referTest();
    orderEntityPage.selectReferralReason();
    orderEntityPage.selectInstitute();
    orderEntityPage.clickNextButton();
  });

  it("Generate Lab Order Number, Request and Received Dates", function () {
    cy.fixture("Order").then((order) => {
      order.samples.forEach((sample) => {
        orderEntityPage.requestDate(sample.receivedDate);
        orderEntityPage.receivedDate(sample.receivedDate);
      });
    });
    orderEntityPage.generateLabOrderNumber();
  });

  it("Select site name", function () {
    cy.wait(1000);
    cy.fixture("Order").then((order) => {
      orderEntityPage.enterSiteName(order.siteName);
    });
  });

  it("Enter requester first and last names", function () {
    cy.fixture("Order").then((order) => {
      orderEntityPage.enterRequesterLastAndFirstName(
        order.requester.fullName,
        order.requester.firstName,
        order.requester.lastName,
      );
    });
    orderEntityPage.rememberSiteAndRequester();
  });
  it("should click submit order button", function () {
    orderEntityPage.clickSubmitOrderButton();
    cy.wait(8000);
  });
});
