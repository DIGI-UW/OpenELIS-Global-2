//import { cy } from "date-fns/locale";
import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let patientPage = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

before(() => {
  cy.fixture("Patient").as("patient");
});

describe("Add/Edit Patient", function () {
  it("User goes to Add/Edit Patient Page", () => {
    homePage = loginPage.goToHomePage();
    patientPage = homePage.goToPatientEntry();
  });
});

//NEW PATIENT
describe("New Patient", function () {
  it("External search button should be deactivated", function () {
    patientPage.getExternalSearchButton();
  });

  it("User navigates to New Patient", function () {
    patientPage.clickNewPatientBtn();
  });

  it("User enters patient Information", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.enterUniquePatientNo(patient.uniqueID);
      patientPage.enterNationalID(patient.nationalId);
      patientPage.patientLastName(patient.lastName);
      patientPage.patientFirstName(patient.firstName);
      patientPage.patientPhoneNumber(patient.patientPhone);
      patientPage.patientDateOfBirth(patient.dOB);
      patientPage.selectMaleGenderRadioButton();
    });
  });

  it("Emergency Contact Info", function () {
    patientPage.emergencyDropDown();
    cy.fixture("Patient").then((patient) => {
      patientPage.emergencyContactLastName(patient.personContactLastName);
      patientPage.emergencyContactFirstName(patient.personContactFirstName);
      patientPage.emergencyContactPhone(patient.personContactPrimaryPhone);
      patientPage.emergencyContactEmail(patient.personContactEmail);
    });
  });

  it("Additional Information", function () {
    patientPage.additionalInformationDropDown();
    cy.fixture("Patient").then((patient) => {
      patientPage.enterTown(patient.town);
      patientPage.enterStreet(patient.street);
      patientPage.enterCamp(patient.camp);
      patientPage.selectRegion(patient.region);
      patientPage.selectDistrict(patient.district);
      patientPage.selectEducation(patient.education);
      patientPage.selectMaritalStatus(patient.maritalStatus);
      patientPage.selectNationality(patient.nationality);
      patientPage.selectOtherNationality(patient.otherNationality);
    });
  });
  it("User saves new patient information", function () {
    cy.wait(500);
    patientPage.clickSavePatientButton();
  });
});

//SEARCH PATIENT
describe("Search for Patient", function () {
  it("User Searches for patient by First Name", function () {
    patientPage.clickSearchPatientBtn();
    cy.fixture("Patient").then((patient) => {
      patientPage.patientFirstName(patient.firstName);
      patientPage.clickSearchBtn();
    });
    cy.wait(200).reload();
  });

  it("User Searches for patient by lastst Name", function () {
    patientPage.clickSearchPatientBtn();
    cy.fixture("Patient").then((patient) => {
      patientPage.patientLastName(patient.lastName);
      patientPage.clickSearchBtn();
    });
    cy.wait(200).reload();
  });

  it("Search patients By gender", function () {
    patientPage.getMaleGenderRadioButton();
    cy.wait(200);
    patientPage.clickSearchBtn();
    cy.wait(200).reload();
  });

  it("Search patient By Date Of Birth", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.patientDateOfBirth(patient.dOB);
    });
    patientPage.clickSearchBtn();
    cy.wait(200).reload();
  });

  it("Search patient By previous Lab Number", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientBylabNo(patient.labNo);
    });
    patientPage.clickSearchBtn();
    cy.wait(200).reload();
  });

  it("Search patient By PatientId", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByPatientId(patient.uniqueID);
    });
    patientPage.clickSearchBtn();
    cy.wait(200).reload();
  });

  it("Search Patient By First and Last Name", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.patientFirstName(patient.firstName);
      patientPage.patientLastName(patient.lastName);
    });
    patientPage.clickSearchBtn();
    patientPage.selectPatient();
  });
});
