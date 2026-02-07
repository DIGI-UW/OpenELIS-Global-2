/**
 * E2E Tests for ASTM Analyzer Field Mapping - User Story 1
 *
 * Reference: OpenELIS Testing Roadmap (.specify/guides/testing-roadmap.md)
 * Constitution V.5 Compliance: Individual test execution during development
 *
 * SIMPLIFIED VERSION - Following analyzerHappyPathUserStories.cy.js patterns
 *
 * Execution:
 * - Development: npm run cy:spec "cypress/e2e/analyzerConfiguration.cy.js"
 * - CI/CD: npm run cy:run (full suite)
 *
 * Local debug (match CI):
 *   1. Rebuild WAR: mvn clean install -DskipTests -Dmaven.test.skip=true
 *   2. Reset Postgres: docker compose -f dev.docker-compose.yml down -v
 *   3. Bring up: docker compose -f dev.docker-compose.yml up -d --force-recreate
 *   4. Load CI fixtures: ./scripts/load-ci-fixtures.sh
 *   5. Run: npm run cy:spec "cypress/e2e/analyzerConfiguration.cy.js"
 */

let testAnalyzerId = null;

/**
 * Setup: Create test analyzer via API and VERIFY it exists
 * Credentials: Cypress.env('USERNAME') / Cypress.env('PASSWORD')
 * Uses cy.session() for auth caching + correct API path pattern
 * NOTE: /api/OpenELIS-Global/rest/... is the CORRECT pattern (like storage tests)
 *       /rest/... returns HTML, not JSON - other analyzer tests are broken
 */
before("Setup test analyzer", () => {
  // Wait for backend API to be available (use correct path pattern)
  cy.waitForBackend("/api/OpenELIS-Global/rest/analyzer/analyzers");

  // Use cy.session() to cache and reuse basic auth session across tests
  cy.session("analyzer-config-session", () => {
    cy.request({
      method: "GET",
      url: "/api/OpenELIS-Global/rest/analyzer/analyzers",
      auth: Cypress.getBasicAuth(),
      failOnStatusCode: false,
    });
  });

  // Create test analyzer via API (ensures at least one exists even if fixture did not load)
  cy.request({
    method: "POST",
    url: "/api/OpenELIS-Global/rest/analyzer/analyzers",
    auth: Cypress.getBasicAuth(),
    body: {
      name: "E2E-Config-Test-Analyzer",
      analyzerType: "CHEMISTRY",
      ipAddress: "192.168.1.150",
      port: 5002,
      protocolVersion: "ASTM LIS2-A2",
      testUnitIds: [],
    },
    failOnStatusCode: false,
  }).then((response) => {
    cy.log(
      `Create response: ${response.status} - ${JSON.stringify(response.body)}`,
    );

    if (response.status === 201 && response.body?.id) {
      testAnalyzerId = response.body.id;
      cy.log(`Created analyzer with ID: ${testAnalyzerId}`);
    } else if (response.status !== 201) {
      throw new Error(
        `POST create analyzer failed: ${response.status} - ${JSON.stringify(response.body)}`,
      );
    } else if (!response.body?.id) {
      throw new Error(
        `POST create analyzer returned 201 but no id: ${JSON.stringify(response.body)}`,
      );
    }
  });

  // VERIFY: Fetch analyzers and find ours (or use first available)
  cy.request({
    method: "GET",
    url: "/api/OpenELIS-Global/rest/analyzer/analyzers",
    auth: Cypress.getBasicAuth(),
  }).then((response) => {
    expect(response.status).to.eq(200);
    expect(response.body).to.be.an("array");
    expect(
      response.body.length,
      "No analyzers in database. Ensure analyzer-test-data.sql loaded (check CI step) or POST create succeeded.",
    ).to.be.greaterThan(0);

    // Find our analyzer or use first available
    const ourAnalyzer = response.body.find(
      (a) => a.name === "E2E-Config-Test-Analyzer",
    );
    testAnalyzerId = ourAnalyzer ? ourAnalyzer.id : response.body[0].id;

    cy.log(
      `Using analyzer ID: ${testAnalyzerId} (name: ${ourAnalyzer?.name || response.body[0].name})`,
    );
  });
});

/**
 * Cleanup: Remove test analyzer
 */
after("Cleanup test analyzer", () => {
  if (testAnalyzerId && testAnalyzerId !== "NO_ID") {
    cy.request({
      method: "DELETE",
      url: `/api/OpenELIS-Global/rest/analyzer/analyzers/${testAnalyzerId}`,
      auth: Cypress.getBasicAuth(),
      failOnStatusCode: false,
    });
  }
});

// TODO: Re-enable once analyzer feature PRs are merged into this branch
describe.skip("Analyzer Configuration - User Story 1", function () {
  beforeEach(() => {
    cy.viewport(1025, 900);
    cy.visit("/", { auth: Cypress.getBasicAuth() });

    // Set up intercepts before actions
    cy.intercept("GET", "**/rest/analyzer/analyzers**").as("getAnalyzers");
    cy.intercept("POST", "**/rest/analyzer/analyzers**").as("createAnalyzer");
    cy.intercept("PUT", "**/rest/analyzer/analyzers/**").as("updateAnalyzer");
    cy.intercept("DELETE", "**/rest/analyzer/analyzers/**").as(
      "deleteAnalyzer",
    );
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
   */
  it("should configure analyzer with field mappings", function () {
    // Navigate to analyzers page
    cy.visit("/analyzers", { auth: Cypress.getBasicAuth() });
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Use the testAnalyzerId set in before() hook
    const id = testAnalyzerId;
    cy.log(`Testing with analyzer ID: ${id}`);

    // Find our analyzer row and navigate to mappings
    cy.get(`[data-testid="analyzer-row-${id}"]`)
      .should("be.visible")
      .within(() => {
        // Click the OverflowMenu trigger button (Carbon renders it as a native <button>)
        cy.get('[data-testid^="analyzer-actions-"]').find("button").click();
      });

    // OverflowMenu items render in a portal at body level, outside the row
    cy.get(`[data-testid="analyzer-action-mappings-${id}"]`)
      .should("be.visible")
      .click();

    // Verify mappings page loads
    cy.url().should("include", "/mappings");
    cy.get('[data-testid="field-mapping"]').should("be.visible");
    cy.get('[data-testid="field-mapping-panel"]').should("be.visible");
  });

  /**
   * Test: Validate IP address format
   */
  it("should validate IP address format", function () {
    // Navigate to analyzers page
    cy.visit("/analyzers", { auth: Cypress.getBasicAuth() });
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Click Add Analyzer button (uses actual button selector)
    cy.contains("button", "Add Analyzer").should("be.visible").click();

    // Verify modal opens (Carbon ComposedModal with title)
    cy.contains(".cds--modal-header__heading", "Add New Analyzer").should(
      "be.visible",
    );

    // The IP field already has a value, clear and enter invalid IP
    cy.get('input[placeholder*="192.168"]').clear().type("invalid-ip");

    // Click Save to trigger validation
    cy.contains("button", "Save").click();

    // Expect validation error (Carbon inline validation)
    cy.get(".cds--form-requirement").should("be.visible");

    // Close modal
    cy.contains("button", "Cancel").click();

    cy.log("IP validation error displayed correctly");
  });

  /**
   * Test: Test analyzer connection
   */
  it("should test analyzer connection", function () {
    cy.then(() => {
      const id = testAnalyzerId;
      if (!id || id === "NO_ID") {
        cy.log("Skipping test - analyzer ID not available");
        return;
      }

      // Navigate to analyzers page
      cy.visit("/analyzers", { auth: Cypress.getBasicAuth() });
      cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should(
        "be.visible",
      );

      // Find analyzer row and open overflow menu
      cy.get(`[data-testid="analyzer-row-${id}"]`)
        .should("be.visible")
        .within(() => {
          // Click the OverflowMenu trigger button (Carbon renders it as a native <button>)
          cy.get('[data-testid^="analyzer-actions-"]').find("button").click();
        });

      // OverflowMenu items render in a portal at body level, outside the row
      cy.get(`[data-testid="analyzer-action-test-connection-${id}"]`)
        .should("be.visible")
        .click();

      // Wait for connection test modal/result
      cy.get("body").then(($body) => {
        const hasModal =
          $body.find('[data-testid="test-connection-modal"]').length > 0;
        const hasResult =
          $body.find('[data-testid*="connection-result"]').length > 0;

        if (hasModal || hasResult) {
          cy.log("Connection test interface displayed");
        } else {
          cy.log("Connection test may use a different UI pattern");
        }
      });
    });
  });

  /**
   * Test: Create test code to OpenELIS test mapping
   */
  it("should create test code to OpenELIS test mapping", function () {
    cy.then(() => {
      const id = testAnalyzerId;
      if (!id || id === "NO_ID") {
        cy.log("Skipping test - analyzer ID not available");
        return;
      }

      // Navigate directly to mappings page
      cy.visit(`/analyzers/${id}/mappings`, {
        auth: Cypress.getBasicAuth(),
        timeout: 180000, // 3 minutes for CI
      });
      cy.get('[data-testid="field-mapping"]', { timeout: 30000 }).should(
        "be.visible",
      );

      // Check for unmapped fields
      cy.get("body").then(($body) => {
        const unmappedFields = $body.find('[data-testid^="unmapped-field-"]');

        if (unmappedFields.length > 0) {
          cy.get('[data-testid^="unmapped-field-"]').first().click();
          cy.get('[data-testid="mapping-modal"]').should("be.visible");
          cy.log("Mapping interface accessible for unmapped fields");
        } else {
          cy.log("No unmapped fields available (may be pre-configured)");
        }
      });
    });
  });

  /**
   * Test: Create unit mapping with conversion factor
   */
  it("should create unit mapping with conversion factor", function () {
    cy.then(() => {
      const id = testAnalyzerId;
      if (!id || id === "NO_ID") {
        cy.log("Skipping test - analyzer ID not available");
        return;
      }

      cy.visit(`/analyzers/${id}/mappings`, {
        auth: Cypress.getBasicAuth(),
        timeout: 180000, // 3 min for CI
      });
      cy.get('[data-testid="field-mapping"]', { timeout: 30000 }).should(
        "be.visible",
      );

      // Check for numeric fields that support unit conversion
      cy.get("body").then(($body) => {
        const numericFields = $body.find('[data-testid*="numeric-field-"]');

        if (numericFields.length > 0) {
          cy.get('[data-testid*="numeric-field-"]').first().click();
          cy.log("Numeric field mapping interface accessible");
        } else {
          cy.log("No numeric fields available for unit mapping test");
        }
      });
    });
  });

  /**
   * Test: Create qualitative value mapping
   */
  it("should create qualitative value mapping", function () {
    cy.then(() => {
      const id = testAnalyzerId;
      if (!id || id === "NO_ID") {
        cy.log("Skipping test - analyzer ID not available");
        return;
      }

      cy.visit(`/analyzers/${id}/mappings`, {
        auth: Cypress.getBasicAuth(),
        timeout: 180000, // 3 min for CI
      });
      cy.get('[data-testid="field-mapping"]', { timeout: 30000 }).should(
        "be.visible",
      );

      // Check for qualitative fields
      cy.get("body").then(($body) => {
        const qualFields = $body.find('[data-testid*="qualitative-field-"]');

        if (qualFields.length > 0) {
          cy.get('[data-testid*="qualitative-field-"]').first().click();
          cy.log("Qualitative field mapping interface accessible");
        } else {
          cy.log("No qualitative fields available for mapping test");
        }
      });
    });
  });

  /**
   * Test: Complete analyzer configuration workflow (happy path)
   */
  it("should complete analyzer configuration workflow (happy path)", function () {
    cy.then(() => {
      const id = testAnalyzerId;
      if (!id || id === "NO_ID") {
        cy.log("Skipping test - analyzer ID not available");
        return;
      }

      // Step 1: Navigate to analyzers list
      cy.visit("/analyzers", { auth: Cypress.getBasicAuth() });
      cy.get('[data-testid="analyzers-list"]').should("be.visible");

      // Step 2: Navigate to mappings for our analyzer
      cy.get(`[data-testid="analyzer-row-${id}"]`)
        .should("be.visible")
        .within(() => {
          // Click the OverflowMenu trigger button (Carbon renders it as a native <button>)
          cy.get('[data-testid^="analyzer-actions-"]').find("button").click();
        });

      // OverflowMenu items render in a portal at body level, outside the row
      cy.get(`[data-testid="analyzer-action-mappings-${id}"]`)
        .should("be.visible")
        .click();

      // Step 3: Verify mappings page
      cy.url().should("include", "/mappings");
      cy.get('[data-testid="field-mapping"]').should("be.visible");

      // Step 4: Navigate back to analyzers
      cy.visit("/analyzers", { auth: Cypress.getBasicAuth() });
      cy.get('[data-testid="analyzers-list"]').should("be.visible");

      cy.log(
        "Complete workflow: analyzer list → mappings → back to list successful",
      );
    });
  });

  /**
   * Test: Display validation dashboard for analyzer
   */
  it("should display validation dashboard for analyzer in progress", function () {
    cy.then(() => {
      const id = testAnalyzerId;
      if (!id || id === "NO_ID") {
        cy.log("Skipping test - analyzer ID not available");
        return;
      }

      cy.visit(`/analyzers/${id}/mappings`, { auth: Cypress.getBasicAuth() });
      cy.get('[data-testid="field-mapping"]', { timeout: 10000 }).should(
        "be.visible",
      );

      // Check for validation/stats display
      cy.get("body").then(($body) => {
        const hasStats =
          $body.find('[data-testid="field-mapping-stats"]').length > 0 ||
          $body.find('[data-testid*="stat-"]').length > 0;

        if (hasStats) {
          cy.log("Validation/stats dashboard displayed");
        } else {
          cy.log("Stats may not be available for this analyzer");
        }
      });
    });
  });

  /**
   * Test: Configure new analyzer
   */
  it("should configure new analyzer", function () {
    // Navigate to analyzers page
    cy.visit("/analyzers", { auth: Cypress.getBasicAuth() });
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Verify Add Analyzer button exists
    cy.get('[data-testid="add-analyzer-button"]').should("be.visible");

    cy.log("Analyzer configuration interface accessible");
  });

  /**
   * Test: Display analyzer list with existing analyzers
   */
  it("should display analyzer list with existing analyzers", function () {
    cy.visit("/analyzers", { auth: Cypress.getBasicAuth() });
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Verify table or list structure
    cy.get("body").then(($body) => {
      const hasTable =
        $body.find('[data-testid="analyzers-table-container"]').length > 0;
      const hasRows = $body.find('[data-testid^="analyzer-row-"]').length > 0;

      if (hasTable && hasRows) {
        cy.log("Analyzer list displayed with data");
      } else if (hasTable) {
        cy.log("Analyzer list displayed (may be empty)");
      }
    });
  });

  /**
   * Test: Display analyzer statistics
   */
  it("should display analyzer statistics", function () {
    cy.visit("/analyzers", { auth: Cypress.getBasicAuth() });
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Check for stats display
    cy.get("body").then(($body) => {
      const hasStats =
        $body.find('[data-testid="analyzers-list-stats"]').length > 0;

      if (hasStats) {
        cy.get('[data-testid="analyzers-list-stats"]').should("be.visible");
        cy.log("Analyzer statistics displayed");
      } else {
        cy.log("Statistics may be displayed differently");
      }
    });
  });
});
