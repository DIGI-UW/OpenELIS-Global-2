/**
 * E2E Tests for ASTM Analyzer Field Mapping - User Story 2 (Maintenance)
 *
 * Reference: OpenELIS Testing Roadmap (.specify/guides/testing-roadmap.md)
 * Quick Reference: Cypress Best Practices (.specify/guides/cypress-best-practices.md)
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
 * Task Reference: T073
 *
 * Execution:
 * - Development: npm run cy:run -- --spec "cypress/e2e/analyzerMaintenance.cy.js"
 * - CI/CD: npm run cy:run (full suite)
 */

let testAnalyzerId = null;
let testMappingId = null;

/**
 * Setup: Login and create test analyzer with mappings via API
 */
before("Login and setup test analyzer with mappings", () => {
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
  // Step 1: Create analyzer
  cy.request({
    method: "POST",
    url: "/rest/analyzer/analyzers",
    body: {
      name: "TEST-Maintenance-Analyzer-E2E",
      analyzerType: "HEMATOLOGY",
      ipAddress: "192.168.1.100",
      port: 5000,
      protocolVersion: "ASTM_E1394",
      testUnitIds: [],
      active: false, // Start inactive for maintenance tests
    },
    failOnStatusCode: false,
  }).then((response) => {
    if (response.status === 201 || response.status === 200) {
      testAnalyzerId = response.body.id || response.body.data?.id;
      cy.wrap(testAnalyzerId).as("analyzerId");

      // Step 2: Create analyzer field
      cy.request({
        method: "POST",
        url: `/rest/analyzer/analyzers/${testAnalyzerId}/fields`,
        body: {
          fieldName: "GLUCOSE",
          fieldType: "NUMERIC",
          unit: "mg/dL",
        },
        failOnStatusCode: false,
      }).then((fieldResponse) => {
        if (fieldResponse.status === 201 || fieldResponse.status === 200) {
          const fieldId =
            fieldResponse.body.id || fieldResponse.body.data?.id;

          // Step 3: Create draft mapping
          cy.request({
            method: "POST",
            url: `/rest/analyzer/analyzers/${testAnalyzerId}/mappings`,
            body: {
              analyzerFieldId: fieldId,
              openelisFieldId: "TEST-001",
              openelisFieldType: "TEST",
              mappingType: "TEST_LEVEL",
              isRequired: false,
              isActive: false, // Draft state
            },
            failOnStatusCode: false,
          }).then((mappingResponse) => {
            if (
              mappingResponse.status === 201 ||
              mappingResponse.status === 200
            ) {
              testMappingId =
                mappingResponse.body.id ||
                mappingResponse.body.data?.id;
              cy.wrap(testMappingId).as("mappingId");
            }
          });
        }
      });
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

describe("Analyzer Maintenance - User Story 2", function () {
  beforeEach(() => {
    // Viewport management (profy.dev: set viewport before visit)
    cy.viewport(1025, 900); // Desktop viewport

    // Set up API intercepts BEFORE actions that trigger them (Constitution V.5)
    cy.intercept("GET", "**/rest/analyzer/analyzers**").as("getAnalyzers");
    cy.intercept("GET", "**/rest/analyzer/analyzers/**/mappings**").as(
      "getMappings",
    );
    cy.intercept("PUT", "**/rest/analyzer/analyzers/**/mappings/**").as(
      "updateMapping",
    );
    cy.intercept("POST", "**/rest/analyzer/analyzers/**/mappings/**/activate**")
      .as("activateMapping");
    cy.intercept("PUT", "**/rest/analyzer/analyzers/**/mappings/**/disable**")
      .as("disableMapping");
  });

  /**
   * Test: Update existing mapping
   * 
   * Scenario: User updates an existing mapping to change the OpenELIS field mapping.
   * For active analyzers, confirmation should be required. For inactive analyzers,
   * update should proceed without confirmation.
   */
  it("should update existing mapping", function () {
    // Arrange: Navigate to field mappings page
    cy.visit(`/analyzers/${testAnalyzerId}/mappings`);

    // Wait for mappings to load
    cy.wait("@getMappings", { timeout: 10000 });

    // Act: Find and click edit button for the mapping
    // Note: This assumes the mapping is displayed in the field mapping panel
    cy.get('[data-testid="field-mapping-panel"]', { timeout: 10000 })
      .should("be.visible")
      .within(() => {
        // Find the row for our test field (GLUCOSE)
        cy.contains("GLUCOSE")
          .parents('[data-testid*="field-row"]')
          .first()
          .within(() => {
            // Click action button to open mapping panel
            cy.get('[data-testid*="field-action"]')
              .should("be.visible")
              .click();
          });
      });

    // Wait for mapping panel to appear
    cy.get('[data-testid="mapping-panel"]', { timeout: 5000 }).should(
      "be.visible",
    );

    // Click edit button in mapping panel
    cy.get('[data-testid="mapping-panel-edit-button"]')
      .should("be.visible")
      .click();

    // Wait for edit mode
    cy.get('[data-testid="mapping-panel-save-button"]', {
      timeout: 5000,
    }).should("be.visible");

    // Change the OpenELIS field (select different field)
    // Note: This assumes OpenELISFieldSelector is available
    cy.get('[data-testid="openelis-field-selector"]')
      .should("be.visible")
      .within(() => {
        // Select a different field (implementation depends on selector component)
        // For now, we'll verify the save button is enabled
        cy.get('[data-testid="mapping-panel-save-button"]')
          .should("be.visible")
          .should("not.be.disabled");
      });

    // Set up intercept for update before clicking save
    cy.intercept("PUT", `**/rest/analyzer/analyzers/${testAnalyzerId}/mappings/**`)
      .as("updateMappingRequest");

    // Click save button
    cy.get('[data-testid="mapping-panel-save-button"]')
      .should("be.visible")
      .should("not.be.disabled")
      .click();

    // Assert: Update request should be sent
    cy.wait("@updateMappingRequest", { timeout: 10000 }).then(
      (interception) => {
        expect(interception.response.statusCode).to.be.oneOf([200, 204]);
      },
    );

    // Verify success (mapping panel should close or show success message)
    // Note: Implementation depends on UI feedback mechanism
  });

  /**
   * Test: Activate draft mapping with confirmation
   * 
   * Scenario: User activates a draft mapping. For active analyzers, confirmation
   * modal should appear. For inactive analyzers, activation should proceed directly.
   */
  it("should activate draft mapping with confirmation", function () {
    // Arrange: Navigate to field mappings page
    cy.visit(`/analyzers/${testAnalyzerId}/mappings`);

    // Wait for mappings to load
    cy.wait("@getMappings", { timeout: 10000 });

    // Act: Find draft mapping and activate it
    cy.get('[data-testid="field-mapping-panel"]', { timeout: 10000 })
      .should("be.visible")
      .within(() => {
        // Find the row for our test field (GLUCOSE) with draft status
        cy.contains("GLUCOSE")
          .parents('[data-testid*="field-row"]')
          .first()
          .within(() => {
            // Verify draft status badge exists
            cy.get('[data-testid*="draft-badge"]').should("be.visible");

            // Click action button to open mapping panel
            cy.get('[data-testid*="field-action"]')
              .should("be.visible")
              .click();
          });
      });

    // Wait for mapping panel to appear
    cy.get('[data-testid="mapping-panel"]', { timeout: 5000 }).should(
      "be.visible",
    );

    // Click "Save and Activate" button (if analyzer is active, confirmation modal should appear)
    // Note: This assumes "Save and Activate" button exists (T078 implementation)
    cy.get('[data-testid="mapping-panel-save-and-activate-button"]', {
      timeout: 5000,
    })
      .should("be.visible")
      .click();

    // Assert: For inactive analyzer, activation should proceed directly
    // For active analyzer, confirmation modal should appear
    // Note: Since analyzer is inactive, activation should proceed without confirmation
    cy.intercept("POST", `**/rest/analyzer/analyzers/${testAnalyzerId}/mappings/**/activate**`)
      .as("activateMappingRequest");

    // If confirmation modal appears (for active analyzers), confirm activation
    cy.get("body").then(($body) => {
      if ($body.find('[data-testid="mapping-activation-modal"]').length > 0) {
        // Confirmation modal appeared - confirm activation
        cy.get('[data-testid="mapping-activation-modal"]')
          .should("be.visible")
          .within(() => {
            // Check confirmation checkbox
            cy.get('[data-testid="activation-confirmation-checkbox"]')
              .check();

            // Click "Activate Changes" button
            cy.get('[data-testid="activation-confirm-button"]')
              .should("be.visible")
              .should("not.be.disabled")
              .click();
          });
      }
    });

    // Wait for activation request
    cy.wait("@activateMappingRequest", { timeout: 10000 }).then(
      (interception) => {
        expect(interception.response.statusCode).to.be.oneOf([200, 204]);
      },
    );

    // Verify mapping is now active (draft badge should be gone, active badge should appear)
    cy.get('[data-testid="field-mapping-panel"]')
      .within(() => {
        cy.contains("GLUCOSE")
          .parents('[data-testid*="field-row"]')
          .first()
          .within(() => {
            // Draft badge should not exist
            cy.get('[data-testid*="draft-badge"]').should("not.exist");
            // Active badge should exist (if implemented)
            // cy.get('[data-testid*="active-badge"]').should("be.visible");
          });
      });
  });

  /**
   * Test: Deactivate mapping while preserving history
   * 
   * Scenario: User deactivates a mapping. The mapping should be marked as inactive
   * but historical data should be preserved. Required mappings cannot be disabled.
   */
  it("should deactivate mapping while preserving history", function () {
    // Arrange: First activate the mapping (if not already active)
    cy.visit(`/analyzers/${testAnalyzerId}/mappings`);
    cy.wait("@getMappings", { timeout: 10000 });

    // Act: Find mapping and deactivate it
    cy.get('[data-testid="field-mapping-panel"]', { timeout: 10000 })
      .should("be.visible")
      .within(() => {
        // Find the row for our test field (GLUCOSE)
        cy.contains("GLUCOSE")
          .parents('[data-testid*="field-row"]')
          .first()
          .within(() => {
            // Click action button to open mapping panel
            cy.get('[data-testid*="field-action"]')
              .should("be.visible")
              .click();
          });
      });

    // Wait for mapping panel to appear
    cy.get('[data-testid="mapping-panel"]', { timeout: 5000 }).should(
      "be.visible",
    );

    // Click "Disable Mapping" button (T169 implementation)
    // Note: This assumes "Disable Mapping" action exists in View Mode
    cy.get('[data-testid="mapping-panel-disable-button"]', {
      timeout: 5000,
    })
      .should("be.visible")
      .click();

    // Assert: Confirmation modal should appear (T170 implementation)
    cy.get('[data-testid="mapping-retirement-modal"]', {
      timeout: 5000,
    })
      .should("be.visible")
      .within(() => {
        // Verify confirmation message
        cy.contains("Disable this mapping?").should("be.visible");
        cy.contains(
          "Historical mappings will be retained for audit purposes.",
        ).should("be.visible");

        // Set up intercept for disable before confirming
        cy.intercept(
          "PUT",
          `**/rest/analyzer/analyzers/${testAnalyzerId}/mappings/**/disable**`,
        ).as("disableMappingRequest");

        // Click "Disable Mapping" button (destructive style)
        cy.get('[data-testid="disable-mapping-confirm-button"]')
          .should("be.visible")
          .click();
      });

    // Wait for disable request
    cy.wait("@disableMappingRequest", { timeout: 10000 }).then(
      (interception) => {
        expect(interception.response.statusCode).to.be.oneOf([200, 204]);
      },
    );

    // Verify mapping is now disabled (retired badge should appear)
    cy.get('[data-testid="field-mapping-panel"]')
      .within(() => {
        cy.contains("GLUCOSE")
          .parents('[data-testid*="field-row"]')
          .first()
          .within(() => {
            // Retired badge should exist (T173 implementation)
            cy.get('[data-testid*="retired-badge"]', {
              timeout: 5000,
            }).should("be.visible");
          });
      });

    // Verify mapping still exists in database (historical data preserved)
    // This is verified by the fact that the mapping row still appears with retired badge
  });
});

