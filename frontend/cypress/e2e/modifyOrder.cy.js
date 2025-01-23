import LoginPage from "../pages/LoginPage";
import PatientEntryPage from "../pages/PatientEntryPage";

let homePage = null;
let loginPage = null;
let modifyOrderPage = null;
let patientPage = new PatientEntryPage();

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Modify Order search by accession Number", function () {
  it("User Visits Home Page and goes to Modify Order Page ", function () {
    homePage = loginPage.goToHomePage();
    modifyOrderPage = homePage.goToModifyOrderPage();
  });

  it("User searches with accession number", () => {
    cy.wait(1000);
    cy.fixture("EnteredOrder").then((order) => {
      modifyOrderPage.enterAccessionNo(order.labNo);
      modifyOrderPage.clickSubmitButton();
    });
  });

  it("should check for program selection button and go to next page ", function () {
    modifyOrderPage.checkProgramButton();
    modifyOrderPage.clickNextButton();
  });

  it("should be able to record", function () {
    modifyOrderPage.assignValues();
  });

  it("User should click next to go add order page and submit the order", function () {
    modifyOrderPage.clickNextButton();
    cy.wait(1000);
    modifyOrderPage.clickNextButton();
  });

  it("should be able to print barcode", function () {
    cy.window().then((win) => {
      cy.spy(win, "open").as("windowOpen");
    });
    modifyOrderPage.clickPrintBarcodeButton();
    cy.get("@windowOpen").should(
      "be.calledWithMatch",
      "/api/OpenELIS-Global/LabelMakerServlet?labNo=",
    );
  });
});

describe("Modify Order search by patient ", function () {
  it("User Visits Home Page and goes to Modify Order Page ", function () {
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
    cy.wait(200).reload();
  });

  it("Should be able to search patients By gender", function () {
    cy.wait(1000);
    patientPage.getMaleGenderRadioButton().should("be.visible");
    patientPage.getMaleGenderRadioButton().click();
    cy.wait(200);
    modifyOrderPage.clickSearchPatientButton();
    cy.fixture("Patient").then((patient) => {
      patientPage.validatePatientByGender("M");
    });
    cy.wait(200).reload();
  });

  it("should search patient By PatientId", function () {
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
  //TO DO needs fixing
  it("Should be able to search by respective patient ", function () {
    cy.wait(1000);
    modifyOrderPage.clickRespectivePatient();
  });
  it("should check for program selection button and go to next page ", function () {
    cy.wait(1000);
    modifyOrderPage.checkProgramButton();
    modifyOrderPage.clickNextButton();
  });

  it("should be able to record", function () {
    cy.wait(1000);
    modifyOrderPage.assignValues();
  });

  it("User should click next to go add order page and submit the order", function () {
    modifyOrderPage.clickNextButton();
    cy.wait(1000);
    modifyOrderPage.clickNextButton();
  });

  it("should be able to print barcode", function () {
    cy.window().then((win) => {
      cy.spy(win, "open").as("windowOpen");
    });
    modifyOrderPage.clickPrintBarcodeButton();
    cy.get("@windowOpen").should(
      "be.calledWithMatch",
      "/api/OpenELIS-Global/LabelMakerServlet?labNo=",
    );
  });
});
