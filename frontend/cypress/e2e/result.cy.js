import LoginPage from "../pages/LoginPage";
import PatientEntryPage from "../pages/PatientEntryPage";

let homePage = null;
let loginPage = null;
let result = null;
let patientPage = new PatientEntryPage();

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Result By Unit", function () {
  before("Navigate to Result By Unit", function () {
    homePage = loginPage.goToHomePage();
    result = homePage.goToResultsByUnit();
  });

  it("User validates Results Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle(res.pageTitle);
    });
  });

  it("Should Search by Unit", function () {
    cy.fixture("workplan").then((order) => {
      result.selectUnitType(order.unitType);
    });
  });

  it("should accept the sample, refer the sample, and save the result", function () {
    result.expandSampleDetails();
    cy.fixture("result").then((res) => {
      result.selectTestMethod(res.pcrTestMethod);
      result.referTests(res.referTests);
      result.referralReason(res.referalReason);
      result.selectInstitute(res.cedres);
      result.selectResultValue(res.negativeResult);
    });
    result.submitResults();
  });
});

describe("Result By Patient", function () {
  it("Navigate to Result By Patient", function () {
    homePage = loginPage.goToHomePage();
    result = homePage.goToResultsByPatient();
  });

  it("User visits Results Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle(res.pageTitle);
    });
  });

  it("Search Patient By First and Last Name and validate", function () {
    cy.wait(1000);
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
    cy.reload();
  });

  it("should search patient By PatientId and validate", function () {
    cy.wait(500);
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByPatientId(patient.nationalId);
      patientPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });
    cy.reload();
  });

  it("Search by sex", function () {
    patientPage.getMaleGenderRadioButton();
    patientPage.clickSearchPatientButton();
    cy.reload();
  });
  it("should search patient By Lab Number and validate", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.enterPreviousLabNumber(patient.labNo);
      patientPage.clickSearchPatientButton();
    });
    cy.reload();
  });

  it("Search by respective patient and accept the result", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.lastName,
      );
    });
    patientPage.getMaleGenderRadioButton();
    patientPage.clickSearchPatientButton();
    cy.wait(1000);
    result.selectPatientFromSearchResults();
    cy.wait(1200);
    result.expandSampleDetails();
    cy.fixture("result").then((res) => {
      result.selectTestMethod(res.pcrTestMethod);
      result.referTests(res.referTests);
      result.referralReason(res.referalReason);
      result.selectInstitute(res.cedres);
      //result.setResultValue(res.resultNo);
    });
    result.submitResults();
  });
});

describe("Result By Order", function () {
  before("navigate to Result By Order", function () {
    homePage = loginPage.goToHomePage();
    result = homePage.goToResultsByOrder();
  });

  it("User visits Results Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle(res.pageTitle);
    });
  });

  it("Should Search by Accession Number", function () {
    cy.fixture("Patient").then((order) => {
      patientPage.enterAccessionNumber(order.labNo);
    });
    result.searchResults();
    cy.wait(900);
  });

  it("should accept the sample and save the result", function () {
    result.expandSampleDetails();
    cy.fixture("result").then((res) => {
      result.selectTestMethod(res.pcrTestMethod);
      result.referTests(res.referTests);
      result.referralReason(res.referalReason);
      result.selectInstitute(res.cedres);
      //result.setResultValue(res.resultNo);
    });
    result.submitResults();
  });
});

describe("Result By Referred Out Tests", function () {
  before("Navigate to Result By Referred Out Tests", function () {
    homePage = loginPage.goToHomePage();
    result = homePage.goToResultsForRefferedOut();
  });

  it("Navigate to Reffered out Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle(res.referrals);
    });
  });

  it("Search by respective patient and accept the result", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.lastName,
      );
    });
    patientPage.clickSearchPatientButton();
    cy.wait(900);
    result.selectPatientFromSearchResults();
    result.clickReferralsByPatient();
  });

  it("Validation that the patient exists in the reports table", function () {
    result.selectAllButtonEnabled(); //wont be if patient does not exist
    result.clickSelectAllButton();
    result.selectNoneButtonEnabled();
    result.printReportsButtonEnabled();
    cy.reload();
  });

  it("Referrals by Sent Date", function () {
    cy.fixture("result").then((res) => {
      result.selectSentDate();
      result.startDate(res.startDate);
      result.endDate(res.endDate);
    });
    result.clickReferralsByTestAndName();
    result.selectAllButtonEnabled(); //wont be if patient does not exist
    result.clickSelectAllButton();
    result.selectNoneButtonEnabled();
    result.printReportsButtonEnabled();
    cy.reload();
  });

  it("Referrals by Result Date", function () {
    cy.fixture("result").then((res) => {
      result.selectResultDate();
      result.startDate(res.startDate);
      result.endDate(res.endDate);
    });
    result.clickReferralsByTestAndName();
    result.selectAllButtonEnabled(); //wont be if patient does not exist
    result.clickSelectAllButton();
    result.selectNoneButtonEnabled();
    result.printReportsButtonEnabled();
    cy.reload();
  });

  it("Referrals by Test Unit and validate", function () {
    cy.fixture("workplan").then((res) => {
      result.unitType(res.unitType);
      result.unitTypeItem();
      result.clickDateButton();
    });
    result.clickReferralsByTestAndName();
    result.selectAllButtonEnabled(); //wont be if patient does not exist
    result.clickSelectAllButton();
    result.selectNoneButtonEnabled();
    result.printReportsButtonEnabled();
    cy.reload();
  });

  it("Referrals by Test Name and validate", function () {
    cy.fixture("workplan").then((res) => {
      result.testName(res.testName);
      result.testNameItem();
      result.clickDateButton();
    });
    result.clickReferralsByTestAndName();
    result.selectAllButtonEnabled(); //wont be if patient does not exist
    result.clickSelectAllButton();
    result.selectNoneButtonEnabled();
    result.printReportsButtonEnabled();
    cy.reload();
  });

  it("search Referrals By LabNumber and validate", function () {
    cy.fixture("Patient").then((order) => {
      result.resultsByLabNumber(order.labNo);
    });
    result.clickReferralsByLabNumber();
    result.selectAllButtonEnabled(); //wont be if patient does not exist
    result.clickSelectAllButton();
    result.selectNoneButtonEnabled();
    result.printReportsButtonEnabled();
  });
});

describe("Result By Range Of Order", function () {
  before("navigate to Result By Range Of Order", function () {
    homePage = loginPage.goToHomePage();
    result = homePage.goToResultsByRangeOrder();
  });

  it("User visits Results Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle(res.pageTitle);
    });
  });

  it("Enter Lab Numbers and Search", function () {
    cy.fixture("Patient").then((order) => {
      result.startLabNumber(order.labNo);
      result.endLabNo(order.endLabNo);
    });
    result.searchResults();
  });

  it("Accept And Save the result", function () {
    result.expandSampleDetails();
    cy.fixture("result").then((res) => {
      result.selectTestMethod(res.pcrTestMethod);
    });
    result.submitResults();
  });
});

describe("Result By Test And Status", function () {
  // This will run before every test case
  beforeEach("Navigate to Results page", function () {
    homePage = loginPage.goToHomePage();
    result = homePage.goToResultsByTestAndStatus();
  });

  it("User visits Results Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle(res.pageTitle);
    });
  });

  it("Search by TestName", function () {
    cy.fixture("workplan").then((order) => {
      result.selectTestName(order.testName);
      result.searchResults();
      result.expandSampleDetails();
    });
    cy.fixture("result").then((res) => {
      result.selectTestMethod(res.pcrTestMethod);
    });
    result.submitResults();
  });

  it("Search by Collection Date", function () {
    cy.fixture("result").then((res) => {
      result.enterCollectionDate();
      result.clickReceivedDate();
      result.searchResults();
      result.expandSampleDetails();
      result.selectTestMethod(res.pcrTestMethod);
    });
    result.submitResults();
  });

  it("Search by Received Date", function () {
    cy.fixture("result").then((res) => {
      result.enterReceivedDate();
      result.searchResults();
      result.expandSampleDetails();
      result.selectTestMethod(res.pcrTestMethod);
    });
    result.submitResults();
  });

  it("Search by Sample status", function () {
    cy.fixture("result").then((res) => {
      result.sampleStatus(res.sample);
      result.searchResults();
      result.expandSampleDetails();
      result.selectTestMethod(res.pcrTestMethod);
    });
    result.submitResults();
  });

  it("Search by Test Analysis", function () {
    cy.fixture("result").then((res) => {
      result.selectAnalysisStatus(res.analysisStatus);
      result.searchResults();
      result.expandSampleDetails();
      result.selectTestMethod(res.pcrTestMethod);
    });
    result.submitResults();
  });
});
