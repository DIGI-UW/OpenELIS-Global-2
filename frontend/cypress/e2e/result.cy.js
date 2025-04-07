import LoginPage from "../pages/LoginPage";
import PatientEntryPage from "../pages/PatientEntryPage";

let homePage = null;
let loginPage = null;
let result = null;
let patientPage = null;

describe("Results Tests", () => {
  beforeEach(() => {
    // Setup before each test
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    patientPage = new PatientEntryPage();
  });

  describe("Result By Unit", () => {
    beforeEach(() => {
      result = homePage.goToResultsByUnit();
    });

    it("should display results page with correct title", () => {
      cy.fixture("result").then((res) => {
        result.getResultTitle().should("contain.text", res.pageTitle);
      });
    });

    it("should search by unit and submit results", () => {
      cy.fixture("workplan").then((order) => {
        result.selectUnitType(order.unitBioType);
      });

      cy.fixture("result").then((res) => {
        result.setResultValue(0, res.positiveResult);
        result.submitResults();
      });
    });
  });

  describe("Result By Patient", () => {
    beforeEach(() => {
      result = homePage.goToResultsByPatient();
    });

    it("should display results page with correct title", () => {
      cy.fixture("result").then((res) => {
        result.getResultTitle().should("contain.text", res.pageTitle);
      });
    });

    it("should search patient by first and last name", () => {
      cy.fixture("Patient").then((patient) => {
        patientPage.searchPatientByFirstAndLastName(
          patient.firstName,
          patient.lastName,
        );
        patientPage.getFirstName().should("have.value", patient.firstName);
        patientPage.getLastName().should("have.value", patient.lastName);
        patientPage.getLastName().should("not.have.value", patient.inValidName);
        patientPage.clickSearchPatientButton();
        patientPage.validatePatientSearchTable(
          patient.firstName,
          patient.inValidName,
        );
      });
    });

    it("should search patient by patient ID", () => {
      cy.fixture("Patient").then((patient) => {
        patientPage.searchPatientByPatientId(patient.nationalId);
        patientPage.clickSearchPatientButton();
        patientPage.validatePatientSearchTable(
          patient.firstName,
          patient.inValidName,
        );
      });
    });

    it("should search patient by lab number", () => {
      cy.fixture("Patient").then((patient) => {
        cy.get("#labNumber").type(patient.labNo);
        patientPage.clickSearchPatientButton();
      });
    });

    it("should search by respective patient and accept the result", () => {
      cy.fixture("Patient").then((patient) => {
        patientPage.searchPatientByFirstAndLastName(
          patient.firstName,
          patient.lastName,
        );
      });
      patientPage.getMaleGenderRadioButton();
      patientPage.clickSearchPatientButton();

      result.selectPatientFromSearchResults();

      cy.fixture("result").then((res) => {
        result.selectResultValue(0, res.invalidResult);
      });
      result.submitResults();
    });
  });

  describe("Result By Order", () => {
    beforeEach(() => {
      result = homePage.goToResultsByOrder();
    });

    it("should display results page with correct title", () => {
      cy.fixture("result").then((res) => {
        result.getResultTitle().should("contain.text", res.pageTitle);
      });
    });

    it("should search by accession number and save results", () => {
      cy.fixture("Patient").then((order) => {
        cy.get("#accessionNumber").type(order.labNo);
      });
      result.searchResults();

      cy.fixture("result").then((res) => {
        result.setResultValue(0, res.positiveResult);
      });
      result.submitResults();
    });
  });

  describe("Result By Referred Out Tests", () => {
    beforeEach(() => {
      result = homePage.goToResultsForRefferedOut();
    });

    it("should display referred out page with correct title", () => {
      cy.fixture("result").then((res) => {
        result.getResultTitle().should("contain.text", res.referralPageTitle);
      });
    });

    it("should search referrals by patient", () => {
      cy.fixture("Patient").then((patient) => {
        patientPage.searchPatientByPatientId(patient.nationalId);
        patientPage.searchPatientByFirstAndLastName(
          patient.firstName,
          patient.lastName,
        );
        patientPage.getFirstName().should("have.value", patient.firstName);
        patientPage.getLastName().should("have.value", patient.lastName);
        patientPage.clickSearchPatientButton();
        patientPage.validatePatientSearchTable(
          patient.firstName,
          patient.inValidName,
        );
      });

      cy.fixture("Patient").then((patient) => {
        result.validatePatientResult(patient);
      });
      patientPage.selectPatientFromSearchResults();
      result.clickReferralsByPatient();
    });

    it("should search referrals by test unit and name", () => {
      cy.fixture("workplan").then((res) => {
        cy.get("#testnames-input").type(res.testName);
        cy.get("#testnames-item-0-item").click();
        cy.get("#testunits-input").type(res.unitType);
        cy.get("#testunits-item-0-item").click();
      });
      result.clickReferralsByTestAndName();
    });

    it("should search referrals by lab number", () => {
      cy.fixture("Patient").then((order) => {
        cy.get("#labNumberInput").type(order.labNo);
      });
      result.clickReferralsByLabNumber();
    });
  });
});
