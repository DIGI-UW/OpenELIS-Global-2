import LoginPage from "../pages/LoginPage";
import PatientEntryPage from "../pages/PatientEntryPage";
import OrderEntityPage from "../pages/OrderEntityPage";
import ProviderManagementPage from "../pages/ProviderManagementPage";
import AdminPage from "../pages/AdminPage";

describe("Add requester details", function () {
  let homePage;
  let loginPage;
  let modifyOrderPage;
  let adminPage = new AdminPage();
  let providerManagementPage = new ProviderManagementPage();

  beforeEach(() => {
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
  });

  it("Navigates to admin and adds requester", function () {
    // Navigate to provider management page
    modifyOrderPage = homePage.goToAdminPage();
    modifyOrderPage = adminPage.goToProviderManagementPage();

    // Add provider details
    providerManagementPage.clickAddProviderButton();
    providerManagementPage.enterProviderLastName();
    providerManagementPage.enterProviderFirstName();
    providerManagementPage.clickActiveDropdown();
    providerManagementPage.addProvider();
  });
});

describe("Modify Order search by patient", function () {
  let homePage;
  let loginPage;
  let modifyOrderPage;
  let patientPage = new PatientEntryPage();
  let orderEntityPage = new OrderEntityPage();

  beforeEach(() => {
    // Set up fresh environment for each test
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    modifyOrderPage = homePage.goToModifyOrderPage();
  });

  it("Should search Patient By First and LastName", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.lastName,
      );
      patientPage.getFirstName().should("have.value", patient.firstName);
      patientPage.getLastName().should("have.value", patient.lastName);
      patientPage.getLastName().should("not.have.value", patient.inValidName);

      modifyOrderPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });
  });

  it("Should be able to search patients By gender", function () {
    cy.wait(1000);
    patientPage.getMaleGenderRadioButton().should("be.visible");
    patientPage.getMaleGenderRadioButton().click();
    cy.wait(200);
    modifyOrderPage.clickSearchPatientButton();
    patientPage.validatePatientByGender("M");
  });

  it("Should search patient By PatientId", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByPatientId(patient.nationalId);
      modifyOrderPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });
  });

  it("Should complete full order workflow", function () {
    // Search for patient
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByPatientId(patient.nationalId);
      modifyOrderPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });

    // Select respective patient
    cy.wait(1000);
    modifyOrderPage.clickRespectivePatient();
    cy.wait(1000);

    // Validate and proceed
    modifyOrderPage.checkProgramButton();
    modifyOrderPage.clickNextButton();

    // Record values
    cy.wait(1000);
    modifyOrderPage.assignValues();

    // Complete order
    modifyOrderPage.clickNextButton();
    cy.wait(1000);
    orderEntityPage.rememberSiteAndRequester();
    modifyOrderPage.clickSubmitButton();

    // Print barcode
    cy.window().then((win) => {
      cy.stub(win, "open").as("windowOpen");
    });
    modifyOrderPage.clickPrintBarcodeButton();
    cy.get("@windowOpen").should(
      "be.calledWithMatch",
      /\/api\/OpenELIS-Global\/LabelMakerServlet\?labNo=/,
    );
  });
});

describe("Modify Order search by accession Number", function () {
  let homePage;
  let loginPage;
  let modifyOrderPage;
  let orderEntityPage = new OrderEntityPage();

  beforeEach(() => {
    // Set up fresh environment for each test
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    modifyOrderPage = homePage.goToModifyOrderPage();
  });

  it("Should search and modify with accession number", () => {
    // Search by accession number
    cy.fixture("Patient").then((patient) => {
      modifyOrderPage.enterAccessionNo(patient.labNo);
      modifyOrderPage.clickSubmitAccessionButton();
    });
    cy.wait(1000);

    // Validate and proceed
    modifyOrderPage.checkProgramButton();
    modifyOrderPage.clickNextButton();

    // Add sample
    modifyOrderPage.selectSerumSample();
    orderEntityPage.checkPanelCheckBoxField();
    modifyOrderPage.clickNextButton();

    // Add order details
    orderEntityPage.generateLabOrderNumber();
    cy.fixture("Order").then((order) => {
      orderEntityPage.enterSiteName(order.siteName);
      orderEntityPage.enterRequesterLastAndFirstName(
        order.requester.fullName,
        order.requester.firstName,
        order.requester.lastName,
      );
    });
    orderEntityPage.rememberSiteAndRequester();
    modifyOrderPage.clickSubmitButton();
  });
});
