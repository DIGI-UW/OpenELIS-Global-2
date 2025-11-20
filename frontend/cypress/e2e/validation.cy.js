/**
 * E2E Tests for Validation (By Routine, By Order, By Range Of Order)
 * Tests validation workflow for different search types
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Session management via cy.login() with cy.session() (10-20x faster)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/validation.cy.js"
 */

import LoginPage from "../pages/LoginPage";
import HomePage from "../pages/HomePage";
import PatientEntryPage from "../pages/PatientEntryPage";
import Validation from "../pages/Validation";

let homePage = null;
let validation = null;
let patientPage = new PatientEntryPage();

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
// Same pattern as cy.setupStorageTests() in storage-setup.js
before("login", () => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  const loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
});

const navigateToValidationPage = (validationType) => {
  validation = homePage[`goToValidationBy${validationType}`]();
  // Verify we're on a validation page (URLs vary: /Validation, /AccessionValidation, /AccessionValidationRange)
  cy.url().should("satisfy", (url) => {
    return url.includes("Validation") || url.includes("AccessionValidation");
  });
};

describe("Validation By Routine", function () {
  before("navigate to Validation Page", function () {
    navigateToValidationPage("Routine");
  });

  beforeEach(() => {
    // Set up intercepts BEFORE actions (Constitution V.5)
    cy.intercept("GET", "**/rest/AccessionValidation?*").as(
      "getValidationResults",
    );
  });

  it("User visits Validation Page", function () {
    validation.checkForHeading();
  });

  it("Should Select Test Unit From Drop-Down And Validate", function () {
    cy.fixture("workplan").then((order) => {
      // Set up intercept BEFORE action
      cy.intercept("GET", "**/rest/AccessionValidation?*").as(
        "getValidationResults",
      );

      // Wait for dropdown to be ready (validation page uses #unitType, not #select-1)
      cy.get("#unitType", { timeout: 10000 })
        .should("be.visible")
        .should("not.be.disabled");

      validation.selectTestUnit(order.unitType);

      // Wait for API call instead of arbitrary wait
      cy.wait("@getValidationResults", { timeout: 15000 })
        .its("response.statusCode")
        .should("eq", 200);
    });
  });
});

describe("Validation By Order", function () {
  before("navigate to Validation Page", function () {
    navigateToValidationPage("Order");
  });

  beforeEach(() => {
    // Set up intercepts BEFORE actions (Constitution V.5)
    cy.intercept("GET", "**/rest/AccessionValidation?*").as(
      "getValidationResults",
    );
  });

  it("User visits Validation Page", function () {
    validation.checkForHeading();
  });

  it("Enter Lab Number, search and validate", function () {
    cy.fixture("Patient").then((order) => {
      // Set up intercept BEFORE action
      cy.intercept("GET", "**/rest/AccessionValidation?*").as(
        "getValidationResults",
      );

      validation.enterLabNumberAndSearch(order.labNo);

      // Wait for API call instead of arbitrary wait
      cy.wait("@getValidationResults", { timeout: 15000 })
        .its("response.statusCode")
        .should("eq", 200);
    });
  });
});

describe("Validation By Range Of Order", function () {
  before("navigate to Validation Page", function () {
    navigateToValidationPage("RangeOrder");
  });

  beforeEach(() => {
    // Set up intercepts BEFORE actions (Constitution V.5)
    // Validation uses /rest/AccessionValidation endpoint, not LogbookResults
    cy.intercept("GET", "**/rest/AccessionValidation?*").as(
      "getValidationResults",
    );
    cy.intercept("POST", "**/rest/AccessionValidation**").as("saveResults");
  });

  it("User visits Validation Page", function () {
    validation.checkForHeading();
  });

  it("Should Enter Lab Number and perform a search", function () {
    cy.fixture("Patient").then((order) => {
      // Set up intercept BEFORE action
      cy.intercept("GET", "**/rest/AccessionValidation?*").as(
        "getValidationResults",
      );

      validation.enterLabNumberAndSearch(order.labNo);

      // Wait for API call instead of arbitrary wait
      cy.wait("@getValidationResults", { timeout: 15000 })
        .its("response.statusCode")
        .should("eq", 200);
    });
  });

  it("Should Save the results", function () {
    // Set up intercept BEFORE action
    cy.intercept("POST", "**/rest/AccessionValidation**").as("saveResults");

    validation.saveResults("Test Note");

    // Wait for API call instead of arbitrary wait
    cy.wait("@saveResults", { timeout: 15000 })
      .its("response.statusCode")
      .should("be.oneOf", [200, 201]);
  });
});
