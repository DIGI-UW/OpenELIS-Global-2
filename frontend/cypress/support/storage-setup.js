import LoginPage from "../pages/LoginPage";

/**
 * Common setup for all storage E2E tests
 * Consolidates login, fixture loading, and API readiness checks
 * Usage: cy.setupStorageTests()
 */
Cypress.Commands.add("setupStorageTests", () => {
  // Wait for backend API to be available
  cy.waitForBackend("/rest/storage/samples");

  // Login
  const loginPage = new LoginPage();
  loginPage.visit();
  const homePage = loginPage.goToHomePage();

  // Load storage test fixtures
  cy.loadStorageFixtures();

  // Return homePage for tests that need it
  return cy.wrap(homePage);
});

/**
 * Cleanup after storage tests
 * Usage: cy.cleanupStorageTests()
 */
Cypress.Commands.add("cleanupStorageTests", () => {
  if (Cypress.env("CLEANUP_FIXTURES")) {
    cy.cleanStorageFixtures();
  } else {
    cy.log("Skipping fixture cleanup (CLEANUP_FIXTURES=false)");
  }
});

/**
 * Set up common API intercepts for storage tests
 * Usage: cy.setupStorageIntercepts()
 * Note: Using ** wildcard to match any query parameters
 */
Cypress.Commands.add("setupStorageIntercepts", () => {
  // Use more flexible patterns to match query parameters
  cy.intercept("GET", "**/rest/storage/samples**").as("getSamples");
  cy.intercept("GET", "**/rest/storage/rooms**").as("getRooms");
  cy.intercept("GET", "**/rest/storage/devices**").as("getDevices");
  cy.intercept("GET", "**/rest/storage/shelves**").as("getShelves");
  cy.intercept("GET", "**/rest/storage/racks**").as("getRacks");
  cy.intercept("GET", "**/rest/storage/locations/search**").as("searchLocations");
  cy.intercept("POST", "**/rest/storage/rooms**").as("createRoom");
  cy.intercept("POST", "**/rest/storage/devices**").as("createDevice");
  cy.intercept("POST", "**/rest/storage/shelves**").as("createShelf");
  cy.intercept("POST", "**/rest/storage/racks**").as("createRack");
  cy.intercept("POST", "**/rest/storage/samples/move**").as("moveSample");
});

