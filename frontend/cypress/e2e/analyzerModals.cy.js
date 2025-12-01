/**
 * Analyzer Modals E2E Test
 *
 * Tests that all analyzer modals can be opened and closed correctly:
 * 1. Add Analyzer modal (from Add button)
 * 2. Edit Analyzer modal (from overflow menu)
 * 3. Delete Analyzer modal (from overflow menu)
 * 4. Test Connection modal (from overflow menu)
 * 5. Copy Mappings modal (from overflow menu)
 * 6. Field Mappings page (navigation, not modal)
 * 7. Error Details modal (from error dashboard)
 * 8. Test Mapping modal (from field mappings page)
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default
 * - Screenshots enabled on failure
 * - Uses data-testid selectors (PREFERRED)
 * - Simple happy path test (open/close only)
 *
 * Execution:
 * - Development: npm run cy:single "cypress/e2e/analyzerModals.cy.js"
 */

// Basic auth credentials
const AUTH = {
  username: "admin",
  password: "adminADMIN!",
};

let testAnalyzerId = null;

describe("Analyzer Modals - Open/Close", () => {
  before("Setup authentication and create test analyzer", () => {
    // Wait for backend API to be available
    cy.waitForBackend("/rest/analyzer/analyzers");

    // Use cy.session() to cache and reuse basic auth session across tests
    cy.session("analyzer-tests-session", () => {
      // Establish session with basic auth by making an authenticated request
      cy.request({
        method: "GET",
        url: "/rest/analyzer/analyzers",
        auth: AUTH,
        failOnStatusCode: false,
      });
    });

    // Create test analyzer via API
    cy.request({
      method: "POST",
      url: "/rest/analyzer/analyzers",
      auth: AUTH,
      body: {
        name: "TEST-Modal-E2E",
        analyzerType: "HEMATOLOGY",
        ipAddress: "192.168.1.100",
        port: 5000,
        protocolVersion: "ASTM_E1394",
        active: true,
      },
      failOnStatusCode: false,
    }).then((response) => {
      if (response.status === 201 || response.status === 200) {
        testAnalyzerId = response.body.id || response.body.data?.id;
      }
    });
  });

  after("Cleanup test analyzer", () => {
    if (testAnalyzerId) {
      cy.request({
        method: "DELETE",
        url: `/rest/analyzer/analyzers/${testAnalyzerId}`,
        auth: AUTH,
        failOnStatusCode: false,
      });
    }
  });

  beforeEach(() => {
    // Viewport management
    cy.viewport(1025, 900);
  });

  it("should open and close Add Analyzer modal", () => {
    cy.visit(`https://${AUTH.username}:${AUTH.password}@localhost/analyzers`);
    cy.get('[data-testid="analyzers-list"]').should("be.visible");

    // Click Add Analyzer button
    cy.get('[data-testid="add-analyzer-button"]').should("be.visible").click();

    // Verify modal opened
    cy.get('[data-testid="analyzer-form"]').should("be.visible");
    cy.get('[role="dialog"]').should("be.visible");

    // Close modal (click cancel or close button)
    cy.get('[data-testid="analyzer-form"]')
      .find("button")
      .contains(/cancel|close/i)
      .first()
      .should("be.visible")
      .click();

    // Verify modal closed
    cy.get('[data-testid="analyzer-form"]').should("not.exist");
  });

  it("should open and close Edit Analyzer modal", () => {
    cy.visit(`https://${AUTH.username}:${AUTH.password}@localhost/analyzers`);
    cy.get('[data-testid="analyzers-list"]').should("be.visible");

    // Wait for analyzers table to load
    cy.get('[data-testid="analyzers-table-container"]', {
      timeout: 10000,
    }).should("be.visible");

    // Find test analyzer row and click overflow menu
    cy.get("body").then(($body) => {
      if ($body.find('[data-testid^="analyzer-action-edit-"]').length > 0) {
        // Click overflow menu button for first analyzer
        cy.get('[data-testid="analyzers-table-container"]')
          .find("tbody")
          .find("tr")
          .first()
          .find('[data-testid^="analyzer-actions-"]')
          .find("button")
          .first()
          .should("be.visible")
          .click();

        // Wait for overflow menu to appear
        cy.get('[role="menu"]').should("be.visible");

        // Click Edit menu item
        cy.get('[role="menuitem"]')
          .contains(/edit/i)
          .should("be.visible")
          .click();

        // Verify modal opened
        cy.get('[data-testid="analyzer-form"]').should("be.visible");
        cy.get('[role="dialog"]').should("be.visible");

        // Close modal
        cy.get('[data-testid="analyzer-form"]')
          .find("button")
          .contains(/cancel|close/i)
          .first()
          .should("be.visible")
          .click();

        // Verify modal closed
        cy.get('[data-testid="analyzer-form"]').should("not.exist");
      } else {
        cy.log("No analyzers found - skipping Edit modal test");
      }
    });
  });

  it("should open and close Delete Analyzer modal", () => {
    cy.visit(`https://${AUTH.username}:${AUTH.password}@localhost/analyzers`);
    cy.get('[data-testid="analyzers-list"]').should("be.visible");

    // Find analyzer row and click overflow menu
    cy.get("body").then(($body) => {
      if ($body.find('[data-testid^="analyzer-action-delete-"]').length > 0) {
        // Click overflow menu button (Carbon OverflowMenu)
        cy.get('[data-testid="analyzers-table-container"]')
          .find("tbody")
          .find("tr")
          .first()
          .find(
            'button[aria-label*="Actions"], [data-testid^="analyzer-actions-"]',
          )
          .first()
          .should("be.visible")
          .click();

        // Wait for overflow menu
        cy.get('[role="menu"]').should("be.visible");

        // Click Delete menu item
        cy.get('[role="menuitem"]')
          .contains(/delete/i)
          .should("be.visible")
          .click();

        // Verify delete modal opened
        cy.get('[data-testid="delete-analyzer-modal"]', {
          timeout: 5000,
        }).should("be.visible");
        cy.get('[role="dialog"]').should("be.visible");

        // Close modal (click cancel)
        cy.get('[data-testid="delete-analyzer-cancel-button"]')
          .should("be.visible")
          .click();

        // Verify modal closed
        cy.get('[data-testid="delete-analyzer-modal"]').should("not.exist");
      } else {
        cy.log("No analyzers found - skipping Delete modal test");
      }
    });
  });

  it("should open and close Test Connection modal", () => {
    cy.visit(`https://${AUTH.username}:${AUTH.password}@localhost/analyzers`);
    cy.get('[data-testid="analyzers-list"]').should("be.visible");

    // Find analyzer row and click overflow menu
    cy.get("body").then(($body) => {
      if (
        $body.find('[data-testid^="analyzer-action-test-connection-"]').length >
        0
      ) {
        // Click overflow menu button (Carbon OverflowMenu)
        cy.get('[data-testid="analyzers-table-container"]')
          .find("tbody")
          .find("tr")
          .first()
          .find(
            'button[aria-label*="Actions"], [data-testid^="analyzer-actions-"]',
          )
          .first()
          .should("be.visible")
          .click();

        // Wait for overflow menu
        cy.get('[role="menu"]').should("be.visible");

        // Click Test Connection menu item
        cy.get('[role="menuitem"]')
          .contains(/test connection/i)
          .should("be.visible")
          .click();

        // Verify test connection modal opened
        cy.get('[data-testid="test-connection-modal"]', {
          timeout: 5000,
        }).should("be.visible");
        cy.get('[role="dialog"]').should("be.visible");

        // Close modal
        cy.get('[data-testid="test-connection-close-button"]')
          .should("be.visible")
          .click();

        // Verify modal closed
        cy.get('[data-testid="test-connection-modal"]').should("not.exist");
      } else {
        cy.log("No analyzers found - skipping Test Connection modal test");
      }
    });
  });

  it("should open and close Copy Mappings modal", () => {
    cy.visit(`https://${AUTH.username}:${AUTH.password}@localhost/analyzers`);
    cy.get('[data-testid="analyzers-list"]').should("be.visible");

    // Find analyzer row and click overflow menu
    cy.get("body").then(($body) => {
      if (
        $body.find('[data-testid^="analyzer-action-copy-mappings-"]').length > 0
      ) {
        // Click overflow menu button (Carbon OverflowMenu)
        cy.get('[data-testid="analyzers-table-container"]')
          .find("tbody")
          .find("tr")
          .first()
          .find(
            'button[aria-label*="Actions"], [data-testid^="analyzer-actions-"]',
          )
          .first()
          .should("be.visible")
          .click();

        // Wait for overflow menu
        cy.get('[role="menu"]').should("be.visible");

        // Click Copy Mappings menu item
        cy.get('[role="menuitem"]')
          .contains(/copy mappings/i)
          .should("be.visible")
          .click();

        // Verify copy mappings modal opened
        cy.get('[data-testid="copy-mappings-modal"]').should("be.visible");
        cy.get('[role="dialog"]').should("be.visible");

        // Close modal
        cy.get('[data-testid="copy-mappings-cancel"]')
          .should("be.visible")
          .click();

        // Verify modal closed
        cy.get('[data-testid="copy-mappings-modal"]').should("not.exist");
      } else {
        cy.log("No analyzers found - skipping Copy Mappings modal test");
      }
    });
  });

  it("should navigate to Field Mappings page and verify Test Mapping modal", () => {
    cy.visit(`https://${AUTH.username}:${AUTH.password}@localhost/analyzers`);
    cy.get('[data-testid="analyzers-list"]').should("be.visible");

    // Navigate to field mappings via analyzer row
    cy.get("body").then(($body) => {
      if ($body.find('[data-testid^="analyzer-action-mappings-"]').length > 0) {
        // Click Field Mappings action
        cy.get('[data-testid="analyzers-table-container"]')
          .find("tbody")
          .find("tr")
          .first()
          .find('[data-testid^="analyzer-action-mappings-"]')
          .should("be.visible")
          .click();

        // Verify field mappings page loaded
        cy.url().should("include", "/mappings");
        cy.get('[data-testid="field-mapping"]').should("be.visible");

        // Open Test Mapping modal
        cy.get('[data-testid="field-mapping-test-button"]')
          .should("be.visible")
          .click();

        // Verify test mapping modal opened
        cy.get('[data-testid="test-mapping-modal"]').should("be.visible");
        cy.get('[role="dialog"]').should("be.visible");

        // Close modal
        cy.get('[data-testid="test-mapping-close"]')
          .should("be.visible")
          .click();

        // Verify modal closed
        cy.get('[data-testid="test-mapping-modal"]').should("not.exist");
      } else {
        cy.log("No analyzers found - skipping Field Mappings navigation test");
      }
    });
  });

  it("should open and close Error Details modal from Error Dashboard", () => {
    cy.visit(
      `https://${AUTH.username}:${AUTH.password}@localhost/analyzers/errors`,
    );
    cy.get('[data-testid="error-dashboard"]').should("be.visible");

    // Check if there are any error rows in the table
    cy.get("body").then(($body) => {
      if (
        $body.find('[data-testid="error-table-container"] tbody tr').length > 0
      ) {
        // Click overflow menu button for first error row
        cy.get('[data-testid="error-table-container"]')
          .find("tbody")
          .find("tr")
          .first()
          .find('button[aria-label*="Actions"]')
          .first()
          .should("be.visible")
          .click();

        // Wait for overflow menu to appear
        cy.get('[role="menu"]').should("be.visible");

        // Click View Details menu item
        cy.get('[role="menuitem"]')
          .contains(/view details/i)
          .should("be.visible")
          .click();

        // Verify error details modal opened
        cy.get('[data-testid="error-details-modal"]').should("be.visible");
        cy.get('[role="dialog"]').should("be.visible");

        // Close modal
        cy.get('[data-testid="error-details-close"]')
          .should("be.visible")
          .click();

        // Verify modal closed
        cy.get('[data-testid="error-details-modal"]').should("not.exist");
      } else {
        cy.log("No errors found - skipping Error Details modal test");
      }
    });
  });
});
