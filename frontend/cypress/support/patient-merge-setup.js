/**
 * Patient Merge E2E Test Setup Commands
 *
 * Follows the same pattern as storage-setup.js for consistency.
 * Provides Cypress commands for loading and cleaning patient merge test data.
 */
import LoginPage from "../pages/LoginPage";

/**
 * Cypress command to load patient merge test fixtures
 * Usage: cy.loadPatientMergeFixtures()
 */
Cypress.Commands.add("loadPatientMergeFixtures", () => {
  cy.task("loadPatientMergeTestData", null, { log: false });
});

/**
 * Cypress command to clean patient merge test fixtures
 * Usage: cy.cleanPatientMergeFixtures()
 */
Cypress.Commands.add("cleanPatientMergeFixtures", () => {
  cy.task("cleanPatientMergeTestData", null, { log: false });
});

/**
 * Cypress command to check if patient merge test fixtures exist
 * Usage: cy.checkPatientMergeFixturesExist()
 */
Cypress.Commands.add("checkPatientMergeFixturesExist", () => {
  return cy.task("checkPatientMergeFixturesExist", null, { log: false });
});

/**
 * Common setup for patient merge E2E tests
 * Usage: cy.setupPatientMergeTests()
 *
 * Environment variables (set via CYPRESS_* prefix):
 * - SKIP_FIXTURES=true: Skip fixture loading entirely (assumes fixtures exist)
 * - FORCE_FIXTURES=true: Force reload fixtures even if they exist
 * - CLEANUP_FIXTURES=true: Clean up fixtures after tests (default: false)
 *
 * This command:
 * 1. Waits for backend API to be available
 * 2. Logs in as admin user
 * 3. Loads test fixtures if needed
 */
Cypress.Commands.add("setupPatientMergeTests", () => {
  // Wait for backend API to be available
  cy.waitForBackend("/rest/patient/merge/details/1");

  // Login as admin (required for patient merge)
  const loginPage = new LoginPage();
  loginPage.visit();
  const homePage = loginPage.goToHomePage();

  // Smart fixture loading based on env vars and existence check
  const skipFixtures = Cypress.env("SKIP_FIXTURES") === true;
  const forceFixtures = Cypress.env("FORCE_FIXTURES") === true;

  if (skipFixtures) {
    cy.log(
      "Skipping fixture loading (SKIP_FIXTURES=true) - assuming fixtures exist",
    );
  } else if (forceFixtures) {
    cy.log(
      "Force loading fixtures (FORCE_FIXTURES=true) - reloading even if exist",
    );
    cy.loadPatientMergeFixtures();
  } else {
    // Check if fixtures already exist before loading
    cy.checkPatientMergeFixturesExist().then((fixturesExist) => {
      if (fixturesExist) {
        cy.log("Fixtures already exist - skipping load for faster iteration");
      } else {
        cy.log("Fixtures not found - loading test data");
        cy.loadPatientMergeFixtures();
      }
    });
  }

  // Return homePage for tests that need it
  return cy.wrap(homePage);
});

/**
 * Cleanup after patient merge tests
 * Usage: cy.cleanupPatientMergeTests()
 *
 * Only cleans up if CLEANUP_FIXTURES=true (default: false for faster iteration)
 */
Cypress.Commands.add("cleanupPatientMergeTests", () => {
  const shouldCleanup = Cypress.env("CLEANUP_FIXTURES") === true;

  if (shouldCleanup) {
    cy.log("Cleaning up fixtures (CLEANUP_FIXTURES=true)");
    cy.cleanPatientMergeFixtures();
  } else {
    cy.log(
      "Skipping fixture cleanup (CLEANUP_FIXTURES=false) - fixtures preserved for next run",
    );
  }
});

/**
 * Set up API intercepts for patient merge tests
 * Usage: cy.setupPatientMergeIntercepts()
 */
Cypress.Commands.add("setupPatientMergeIntercepts", () => {
  cy.intercept("GET", "**/rest/patient/merge/details/**").as(
    "getPatientDetails",
  );
  cy.intercept("POST", "**/rest/patient/merge/validate").as("validateMerge");
  cy.intercept("POST", "**/rest/patient/merge/execute").as("executeMerge");
  cy.intercept("GET", "**/rest/patient-search-results**").as("patientSearch");
});
