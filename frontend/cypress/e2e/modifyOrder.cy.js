import LoginPage from "../pages/LoginPage";
import PatientEntryPage from "../pages/PatientEntryPage";
import OrderEntityPage from "../pages/OrderEntityPage";

let homePage = null;
let loginPage = null;
let modifyOrderPage = null;
let patientPage = new PatientEntryPage();
let orderEntityPage = new OrderEntityPage();

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

//"Modify Order search by accession Number", is a WIP.

describe("Modify Order search by patient ", function () {
  it("User Visits Home Page and goes to Modify Order Page ", function () {
    homePage = loginPage.goToHomePage();
    modifyOrderPage = homePage.goToModifyOrderPage();
  });

  it("Should search Patient By First and LastName", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientPage.patientFirstName(patient.firstName);
      patientPage.patientLastName(patient.lastName);
    });
    patientPage.clickSearchBtn();
    patientPage.selectPatient();
    cy.wait(800);
    modifyOrderPage.clickNextButton();
  });

  it("User adds sample", function () {
    modifyOrderPage.selectSerum();
    orderEntityPage.checkPanelCheckBoxField();
    modifyOrderPage.clickRejectSample();
    modifyOrderPage.rejectReason();
    modifyOrderPage.clickNextButton();
  });

  it("Add Order", function () {
    modifyOrderPage.generateLabOrderNumber();
    cy.fixture("Order").then((order) => {
      orderEntityPage.searchRequester(order.requester);
      orderEntityPage.requesterFName(order.requesterFName);
      orderEntityPage.requesterLName(order.requesterLName);
      orderEntityPage.enterSiteName(order.siteName);
    });
  });

  it("Result Reporting and Submit Order", function () {
    modifyOrderPage.checkPatientEmail();
    modifyOrderPage.clickSubmitButton();
  });
});
