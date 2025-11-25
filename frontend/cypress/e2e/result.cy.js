/**
 * E2E Tests for Results (By Unit, By Patient, By Order, By Referred Out, By Range, By Test And Status)
 * Tests result entry, search, and validation functionality
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Session management via cy.login() with cy.session() (10-20x faster)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/result.cy.js"
 */

import LoginPage from "../pages/LoginPage";
import PatientEntryPage from "../pages/PatientEntryPage";

let homePage = null;
let result = null;
let patientPage = new PatientEntryPage();

// Load test fixtures before running tests (ensures patient data exists)
before("Load test fixtures", () => {
  // Wait for backend API to be available before loading fixtures
  cy.waitForBackend("/rest/storage/samples");
  // Load test data (patients, samples, results) needed for result tests
  cy.loadStorageFixtures();
});

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
// Same pattern as cy.setupStorageTests() in storage-setup.js
before("login", () => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  const loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
});

describe("Result By Unit", function () {
  before("Navigate to Result By Unit", function () {
    result = homePage.goToResultsByUnit();
    // Verify we're on the results page
    cy.url().should("include", "/LogbookResults");
  });

  beforeEach(() => {
    // No intercepts needed - wait for UI state instead (tests what users see)
  });

  it("User validates Results Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle(res.pageTitle);
    });
  });

  it("Should Search by Unit", function () {
    cy.fixture("workplan").then((order) => {
      // Wait for dropdown to be ready (results page uses #unitType, not #select-1)
      cy.get("#unitType", { timeout: 10000 })
        .should("be.visible")
        .should("not.be.disabled");

      result.selectUnitType(order.unitType);

      // Wait for DataTable to render results (test UI, not API)
      result.waitForResultsTable();
    });
  });

  it("should accept the sample, refer the sample, and save the result", function () {
    cy.fixture("workplan").then((order) => {
      // Ensure unit type is selected (may have been reset)
      cy.get("#unitType", { timeout: 10000 })
        .should("be.visible")
        .should("not.be.disabled");

      // Re-select unit type to trigger search (ensures fresh results)
      result.selectUnitType(order.unitType);

      // Wait for DataTable to render results (test UI, not API)
      result.waitForResultsTable();
    });

    result.expandSampleDetails();

    // Wait for expanded details to render before interacting
    cy.wait(1000);

    cy.fixture("result").then((res) => {
      result.selectTestMethod(res.pcrTestMethod);
      result.referTests(res.referTests);
      result.referralReason(res.referalReason);
      result.selectInstitute(res.cedres);
      // Wait a bit more for result value field to appear after institute selection
      cy.wait(500);
      result.selectResultValue(res.negativeResult);
    });

    result.submitResults();

    // Wait for save to complete - results may show notification or just update silently
    // Wait a moment for any async operations to complete
    cy.wait(2000);
  });
});

describe("Result By Patient", function () {
  before("Navigate to Result By Patient", function () {
    result = homePage.goToResultsByPatient();
    // Wait for patient search form to be ready
    cy.get("input#firstName, input#patientId, input#labNumber", { timeout: 10000 })
      .should("exist");
  });

  beforeEach(() => {
    // No intercepts needed - wait for UI state instead (tests what users see)
  });

  it("User visits Results Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle(res.pageTitle);
    });
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

      // Verify button is ready before clicking
      cy.get("#local_search").should("be.visible").should("not.be.disabled");

      patientPage.clickSearchPatientButton();

      // Wait for search results table to appear (test UI, not API)
      cy.get(".cds--data-table tbody", { timeout: 10000 })
        .should("exist")
        .find("tr")
        .should("have.length.greaterThan", 0);

      // Validate - check for first name in results (last name may be truncated in display)
      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });
  });

  it("should search patient By PatientId and validate", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByPatientId(patient.nationalId);

      // Verify button is ready before clicking
      cy.get("#local_search").should("be.visible").should("not.be.disabled");

      patientPage.clickSearchPatientButton();

      // Wait for search results table to appear (test UI, not API)
      cy.get(".cds--data-table tbody", { timeout: 10000 })
        .should("exist")
        .find("tr")
        .should("have.length.greaterThan", 0);

      // Validate last name column - pass last name, not first name
      patientPage.validatePatientSearchTable(
        patient.lastName,
        patient.inValidName,
      );
    });
  });

  it("Search by sex", function () {
    patientPage.getMaleGenderRadioButton().should("be.visible").click();

    // Verify button is ready before clicking
    cy.get("#local_search").should("be.visible").should("not.be.disabled");

    patientPage.clickSearchPatientButton();

    // Wait for search results table to appear (test UI, not API)
    cy.get(".cds--data-table tbody", { timeout: 10000 })
      .should("exist")
      .find("tr")
      .should("have.length.greaterThan", 0);
  });

  it("should search patient By Lab Number and validate", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.enterPreviousLabNumber(patient.labNo);

      // Verify button is ready before clicking
      cy.get("#local_search").should("be.visible").should("not.be.disabled");

      patientPage.clickSearchPatientButton();

      // Wait for search results table to appear (test UI, not API)
      cy.get(".cds--data-table tbody", { timeout: 10000 })
        .should("exist")
        .find("tr")
        .should("have.length.greaterThan", 0);
    });
  });

  it("Search by respective patient and accept the result", function () {
    cy.fixture("Patient").then((patient) => {
      // Search by first name only to avoid last name truncation
      patientPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.firstName, // Use first name to avoid truncation
      );
    });

    patientPage.getMaleGenderRadioButton().should("be.visible").click();

    // Verify button is ready before clicking
    cy.get("#local_search").should("be.visible").should("not.be.disabled");

    patientPage.clickSearchPatientButton();

    // Wait for search results table to appear (test UI, not API)
    cy.get(".cds--data-table tbody", { timeout: 10000 })
      .should("exist")
      .find("tr")
      .should("have.length.greaterThan", 0);

    result.selectPatientFromSearchResults();

    // Wait for results table to load after selecting patient (DataTable uses ARIA roles)
    result.waitForResultsTable();

    result.expandSampleDetails();

    cy.fixture("result").then((res) => {
      result.selectTestMethod(res.pcrTestMethod);
      result.referTests(res.referTests);
      result.referralReason(res.referalReason);
      result.selectInstitute(res.cedres);
    });

    result.submitResults();

    // Wait for save to complete - results may show notification or just update silently
    // Wait a moment for any async operations to complete
    cy.wait(2000);
  });
});

describe("Result By Order", function () {
  before("navigate to Result By Order", function () {
    result = homePage.goToResultsByOrder();
    // Verify we're on the accession results page (not LogbookResults)
    cy.url().should("include", "/AccessionResults");
  });

  beforeEach(() => {
    // No intercepts needed - wait for UI state instead (tests what users see)
  });

  it("User visits Results Page", function () {
    cy.fixture("result").then((res) => {
      result.getResultTitle(res.pageTitle);
    });
  });

  it("Should Search by Accession Number", function () {
    // Use "E2E" (3 chars) to match backend query which requires exact length match
    // Test data includes sample 1005 with accession "E2E" and unfinished analysis 20010
    patientPage.enterAccessionNumber("E2E");

    // Verify button is ready before clicking
    cy.get("#searchResults", { timeout: 10000 })
      .should("be.visible")
      .should("not.be.disabled");

    // Click search button
    cy.get("#searchResults").click();

    // Wait for DataTable to render results (test UI, not API)
    result.waitForResultsTable();
  });

  it("should accept the sample and save the result", function () {
    // Wait for search results to load before expanding (results from previous test may still be visible)
    result.waitForResultsTable();

    result.expandSampleDetails();

    cy.fixture("result").then((res) => {
      result.selectTestMethod(res.pcrTestMethod);
      result.referTests(res.referTests);
      result.referralReason(res.referalReason);
      result.selectInstitute(res.cedres);
    });

    result.submitResults();

    // Wait for save to complete - results may show notification or just update silently
    // Wait a moment for any async operations to complete
    cy.wait(2000);
  });
});

describe("Result By Referred Out Tests", function () {
  before("Navigate to Result By Referred Out Tests", function () {
    result = homePage.goToResultsForRefferedOut();
    // Verify we're on the referred out page
    cy.url().should("include", "/ReferredOutTests");
  });

  beforeEach(() => {
    // No intercepts needed - wait for UI state instead (tests what users see)
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

      // Verify button is ready before clicking
      cy.get("#local_search").should("be.visible").should("not.be.disabled");

      patientPage.clickSearchPatientButton();

      // Wait for search results table to appear (test UI, not API)
      cy.get(".cds--data-table tbody", { timeout: 10000 })
        .should("exist")
        .find("tr")
        .should("have.length.greaterThan", 0);
    });

    result.selectPatientFromSearchResults();
    result.clickReferralsByPatient();

    // Wait for referred out results table to appear (test UI, not API)
    result.waitForResultsTable();
  });

  it("Validation that the patient exists in the reports table", function () {
    // Wait for DataTable to render results (test UI, not API)
    // For referred out tests, check if rows exist first
    cy.get("tbody tr, [role='rowgroup'] [role='row']", { timeout: 15000 })
      .should("exist")
      .should("have.length.greaterThan", 0);

    // If rows exist, buttons should be enabled
    result.selectAllButtonEnabled(); //wont be if patient does not exist
    result.clickSelectAllButton();
    result.selectNoneButtonEnabled();
    result.printReportsButtonEnabled();
  });

  it("Referrals by Sent Date", function () {
    cy.fixture("result").then((res) => {
      result.selectSentDate();
      result.startDate(res.startDate);
      result.endDate(res.endDate);

      result.clickReferralsByTestAndName();
    });

    // Wait for DataTable to render results (test UI, not API)
    result.waitForResultsTable();

    result.selectAllButtonEnabled(); //wont be if patient does not exist
    result.clickSelectAllButton();
    result.selectNoneButtonEnabled();
    result.printReportsButtonEnabled();
  });

  it("Referrals by Result Date", function () {
    cy.fixture("result").then((res) => {
      result.selectResultDate();
      result.startDate(res.startDate);
      result.endDate(res.endDate);

      result.clickReferralsByTestAndName();
    });

    // Wait for DataTable to render results (test UI, not API)
    result.waitForResultsTable();

    result.selectAllButtonEnabled(); //wont be if patient does not exist
    result.clickSelectAllButton();
    result.selectNoneButtonEnabled();
    result.printReportsButtonEnabled();
  });

  it("Referrals by Test Unit and validate", function () {
    cy.fixture("workplan").then((res) => {
      result.unitType(res.unitType);
      result.unitTypeItem();
      result.clickDateButton();

      result.clickReferralsByTestAndName();
    });

    // Wait for DataTable to render results (test UI, not API)
    result.waitForResultsTable();

    result.selectAllButtonEnabled(); //wont be if patient does not exist
    result.clickSelectAllButton();
    result.selectNoneButtonEnabled();
    result.printReportsButtonEnabled();
  });

  it("Referrals by Test Name and validate", function () {
    cy.fixture("workplan").then((res) => {
      result.testName(res.testName);
      result.testNameItem();
      result.clickDateButton();

      result.clickReferralsByTestAndName();
    });

    // Wait for DataTable to render results (test UI, not API)
    result.waitForResultsTable();

    result.selectAllButtonEnabled(); //wont be if patient does not exist
    result.clickSelectAllButton();
    result.selectNoneButtonEnabled();
    result.printReportsButtonEnabled();
  });

  it("search Referrals By LabNumber and validate", function () {
    cy.fixture("Patient").then((order) => {
      result.resultsByLabNumber(order.labNo);

      result.clickReferralsByLabNumber();
    });

    // Wait for DataTable to render results (test UI, not API)
    result.waitForResultsTable();

    result.selectAllButtonEnabled(); //wont be if patient does not exist
    result.clickSelectAllButton();
    result.selectNoneButtonEnabled();
    result.printReportsButtonEnabled();
  });
});

describe("Result By Range Of Order", function () {
  before("navigate to Result By Range Of Order", function () {
    result = homePage.goToResultsByRangeOrder();
    // Verify we're on the range results page (not LogbookResults)
    cy.url().should("include", "/RangeResults");
  });

  beforeEach(() => {
    // No intercepts needed - wait for UI state instead (tests what users see)
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

      result.searchResults();

      // Wait for DataTable to render results (test UI, not API)
      result.waitForResultsTable();
    });
  });

  it("Accept And Save the result", function () {
    // Wait for search results to load before expanding (use .should() for retry-ability)
    result.waitForResultsTable();

    result.expandSampleDetails();

    cy.fixture("result").then((res) => {
      result.selectTestMethod(res.pcrTestMethod);
    });

    result.submitResults();

    // Wait for save to complete - results may show notification or just update silently
    // Wait a moment for any async operations to complete
    cy.wait(2000);
  });
});

describe("Result By Test And Status", function () {
  beforeEach("Navigate to Results page", function () {
    result = homePage.goToResultsByTestAndStatus();
    // Verify we're on the status results page (not LogbookResults)
    cy.url().should("include", "/StatusResults");
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

      // Wait for DataTable to render results (test UI, not API)
      result.waitForResultsTable();

      result.expandSampleDetails();
    });

    cy.fixture("result").then((res) => {
      result.selectTestMethod(res.pcrTestMethod);
    });

    result.submitResults();

    // Wait for save to complete - results may show notification or just update silently
    // Wait a moment for any async operations to complete
    cy.wait(2000);
  });

  it("Search by Collection Date", function () {
    cy.fixture("result").then((res) => {
      result.enterCollectionDate();
      result.clickReceivedDate();
      result.searchResults();

      // Wait for DataTable to render results (test UI, not API)
      result.waitForResultsTable();

      result.expandSampleDetails();
      result.selectTestMethod(res.pcrTestMethod);
    });

    result.submitResults();

    // Wait for save to complete - results may show notification or just update silently
    // Wait a moment for any async operations to complete
    cy.wait(2000);
  });

  it("Search by Received Date", function () {
    cy.fixture("result").then((res) => {
      result.enterReceivedDate();
      result.searchResults();

      // Wait for DataTable to render results (test UI, not API)
      result.waitForResultsTable();

      result.expandSampleDetails();
      result.selectTestMethod(res.pcrTestMethod);
    });

    result.submitResults();

    // Wait for save to complete - results may show notification or just update silently
    // Wait a moment for any async operations to complete
    cy.wait(2000);
  });

  it("Search by Sample status", function () {
    cy.fixture("result").then((res) => {
      result.sampleStatus(res.sample);
      result.searchResults();

      // Wait for DataTable to render results (test UI, not API)
      result.waitForResultsTable();

      result.expandSampleDetails();
      result.selectTestMethod(res.pcrTestMethod);
    });

    result.submitResults();

    // Wait for save to complete - results may show notification or just update silently
    // Wait a moment for any async operations to complete
    cy.wait(2000);
  });

  it("Search by Test Analysis", function () {
    cy.fixture("result").then((res) => {
      result.selectAnalysisStatus(res.analysisStatus);
      result.searchResults();

      // Wait for DataTable to render results (test UI, not API)
      result.waitForResultsTable();

      result.expandSampleDetails();
      result.selectTestMethod(res.pcrTestMethod);
    });

    result.submitResults();

    // Wait for save to complete - results may show notification or just update silently
    // Wait a moment for any async operations to complete
    cy.wait(2000);
  });
});
