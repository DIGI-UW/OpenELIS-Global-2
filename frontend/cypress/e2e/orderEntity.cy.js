import LoginPage from "../pages/LoginPage";
import ProviderManagementPage from "../pages/ProviderManagementPage";
import AdminPage from "../pages/AdminPage";

let homePage = null;
let loginPage = null;
let adminPage = new AdminPage();
let orderEntityPage = null;
let patientEntryPage = null;
let providerManagementPage = new ProviderManagementPage();

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Add requester details first", function () {
  it("Navidates to admin", function () {
    homePage = loginPage.goToHomePage();
    orderEntityPage = homePage.goToAdminPage();
    orderEntityPage = adminPage.goToProviderManagementPage();
  });

  it("Adds and saves requester", function () {
    providerManagementPage.clickAddProviderButton();
    providerManagementPage.enterProviderLastName();
    providerManagementPage.enterProviderFirstName();
    providerManagementPage.clickActiveDropdown();
    providerManagementPage.addProvider();
  });
});

describe("Order Entity", function () {
  it("User Visits Home Page and goes to Order entity Page ", function () {
    homePage = loginPage.goToHomePage();
    orderEntityPage = homePage.goToOrderPage();
  });

  it("Should search patient in the search box", function () {
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

  it("User goes to program selection", function () {
    orderEntityPage.selectCytology();
    cy.wait(200);
    orderEntityPage.clickNextButton();
  });

  it("User should select sample type option", function () {
    cy.fixture("Order").then((order) => {
      order.samples.forEach((sample) => {
        orderEntityPage.selectSampleTypeOption(sample.sampleType);
        orderEntityPage.checkPanelCheckBoxField();
      });
    });
    cy.wait(1000);
    orderEntityPage.clickNextButton();
  });

  it("Should do a validation check for labNo and then click generate Lab Order Number and store it in a fixture", function () {
    cy.fixture("Order").then((order) => {
      orderEntityPage.validateAcessionNumber(order.invalidLabNo);
    });

    orderEntityPage.generateLabOrderNumber();

    cy.get("#labNo").then(($input) => {
      const generatedOrderNumber = $input.val();

      cy.fixture("Order").then((order) => {
        order.labNo = generatedOrderNumber;
        cy.writeFile("cypress/fixtures/EnteredOrder.json", order);
      });
    });
    cy.wait(1000);
  });

  it("should Enter or select site name", function () {
    cy.scrollTo("top");
    cy.wait(1000);
    cy.fixture("Order").then((order) => {
      orderEntityPage.enterSiteName(order.siteName);
    });
  });

  it("should enter requester first and last name's", function () {
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
