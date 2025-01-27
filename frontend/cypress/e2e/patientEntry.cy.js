import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let patientPage = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});
describe("Add/Edit Patient", function () {
  it("User goes to Add/Edit Patient Page", () => {
    homePage = loginPage.goToHomePage();
    patientPage = homePage.goToPatientEntry();
  });
});
  //SEARCH PATIENT
describe("Search for Patient", function () {
  it("User Searches for patient by First Name", function(){
    patientPage.clickSearchPatientBtn();
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByFirstNameOnly(patient.firstName);
      patientPage.clickSearchBtn();
     });
     cy.wait(200).reload();
    });


  it("User Searches for patient by lastst Name", function(){
    patientPage.clickSearchPatientBtn();
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByLastNameOnly(patient.lastName);
      patientPage.clickSearchBtn();
     });
     cy.wait(200).reload();
    });

  it("Should be able to search patients By gender", function () {
    patientPage.getMaleGenderRadioButton();
    cy.wait(200);
    patientPage.clickSearchBtn();
    cy.wait(200).reload();
  });

  it("Should search Patient By First and LastName", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByFirstNameOnly(patient.firstName);
      patientPage.searchPatientByLastNameOnly(patient.lastName);
    });
    patientPage.clickSearchBtn();
    cy.wait(200).reload();
  });

  it("should search patient By Date Of Birth", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByDateOfBirth(patient.DOB);
      patientPage.clickSearchBtn();
      patientPage.validatePatientSearchTablebyRespectiveField(
        patient.DOB,
        "DOB",
      );
    });
    cy.wait(200).reload();
  });

  it("should search patient By Lab Number", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientBylabNo(patient.labNo);
      cy.intercept(
        "GET",
        `**/rest/patient-search-results?*labNumber=${patient.labNo}*`,
      ).as("getPatientSearch");
      patientPage.clickSearchBtn();
      cy.wait("@getPatientSearch").then((interception) => {
        const responseBody = interception.response.body;
        console.log(responseBody);
        expect(responseBody.patientSearchResults).to.be.an("array").that.is
          .empty;
      });
    });
    cy.wait(200).reload();
  });

  it("should search patient By PatientId", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByPatientId(patient.nationalId);
      patientPage.clickSearchBtn();
      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });
  });
});
  //NEW PATIENT
describe("New Patient", function () {
  it("External search button should be deactivated", function () {
    patientPage.getExternalSearchButton();
  });

  it("User should be able to navigate to create Patient tab", function () {
    patientPage.clickNewPatientTab();
    patientPage.getSubmitButton().should("be.visible");
  });

  it("User should enter patient Information", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.enterPatientInfo(
        patient.firstName,
        patient.lastName,
        patient.subjectNumber,
        patient.nationalId,
        patient.DOB,
      );
    });
  });
  it("User should click save new patient information button", function () {
    patientPage.clickSavePatientButton();
    cy.wait(1000);
    cy.get("div[role='status']").should("be.visible");
    cy.wait(200).reload();
  });
});

  

