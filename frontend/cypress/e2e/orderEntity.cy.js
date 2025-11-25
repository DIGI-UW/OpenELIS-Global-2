/**
 * E2E Tests for Order Entity
 * Tests order creation workflow from patient search to submission
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Session management via cy.login() with cy.session() (10-20x faster)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/orderEntity.cy.js"
 */

import LoginPage from "../pages/LoginPage";
import AdminPage from "../pages/AdminPage";

let homePage = null;
let adminPage = new AdminPage();
let orderEntityPage = null;
let patientEntryPage = null;

// Load test fixtures before running tests (ensures patient data exists)
// Order entity tests search for patients, so fixtures must be loaded first
before("load fixtures", () => {
  cy.loadStorageFixtures();
  // Verify fixtures loaded correctly (including patient E2E-PAT-001)
  cy.task("verifyFixtures").then((result) => {
    if (!result || result.status !== "OK") {
      cy.log("⚠️  WARNING: Fixture verification failed");
      cy.log(`Verification result: ${JSON.stringify(result)}`);
      // Don't fail here - let tests run and fail with clear error messages
    } else {
      cy.log(
        "✅ Fixtures verified - patient data ready for order entity tests",
      );
    }
  });
});

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
// Same pattern as cy.setupStorageTests() in storage-setup.js
before("login", () => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  const loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
});

describe("Order Entity", function () {
  beforeEach(() => {
    // No intercepts needed - wait for UI state instead (tests what users see)
  });

  it("Navigate to Home Page then to Order entity Page ", function () {
    orderEntityPage = homePage.goToOrderPage();
    // Verify we're on the order page
    cy.url().should("satisfy", (url) => {
      return url.includes("/AddOrder") || url.includes("/SamplePatientEntry");
    });
  });

  it("Search patient in the search box", function () {
    patientEntryPage = orderEntityPage.getPatientPage();
    orderEntityPage.verifyPatientInfoPage();

    cy.fixture("Patient").then((patient) => {
      patientEntryPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.lastName,
      );

      // Verify button is ready before clicking
      cy.get("#local_search").should("be.visible").should("not.be.disabled");

      patientEntryPage.clickSearchPatientButton();

      // Wait for table to show results (test UI, not API)
      cy.get("table tbody tr", { timeout: 10000 })
        .should("be.visible")
        .should("have.length.greaterThan", 0);

      patientEntryPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );

      patientEntryPage.selectPatientFromSearchResults();

      // Wait for form to populate (use .should() for retry-ability)
      patientEntryPage.getFirstName().should("have.value", patient.firstName);
      patientEntryPage.getLastName().should("have.value", patient.lastName);
    });

    orderEntityPage.clickNextButton();
    // Wait for navigation to complete and page to render
    orderEntityPage.waitForProgramSelectionPage();
  });

  it("Navigate to program selection", function () {
    // Page should already be on program selection, but verify to be safe
    orderEntityPage.verifyProgramSelectionPage();

    orderEntityPage.selectCytology();
    orderEntityPage.clickNextButton();
    // Wait for navigation to sample type page
    orderEntityPage.waitForSampleTypePage();
  });

  it("Select sample type", function () {
    // Page should already be on sample type, but verify to be safe
    orderEntityPage.verifySampleTypePage();
    cy.fixture("Order").then((order) => {
      order.samples.forEach((sample) => {
        orderEntityPage.selectSampleTypeOption(sample.sampleType);
        orderEntityPage.checkPanelCheckBoxField();
        orderEntityPage.collectionDate(sample.collectionDate);
      });
    });
    // Test WITH referral enabled
    orderEntityPage.referTest();
    orderEntityPage.selectReferralReason();
    orderEntityPage.selectInstitute();
    orderEntityPage.clickNextButton();
    // Wait for navigation to order details page
    orderEntityPage.waitForOrderDetailsPage();
  });

  it("Generate Lab Order Number, Request and Received Dates", function () {
    // Page should already be on order details, but verify to be safe
    orderEntityPage.verifyOrderDetailsPage();
    cy.fixture("Order").then((order) => {
      order.samples.forEach((sample) => {
        orderEntityPage.requestDate(sample.receivedDate);
        orderEntityPage.receivedDate(sample.receivedDate);
      });
    });
    orderEntityPage.generateLabOrderNumber();
  });

  it("Select site name", function () {
    cy.fixture("Order").then((order) => {
      orderEntityPage.enterSiteName(order.siteName);
    });
  });

  it("Enter requester first and last names", function () {
    cy.fixture("Order").then((order) => {
      orderEntityPage.enterRequesterLastAndFirstName(
        order.requester.fullName,
        order.requester.firstName,
        order.requester.lastName,
      );
    });
    orderEntityPage.rememberSiteAndRequester();
  });

  it("should click submit order button", function () {
    orderEntityPage.clickSubmitOrderButton();
    orderEntityPage.verifySuccessPage();
  });
});
