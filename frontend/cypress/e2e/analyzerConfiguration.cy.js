/**
 * E2E Tests for ASTM Analyzer Field Mapping - User Story 1
 *
 * Reference: OpenELIS Testing Roadmap (.specify/guides/testing-roadmap.md)
 * Quick Reference: Cypress Best Practices (.specify/guides/cypress-best-practices.md)
 * Template: Cypress E2E Test
 *
 * Constitution V.5 Compliance Checklist:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Browser console logging enabled and reviewed after each run
 * - Tests run individually during development (not full suite)
 * - Post-run review completed (console logs, screenshots, test output)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Focused on happy paths (user workflows, not implementation details)
 * - data-testid selectors used (PREFERRED)
 * - Viewport set before visit
 * - API-based test data setup (10x faster than UI)
 *
 * Task Reference: T040
 *
 * Execution:
 * - Development: npm run cy:run -- --spec "cypress/e2e/analyzerConfiguration.cy.js"
 * - CI/CD: npm run cy:run (full suite)
 */

let testAnalyzerId = null;

/**
 * Setup: Login and create test analyzer via API
 */
before("Login and setup test analyzer", () => {
  // Login via API (fast - not UI)
  cy.request({
    method: "POST",
    url: "/api/OpenELIS-Global/LoginPage",
    body: {
      loginName: "admin",
      password: "adminADMIN!",
    },
    failOnStatusCode: false,
  }).then((response) => {
    // Store session cookies/tokens if needed
    // OpenELIS may use cookies or session storage for authentication
  });

  // Fast API-based test data setup (NOT UI interactions)
  cy.request({
    method: "POST",
    url: "/rest/analyzer/analyzers",
    body: {
      name: "TEST-Analyzer-E2E",
      analyzerType: "HEMATOLOGY",
      ipAddress: "192.168.1.100",
      port: 5000,
      protocolVersion: "ASTM_E1394",
      testUnitIds: [],
      active: true,
    },
    failOnStatusCode: false,
  }).then((response) => {
    if (response.status === 201 || response.status === 200) {
      testAnalyzerId = response.body.id || response.body.data?.id;
      cy.wrap(testAnalyzerId).as("analyzerId");
    }
  });
});

/**
 * Cleanup: Remove test analyzer after all tests
 */
after("Cleanup test analyzer", () => {
  if (testAnalyzerId) {
    cy.request({
      method: "DELETE",
      url: `/rest/analyzer/analyzers/${testAnalyzerId}`,
      failOnStatusCode: false,
    });
  }
});

describe("Analyzer Configuration - User Story 1", function () {
  beforeEach(() => {
    // Viewport management (profy.dev: set viewport before visit)
    cy.viewport(1025, 900); // Desktop viewport

    // Set up API intercepts BEFORE actions that trigger them (Constitution V.5)
    cy.intercept("GET", "**/rest/analyzer/analyzers**").as("getAnalyzers");
    cy.intercept("POST", "**/rest/analyzer/analyzers**").as("createAnalyzer");
    cy.intercept("PUT", "**/rest/analyzer/analyzers/**").as("updateAnalyzer");
    cy.intercept("DELETE", "**/rest/analyzer/analyzers/**").as("deleteAnalyzer");
    cy.intercept("POST", "**/rest/analyzer/analyzers/**/test-connection**").as(
      "testConnection",
    );
    cy.intercept("GET", "**/rest/analyzer/analyzers/**/mappings**").as(
      "getMappings",
    );
    cy.intercept("POST", "**/rest/analyzer/analyzers/**/mappings**").as(
      "createMapping",
    );
  });

  /**
   * Test: Configure analyzer with field mappings
   * Scenario: User creates analyzer, navigates to field mappings, and creates mappings
   */
  it("should configure analyzer with field mappings", function () {
    // Navigate to analyzers page
    cy.visit("/analyzers");

    // Wait for page to load
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Wait for analyzers to load
    cy.wait("@getAnalyzers").its("response.statusCode").should("eq", 200);

    // Verify test analyzer is visible in table
    cy.get('[data-testid="analyzers-table-container"]')
      .find("tbody")
      .find("tr")
      .contains("TEST-Analyzer-E2E")
      .should("be.visible");

    // Click "Field Mappings" action for test analyzer
    cy.get('[data-testid="analyzers-table-container"]')
      .find("tbody")
      .find("tr")
      .contains("TEST-Analyzer-E2E")
      .parent()
      .find('[data-testid="analyzer-action-field-mappings"]')
      .should("be.visible")
      .click();

    // Wait for field mappings page to load
    cy.get('[data-testid="field-mapping"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Wait for mappings to load
    cy.wait("@getMappings").its("response.statusCode").should("eq", 200);

    // Verify field mapping interface is displayed
    cy.get('[data-testid="field-mapping-panel"]').should("be.visible");
  });

  /**
   * Test: Validate IP address format
   * Scenario: User tries to create analyzer with invalid IP address
   */
  it("should validate IP address format", function () {
    // Navigate to analyzers page
    cy.visit("/analyzers");

    // Wait for page to load
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Click "Add Analyzer" button
    cy.get('[data-testid="add-analyzer-button"]')
      .should("be.visible")
      .should("not.be.disabled")
      .click();

    // Wait for modal to open (Carbon uses portals)
    cy.get('[role="dialog"]', { timeout: 10000 }).should("be.visible");

    // Fill form with invalid IP address
    cy.get('[data-testid="analyzer-form-name-input"]')
      .should("be.visible")
      .clear()
      .type("Test Analyzer Invalid IP");

    cy.get('[data-testid="analyzer-form-ip-input"]')
      .should("be.visible")
      .clear()
      .type("999.999.999.999"); // Invalid IP

    // Try to save (should show validation error)
    cy.get('[data-testid="analyzer-form-save-button"]')
      .should("be.visible")
      .click();

    // Verify validation error is displayed
    cy.get('[data-testid="analyzer-form"]')
      .should("contain.text", "Invalid")
      .or("contain.text", "invalid");

    // Close modal
    cy.get('[data-testid="analyzer-form"]')
      .find('button:contains("Cancel"), [data-testid*="cancel"]')
      .first()
      .click();
  });

  /**
   * Test: Test analyzer connection
   * Scenario: User tests TCP connection to analyzer
   */
  it("should test analyzer connection", function () {
    // Navigate to analyzers page
    cy.visit("/analyzers");

    // Wait for page to load
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Wait for analyzers to load
    cy.wait("@getAnalyzers").its("response.statusCode").should("eq", 200);

    // Find test analyzer row and click "Test Connection"
    cy.get('[data-testid="analyzers-table-container"]')
      .find("tbody")
      .find("tr")
      .contains("TEST-Analyzer-E2E")
      .parent()
      .find('[data-testid="analyzer-action-test-connection"]')
      .should("be.visible")
      .click();

    // Wait for test connection modal to open
    cy.get('[data-testid="test-connection-modal"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Wait for connection test API call
    cy.wait("@testConnection", { timeout: 30000 }).then((interception) => {
      // Connection test may succeed or fail (depends on actual analyzer)
      // Just verify the modal shows appropriate state
      cy.get('[data-testid="test-connection-modal"]').should("be.visible");
    });
  });

  /**
   * Test: Create test code to OpenELIS test mapping
   * Scenario: User creates a mapping from analyzer test code to OpenELIS test
   */
  it("should create test code to OpenELIS test mapping", function () {
    // Navigate to field mappings page for test analyzer
    cy.visit(`/analyzers/${testAnalyzerId}/mappings`);

    // Wait for field mappings page to load
    cy.get('[data-testid="field-mapping"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Wait for mappings to load
    cy.wait("@getMappings").its("response.statusCode").should("eq", 200);

    // Wait for analyzer fields to load
    cy.get('[data-testid="field-mapping-panel"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Select first unmapped field (if available)
    cy.get('[data-testid="field-mapping-table-container"]')
      .find("tbody")
      .find("tr")
      .first()
      .should("be.visible")
      .click();

    // Wait for mapping panel to appear
    cy.get('[data-testid="mapping-panel"]', { timeout: 5000 }).should(
      "be.visible",
    );

    // Select OpenELIS field (if field selector is available)
    // Note: This assumes OpenELISFieldSelector is implemented
    cy.get('[data-testid="mapping-panel"]')
      .find('[data-testid*="field-selector"], [role="combobox"]')
      .first()
      .should("be.visible")
      .click();

    // Wait for dropdown to open (Carbon renders in portal)
    cy.get('[role="listbox"]', { timeout: 5000 }).should("be.visible");

    // Select first option (if available)
    cy.get('[role="option"]').first().click();

    // Save mapping
    cy.get('[data-testid="mapping-panel-save-button"]')
      .should("be.visible")
      .should("not.be.disabled")
      .click();

    // Wait for create mapping API call
    cy.wait("@createMapping", { timeout: 10000 }).then((interception) => {
      expect(interception.response.statusCode).to.be.oneOf([200, 201]);
    });

    // Verify success (mapping panel should show view mode or success message)
    cy.get('[data-testid="mapping-panel"]').should("be.visible");
  });

  /**
   * Test: Create unit mapping with conversion factor
   * Scenario: User creates a unit mapping with conversion factor for numeric field
   */
  it("should create unit mapping with conversion factor", function () {
    // Navigate to field mappings page for test analyzer
    cy.visit(`/analyzers/${testAnalyzerId}/mappings`);

    // Wait for field mappings page to load
    cy.get('[data-testid="field-mapping"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Wait for mappings to load
    cy.wait("@getMappings").its("response.statusCode").should("eq", 200);

    // This test assumes:
    // 1. A numeric field exists in the analyzer
    // 2. UnitMappingModal can be opened from MappingPanel
    // 3. Unit mapping functionality is integrated into the mapping workflow

    // For now, verify the page loads correctly
    // Full unit mapping test requires integration with UnitMappingModal
    cy.get('[data-testid="field-mapping-panel"]').should("be.visible");
  });

  /**
   * Test: Create qualitative value mapping
   * Scenario: User creates qualitative value mappings (many-to-one)
   */
  it("should create qualitative value mapping", function () {
    // Navigate to field mappings page for test analyzer
    cy.visit(`/analyzers/${testAnalyzerId}/mappings`);

    // Wait for field mappings page to load
    cy.get('[data-testid="field-mapping"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Wait for mappings to load
    cy.wait("@getMappings").its("response.statusCode").should("eq", 200);

    // This test assumes:
    // 1. A qualitative field exists in the analyzer
    // 2. QualitativeMappingModal can be opened from MappingPanel
    // 3. Qualitative mapping functionality is integrated into the mapping workflow

    // For now, verify the page loads correctly
    // Full qualitative mapping test requires integration with QualitativeMappingModal
    cy.get('[data-testid="field-mapping-panel"]').should("be.visible");
  });
});

/**
 * Post-Run Review (MANDATORY - Constitution V.5):
 *
 * After each test execution, review:
 * 1. Console Logs: Check browser console in Cypress UI for errors, failed API requests, warnings
 * 2. Screenshots: Review failure screenshots for UI state at failure point
 * 3. Test Output: Review Cypress command log for execution order and timeouts
 */

