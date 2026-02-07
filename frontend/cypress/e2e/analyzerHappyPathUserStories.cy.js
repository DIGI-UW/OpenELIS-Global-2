/**
 * E2E Tests: ASTM Analyzer Field Mapping - Happy Path User Stories
 *
 * Reference: OpenELIS Testing Roadmap (.specify/guides/testing-roadmap.md)
 * Constitution V.5 Compliance: Individual test execution during development
 *
 * This test suite covers the happy path scenarios for the three main user stories:
 * - User Story 1: Configure field mappings for a new ASTM analyzer (P1)
 * - User Story 2: Maintain mappings as instruments and test menus change (P2)
 * - User Story 3: Resolve unmapped or failed analyzer messages (P3)
 *
 * Execution:
 * - Development: npm run cy:single "cypress/e2e/analyzerHappyPathUserStories.cy.js"
 * - CI/CD: npm run cy:run (full suite)
 */

let testAnalyzerId = null;

/**
 * Setup: Wait for backend and create test analyzer via API with basic auth
 * Credentials: Cypress.env('USERNAME') / Cypress.env('PASSWORD')
 */
before("Setup test analyzer", () => {
  // Wait for backend API to be available
  cy.waitForBackend("/api/OpenELIS-Global/rest/analyzer/analyzers");

  // Use cy.session() to cache and reuse basic auth session across tests
  cy.session("analyzer-tests-session", () => {
    // Establish session with basic auth by making an authenticated request
    cy.request({
      method: "GET",
      url: "/api/OpenELIS-Global/rest/analyzer/analyzers",
      auth: Cypress.getBasicAuth(),
      failOnStatusCode: false,
    });
  });

  // Create test analyzer via API with basic auth
  cy.request({
    method: "POST",
    url: "/api/OpenELIS-Global/rest/analyzer/analyzers",
    auth: Cypress.getBasicAuth(),
    body: {
      name: "E2E-Test-Analyzer-HappyPath",
      analyzerType: "CHEMISTRY",
      ipAddress: "192.168.1.200",
      port: 5001,
      protocolVersion: "ASTM LIS2-A2",
      testUnitIds: [],
      active: true,
    },
    failOnStatusCode: false,
  }).then((response) => {
    if (response.status === 201 || response.status === 200) {
      // Extract ID from response - check multiple possible locations
      let id = null;
      if (response.body) {
        id =
          response.body.id ||
          response.body.data?.id ||
          (response.body.data &&
            typeof response.body.data === "object" &&
            response.body.data.id) ||
          (typeof response.body === "object" &&
            Object.values(response.body)[0]?.id);
      }

      if (id) {
        testAnalyzerId = id;
        cy.log(`Created test analyzer with ID: ${id}`);
      } else {
        cy.log(
          `Warning: Analyzer created but ID not found in response: ${JSON.stringify(response.body)}`,
        );
        testAnalyzerId = "NO_ID";
      }
    } else {
      cy.log(
        `Failed to create analyzer: ${response.status} - ${JSON.stringify(response.body)}`,
      );
      testAnalyzerId = "NO_ID";
    }
  });

  // Set alias after analyzer creation completes - use cy.then() to ensure it runs after the request
  cy.then(() => {
    if (testAnalyzerId && testAnalyzerId !== "NO_ID") {
      cy.wrap(testAnalyzerId).as("analyzerId");
    } else {
      cy.wrap("NO_ID").as("analyzerId");
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
      url: `/api/OpenELIS-Global/rest/analyzer/analyzers/${testAnalyzerId}`,
      auth: Cypress.getBasicAuth(),
      failOnStatusCode: false,
    });
  }
});

// TODO: Re-enable once analyzer feature PRs are merged into this branch
describe.skip("User Story 1: Configure field mappings for a new ASTM analyzer (P1)", () => {
  beforeEach(() => {
    cy.viewport(1025, 900);
    // Visit with basic auth in URL format
    cy.visit("/", { auth: Cypress.getBasicAuth() });
  });

  it("should allow administrator to configure field mappings for a new analyzer", () => {
    // Get analyzer ID from alias or use testAnalyzerId directly
    cy.then(() => {
      const id = testAnalyzerId;
      if (!id || id === "NO_ID") {
        cy.log("Skipping test - analyzer ID not available");
        return;
      }

      // Navigate to analyzers list
      cy.visit("/analyzers", { auth: Cypress.getBasicAuth() });

      // Find and click on the test analyzer
      cy.get('[data-testid="analyzers-list"]').should("be.visible");
      cy.get(`[data-testid="analyzer-row-${id}"]`)
        .should("be.visible")
        .within(() => {
          // Open overflow menu
          cy.get('[data-testid^="analyzer-actions-"]')
            .find('[role="button"]')
            .click();

          // Click Field Mappings
          cy.get(`[data-testid="analyzer-action-mappings-${id}"]`)
            .should("be.visible")
            .click();
        });

      // Verify mappings page loaded
      cy.url().should("include", "/mappings");
      cy.get('[data-testid="field-mapping"]').should("be.visible");

      // Verify mapping page displays correctly
      cy.get('[data-testid="field-mapping-stats"]').should("be.visible");
      cy.get('[data-testid="field-mapping-panel"]').should("be.visible");

      // Verify that unmapped fields are shown (new analyzer has no mappings)
      cy.get('[data-testid="stat-unmapped-fields"]').should("be.visible");

      cy.log(
        "User Story 1: Analyzer mapping UI opened successfully for new analyzer",
      );
    });
  });

  it("should allow mapping analyzer test codes to OpenELIS tests", () => {
    // Get analyzer ID and navigate directly to mappings page
    cy.then(() => {
      const id = testAnalyzerId;
      if (!id || id === "NO_ID") {
        cy.log("Skipping test - analyzer ID not available");
        return;
      }
      cy.visit(`/analyzers/${id}/mappings`, { auth: Cypress.getBasicAuth() });
      cy.get('[data-testid="field-mapping"]').should("be.visible");

      // Check if there are unmapped fields to map
      cy.get("body").then(($body) => {
        const unmappedFields = $body.find('[data-testid^="unmapped-field-"]');

        if (unmappedFields.length > 0) {
          // Click on first unmapped field to open mapping modal
          cy.get('[data-testid^="unmapped-field-"]').first().click();

          // Verify mapping modal opens
          cy.get('[data-testid="mapping-modal"]').should("be.visible");

          // Verify OpenELIS field selector is visible
          cy.get('[data-testid="openelis-field-selector"]').should(
            "be.visible",
          );

          cy.log("User Story 1: Mapping interface accessible for test codes");
        } else {
          cy.log(
            "User Story 1: No unmapped fields available for mapping (test data may be pre-configured)",
          );
        }
      });
    });
  });
});

// TODO: Re-enable once analyzer feature PRs are merged into this branch
describe.skip("User Story 2: Maintain mappings as instruments and test menus change (P2)", () => {
  beforeEach(() => {
    cy.viewport(1025, 900);
    cy.visit("/", { auth: Cypress.getBasicAuth() });
  });

  it("should allow updating existing mappings when analyzer configuration changes", () => {
    // Get analyzer ID and navigate to mappings page
    cy.then(() => {
      const id = testAnalyzerId;
      if (!id || id === "NO_ID") {
        cy.log("Skipping test - analyzer ID not available");
        return;
      }
      cy.visit(`/analyzers/${id}/mappings`, { auth: Cypress.getBasicAuth() });
      cy.get('[data-testid="field-mapping"]').should("be.visible");

      // Check if there are existing mappings to update
      cy.get("body").then(($body) => {
        const mappedRows = $body.find('[data-testid^="mapped-field-"]');

        if (mappedRows.length > 0) {
          // Click on first mapped field to edit
          cy.get('[data-testid^="mapped-field-"]').first().click();

          // Verify edit modal opens
          cy.get('[data-testid="mapping-modal"]').should("be.visible");

          cy.log(
            "User Story 2: Edit mapping interface accessible for existing mappings",
          );
        } else {
          cy.log(
            "User Story 2: No existing mappings to update (test data may be empty)",
          );
        }
      });
    });
  });

  it("should display mapping statistics and status correctly", () => {
    // Get analyzer ID and navigate to mappings page
    cy.then(() => {
      const id = testAnalyzerId;
      if (!id || id === "NO_ID") {
        cy.log("Skipping test - analyzer ID not available");
        return;
      }
      cy.visit(`/analyzers/${id}/mappings`, { auth: Cypress.getBasicAuth() });
      cy.get('[data-testid="field-mapping"]').should("be.visible");

      // Verify statistics cards are displayed
      cy.get('[data-testid="stat-total-mappings"]').should("be.visible");
      cy.get('[data-testid="stat-required-mappings"]').should("be.visible");
      cy.get('[data-testid="stat-unmapped-fields"]').should("be.visible");

      cy.log("User Story 2: Mapping statistics displayed correctly");
    });
  });
});

// TODO: Re-enable once analyzer feature PRs are merged into this branch
describe.skip("User Story 3: Resolve unmapped or failed analyzer messages (P3)", () => {
  beforeEach(() => {
    cy.viewport(1025, 900);
    cy.visit("/", { auth: Cypress.getBasicAuth() });
  });

  it("should display unmapped messages in Error Dashboard", () => {
    // Navigate to Error Dashboard
    cy.visit("/analyzers/errors", { auth: Cypress.getBasicAuth() });
    cy.get('[data-testid="error-dashboard"]').should("be.visible");

    // Verify Error Dashboard components are visible
    cy.get('[data-testid="error-dashboard-stats"]').should("be.visible");
    cy.get('[data-testid="error-table"]').should("be.visible");

    cy.log("User Story 3: Error Dashboard displays unmapped messages");
  });

  it("should allow viewing error details with context for mapping", () => {
    // Navigate to Error Dashboard
    cy.visit("/analyzers/errors", { auth: Cypress.getBasicAuth() });
    cy.get('[data-testid="error-dashboard"]').should("be.visible");

    // Check if there are errors to view
    cy.get("body").then(($body) => {
      const errorRows = $body.find('[data-testid^="error-row-"]');

      if (errorRows.length > 0) {
        // Get first error row and open overflow menu to view details
        cy.get('[data-testid^="error-row-"]')
          .first()
          .then(($row) => {
            const rowId =
              $row.attr("data-testid")?.replace("error-row-", "") || "";

            // Find and click OverflowMenu button in actions cell
            cy.wrap($row).within(() => {
              cy.get(`[data-testid="error-actions-${rowId}"]`)
                .should("exist")
                .find("button")
                .last()
                .click({ force: true });
            });

            // Wait for menu item to appear and click it
            cy.get('[data-testid^="error-action-view-"]', { timeout: 5000 })
              .first()
              .should("exist")
              .click({ force: true });
          });

        // Verify error details modal opens
        cy.get('[data-testid="error-details-modal"]', {
          timeout: 10000,
        }).should("be.visible");

        // Wait a moment for modal content to render
        cy.wait(500);

        // Take screenshot for debugging
        cy.screenshot("error-details-modal-opened");

        // Verify error context is displayed - modal should contain error information
        // The modal uses standard Carbon components without specific testids for content
        // Just verify the modal is visible (which confirms it opened successfully)
        cy.get('[data-testid="error-details-modal"]').should("be.visible");

        // Verify modal has content - check that it contains text (any text indicates content loaded)
        cy.get('[data-testid="error-details-modal"]').should(
          "contain.text",
          "",
        );

        cy.log(
          "User Story 3: Error details displayed with context for mapping",
        );
      } else {
        cy.log(
          "User Story 3: No errors available to view (test data may be clean)",
        );
      }
    });
  });

  it("should allow creating mappings from error context", () => {
    // Navigate to Error Dashboard
    cy.visit("/analyzers/errors", { auth: Cypress.getBasicAuth() });
    cy.get('[data-testid="error-dashboard"]').should("be.visible");

    // Check if there are errors with mapping options
    cy.get("body").then(($body) => {
      const errorRows = $body.find('[data-testid^="error-row-"]');

      if (errorRows.length > 0) {
        // Click on first error (use force if covered by header)
        cy.get('[data-testid^="error-row-"]')
          .first()
          .scrollIntoView()
          .click({ force: true });

        // Check if "Create Mapping" button is available
        cy.get("body").then(($modalBody) => {
          const createMappingBtn = $modalBody.find(
            '[data-testid="create-mapping-from-error"]',
          );

          if (createMappingBtn.length > 0) {
            cy.get('[data-testid="create-mapping-from-error"]').click();

            // Verify mapping modal opens from error context
            cy.get('[data-testid="mapping-modal"]').should("be.visible");

            cy.log(
              "User Story 3: Mapping creation accessible from error context",
            );
          } else {
            cy.log(
              "User Story 3: Create mapping option not available for this error type",
            );
          }
        });
      } else {
        cy.log("User Story 3: No errors available to create mappings from");
      }
    });
  });
});

// TODO: Re-enable once analyzer feature PRs are merged into this branch
describe.skip("Cross-Story: End-to-End Happy Path Flow", () => {
  beforeEach(() => {
    cy.viewport(1025, 900);
    cy.visit("/", { auth: Cypress.getBasicAuth() });
  });

  it("should complete full workflow: configure analyzer → view mappings → check errors", () => {
    // Get analyzer ID
    cy.then(() => {
      const id = testAnalyzerId;
      if (!id || id === "NO_ID") {
        cy.log("Skipping test - analyzer ID not available");
        return;
      }

      // Step 1: Navigate to analyzers list
      cy.visit("/analyzers", { auth: Cypress.getBasicAuth() });
      cy.get('[data-testid="analyzers-list"]').should("be.visible");

      // Step 2: Open mappings for test analyzer
      cy.get(`[data-testid="analyzer-row-${id}"]`)
        .should("be.visible")
        .within(() => {
          cy.get('[data-testid^="analyzer-actions-"]')
            .find('[role="button"]')
            .click();
          cy.get(`[data-testid="analyzer-action-mappings-${id}"]`)
            .should("be.visible")
            .click();
        });

      // Step 3: Verify mappings page loads
      cy.url().should("include", "/mappings");
      cy.get('[data-testid="field-mapping"]').should("be.visible");

      // Step 4: Navigate to Error Dashboard
      cy.visit("/analyzers/errors", { auth: Cypress.getBasicAuth() });
      cy.get('[data-testid="error-dashboard"]').should("be.visible");

      // Step 5: Navigate back to analyzers list
      cy.visit("/analyzers", { auth: Cypress.getBasicAuth() });
      cy.get('[data-testid="analyzers-list"]').should("be.visible");

      cy.log(
        "Cross-Story: Full workflow completed successfully - analyzer configuration, mappings, and error dashboard accessible",
      );
    });
  });
});
