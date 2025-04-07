import LoginPage from "../pages/LoginPage";
import ProviderManagementPage from "../pages/ProviderManagementPage";
import AdminPage from "../pages/AdminPage";

describe("Add requester details", function () {
  let homePage;
  let loginPage;
  let adminPage = new AdminPage();
  let providerManagementPage = new ProviderManagementPage();

  beforeEach(() => {
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
  });

  it("Navigates to admin and adds requester", function () {
    // Go to provider management page
    const orderEntityPage = homePage.goToAdminPage();
    adminPage.goToProviderManagementPage();

    // Add provider details
    providerManagementPage.clickAddProviderButton();
    providerManagementPage.enterProviderLastName();
    providerManagementPage.enterProviderFirstName();
    providerManagementPage.clickActiveDropdown();
    providerManagementPage.addProvider();
  });
});

describe("Order Entity", function () {
  let homePage;
  let loginPage;
  let orderEntityPage;
  let patientEntryPage;

  beforeEach(() => {
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    orderEntityPage = homePage.goToOrderPage();
  });

  it("Should complete full order workflow", function () {
    // Get patient page and search for patient
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

      // Verify patient is selected correctly
      patientEntryPage.getFirstName().should("have.value", patient.firstName);
      patientEntryPage.getLastName().should("have.value", patient.lastName);
    });

    // Move to program selection
    orderEntityPage.clickNextButton();
    orderEntityPage.selectCytology();
    cy.wait(200);
    orderEntityPage.clickNextButton();

    // Select sample types
    cy.fixture("Order").then((order) => {
      order.samples.forEach((sample) => {
        orderEntityPage.selectSampleTypeOption(sample.sampleType);
        orderEntityPage.checkPanelCheckBoxField();
      });
    });
    cy.wait(1000);
    orderEntityPage.clickNextButton();

    // Validate and generate lab order number
    cy.fixture("Order").then((order) => {
      orderEntityPage.validateAcessionNumber(order.invalidLabNo);
    });

    orderEntityPage.generateLabOrderNumber();

    // Save generated lab order number
    cy.get("#labNo").then(($input) => {
      const generatedOrderNumber = $input.val();

      cy.fixture("Order").then((order) => {
        order.labNo = generatedOrderNumber;
        cy.writeFile("cypress/fixtures/EnteredOrder.json", order);
      });
    });
    cy.wait(1000);

    // Enter site name and requester details
    cy.scrollTo("top");
    cy.wait(1000);

    cy.fixture("Order").then((order) => {
      orderEntityPage.enterSiteName(order.siteName);
      orderEntityPage.enterRequesterLastAndFirstName(
        order.requester.fullName,
        order.requester.firstName,
        order.requester.lastName,
      );
    });

    orderEntityPage.rememberSiteAndRequester();
    orderEntityPage.clickSubmitOrderButton();
  });
});
