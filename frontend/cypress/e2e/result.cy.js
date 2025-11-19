import LoginPage from "../pages/LoginPage";
import PatientEntryPage from "../pages/PatientEntryPage";

let homePage = null;
let loginPage = null;
let result = null;
let patientPage = new PatientEntryPage();

before("Load test fixtures", () => {
  // Wait for backend API to be available before loading fixtures
  cy.waitForBackend("/rest/storage/samples");
  // Load test data (patients, samples, results) needed for result tests
  cy.loadStorageFixtures();
});

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
    // Wait for search results to load before expanding
    cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
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
    cy.fixture("Patient").then((patient) => {
      // Search by first and last name (truncation issue fixed by using TEST- prefix)
      patientPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.lastName,
      );
      patientPage.getFirstName().should("have.value", patient.firstName);
      patientPage.clickSearchPatientButton();
      // Use Cypress retry-ability - wait for search results table to appear with rows
      cy.get(".cds--data-table tbody")
        .should("exist")
        .find("tr")
        .should("have.length.greaterThan", 0);
      // Validate - check for first name in results (last name may be truncated in display)
      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });
    cy.reload();
  });

  it("should search patient By PatientId and validate", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByPatientId(patient.nationalId);
      patientPage.clickSearchPatientButton();
      // Use Cypress retry-ability - wait for search results table to appear with rows
      cy.get(".cds--data-table tbody")
        .should("exist")
        .find("tr")
        .should("have.length.greaterThan", 0);
      // Validate last name column - pass last name, not first name
      patientPage.validatePatientSearchTable(
        patient.lastName,
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
      // Search by first name only to avoid last name truncation
      patientPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.firstName, // Use first name to avoid truncation
      );
    });
    patientPage.getMaleGenderRadioButton();
    patientPage.clickSearchPatientButton();
    // Use Cypress retry-ability - wait for search results table to appear with rows
    cy.get(".cds--data-table tbody")
      .should("exist")
      .find("tr")
      .should("have.length.greaterThan", 0);
    result.selectPatientFromSearchResults();
    // Wait for results table to load after selecting patient
    cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
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
    // Set up intercept for search API call
    cy.intercept("GET", "**/rest/LogbookResults?*").as("searchResults");
    // Use "E2E" (3 chars) to match backend query which requires exact length match
    // Test data includes sample 1005 with accession "E2E" and unfinished analysis 20010
    patientPage.enterAccessionNumber("E2E");
    // Click search button
    cy.get("#searchResults").should("be.visible").click();
    // Wait for API response
    cy.wait("@searchResults").then((interception) => {
      // Verify the request was made with correct parameters
      expect(interception.request.url).to.include("labNumber=E2E");
      // Log full response for debugging
      cy.log("Full request URL: " + interception.request.url);
      if (interception.response) {
        cy.log("Response status: " + interception.response.statusCode);
        if (interception.response.body) {
          const body = interception.response.body;
          cy.log("Response body type: " + typeof body);
          if (typeof body === "object" && body !== null) {
            const keys = Object.keys(body);
            cy.log("Response body keys: " + keys.join(", "));
            if (body.testResult) {
              cy.log(`testResult array length: ${body.testResult.length}`);
              if (body.testResult.length > 0) {
                cy.log(
                  "First result: " +
                    JSON.stringify(body.testResult[0]).substring(0, 200),
                );
              } else {
                cy.log("testResult is empty - this is why tbody doesn't exist");
              }
            } else {
              cy.log("No testResult property in response");
              cy.log(
                "Full response (first 500 chars): " +
                  JSON.stringify(body).substring(0, 500),
              );
            }
          } else {
            cy.log("Response body is not an object: " + body);
          }
        } else {
          cy.log("No response body");
        }
      } else {
        cy.log("No response object");
      }
    });
    // Wait for table to appear with results using Cypress retry-ability
    cy.get("tbody")
      .should("exist")
      .find("tr")
      .should("have.length.greaterThan", 0);
  });

  it("should accept the sample and save the result", function () {
    // Wait for search results to load before expanding (results from previous test may still be visible)
    cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
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
    // Wait for search results to appear
    cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
    result.selectPatientFromSearchResults();
    result.clickReferralsByPatient();
  });

  it("Validation that the patient exists in the reports table", function () {
    // Wait for table to load before checking buttons
    cy.get("tbody").should("exist");
    cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
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
    // Wait for table to load after search
    cy.get("tbody").should("exist");
    cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
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
    // Wait for table to load after search
    cy.get("tbody").should("exist");
    cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
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
    // Wait for table to load after search
    cy.get("tbody").should("exist");
    cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
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
    // Wait for table to load after search
    cy.get("tbody").should("exist");
    cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
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
    // Wait for table to load after search
    cy.get("tbody").should("exist");
    cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
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
    // Wait for search results to load before expanding
    cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
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
      // Wait for search results to load before expanding
      cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
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
      // Wait for search results to load before expanding
      cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
      result.expandSampleDetails();
      result.selectTestMethod(res.pcrTestMethod);
    });
    result.submitResults();
  });

  it("Search by Received Date", function () {
    cy.fixture("result").then((res) => {
      result.enterReceivedDate();
      result.searchResults();
      // Wait for search results to load before expanding
      cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
      result.expandSampleDetails();
      result.selectTestMethod(res.pcrTestMethod);
    });
    result.submitResults();
  });

  it("Search by Sample status", function () {
    cy.fixture("result").then((res) => {
      result.sampleStatus(res.sample);
      result.searchResults();
      // Wait for search results to load before expanding
      cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
      result.expandSampleDetails();
      result.selectTestMethod(res.pcrTestMethod);
    });
    result.submitResults();
  });

  it("Search by Test Analysis", function () {
    cy.fixture("result").then((res) => {
      result.selectAnalysisStatus(res.analysisStatus);
      result.searchResults();
      // Wait for search results to load before expanding
      cy.get("tbody tr").should("exist").should("have.length.greaterThan", 0);
      result.expandSampleDetails();
      result.selectTestMethod(res.pcrTestMethod);
    });
    result.submitResults();
  });
});
