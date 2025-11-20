/**
 * E2E Tests for Lab Number Management
 * Tests lab number type selection and configuration
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Session management via cy.login() with cy.session() (10-20x faster)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/labNumberManagement.cy.js"
 */

import LoginPage from "../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let labNumMgtPage = null;

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
// Same pattern as cy.setupStorageTests() in storage-setup.js
before(() => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPageProgram();
  
  // Verify we're on the admin page
  cy.url().should("include", "/Admin");
});

beforeEach(() => {
  // Load fixture data for each test
  cy.fixture("LabNumberManagement").as("labNMData");
  
  // Set up intercepts BEFORE actions (Constitution V.5)
  cy.intercept("POST", "**/rest/lab-number-management**").as("saveLabNumberConfig");
});

describe("Lab Number Management", function () {
  it("User navigates to the Lab Number Management page", function () {
    labNumMgtPage = adminPage.goToLabNumberManagementPage();
    
    // Verify we're on the lab number management page
    cy.url().should("include", "/LabNumberManagement");
  });

  it("Validate Page Visibility", function () {
    labNumMgtPage.verifyPageLoaded();
  });

  it("User selects legacy lab number type and submits", function () {
    cy.get("@labNMData").then((labNumberManagementData) => {
      // Wait for dropdown to be ready
      cy.get("select", { timeout: 10000 })
        .should("be.visible");
      
      labNumMgtPage.selectLabNumber(
        labNumberManagementData.legacyLabNumberType,
      );
      
      // Set up intercept BEFORE action
      cy.intercept("POST", "**/rest/lab-number-management**").as("saveLabNumberConfig");
      
      // Verify submit button is ready before clicking
      cy.get("button", { timeout: 10000 })
        .contains("Submit")
        .should("be.visible")
        .should("not.be.disabled");
      
      labNumMgtPage.clickSubmitButton();
      
      // Wait for API call instead of arbitrary wait
      cy.wait("@saveLabNumberConfig", { timeout: 15000 })
        .its("response.statusCode")
        .should("be.oneOf", [200, 201]);
    });
  });

  it("User selects alpha numeric lab number type and submits", function () {
    cy.get("@labNMData").then((labNumberManagementData) => {
      // Wait for dropdown to be ready
      cy.get("select", { timeout: 10000 })
        .should("be.visible");
      
      labNumMgtPage.selectLabNumber(labNumberManagementData.alphaLabNumberType);
      labNumMgtPage.checkPrefixCheckBox();
      
      // Wait for prefix input to be ready
      cy.get("input", { timeout: 10000 })
        .should("be.visible");
      
      labNumMgtPage.typePrefix(labNumberManagementData.userPrefix);
      
      // Set up intercept BEFORE action
      cy.intercept("POST", "**/rest/lab-number-management**").as("saveLabNumberConfig");
      
      // Verify submit button is ready before clicking
      cy.get("button", { timeout: 10000 })
        .contains("Submit")
        .should("be.visible")
        .should("not.be.disabled");
      
      labNumMgtPage.clickSubmitButton();
      
      // Wait for API call instead of arbitrary wait
      cy.wait("@saveLabNumberConfig", { timeout: 15000 })
        .its("response.statusCode")
        .should("be.oneOf", [200, 201]);
    });
  });

  //Back to default type
  it("Navigate back to legacy lab number type", function () {
    cy.get("@labNMData").then((labNumberManagementData) => {
      // Wait for dropdown to be ready
      cy.get("select", { timeout: 10000 })
        .should("be.visible");
      
      labNumMgtPage.selectLabNumber(
        labNumberManagementData.legacyLabNumberType,
      );
      
      // Set up intercept BEFORE action
      cy.intercept("POST", "**/rest/lab-number-management**").as("saveLabNumberConfig");
      
      // Verify submit button is ready before clicking
      cy.get("button", { timeout: 10000 })
        .contains("Submit")
        .should("be.visible")
        .should("not.be.disabled");
      
      labNumMgtPage.clickSubmitButton();
      
      // Wait for API call instead of arbitrary wait
      cy.wait("@saveLabNumberConfig", { timeout: 15000 })
        .its("response.statusCode")
        .should("be.oneOf", [200, 201]);
    });
  });
});
