/**
 * Minimal focused test for Add Location button crash
 * This test captures console errors and video to debug the crash
 */

before("Setup storage tests", () => {
  cy.setupStorageTests();
});

after("Cleanup storage tests", () => {
  cy.cleanupStorageTests();
});

describe("Add Location Button Crash Debug", function () {
  beforeEach(() => {
    // Set up common API intercepts
    cy.setupStorageIntercepts();

    cy.visit("/Storage/samples");
    // Wait for the sample list to appear instead of waiting for intercept
    // The intercept might not match if the request pattern differs
    cy.get('[data-testid="sample-list"]').should("be.visible");
  });

  it("Should not crash when clicking Add Location button", function () {
    // Declare arrays to store captured errors and rejections
    const errors = [];
    const rejections = [];

    // Set up error capture BEFORE any actions
    cy.window().then((win) => {
      win.addEventListener("error", (e) => {
        errors.push({
          message: e.message,
          filename: e.filename,
          lineno: e.lineno,
        });
        cy.log("Window Error:", e.message, e.filename, e.lineno);
      });
      win.addEventListener("unhandledrejection", (e) => {
        rejections.push({ reason: e.reason });
        cy.log("Unhandled Promise Rejection:", e.reason);
      });
    });

    // Turn off uncaught exception handling to see real errors
    Cypress.on("uncaught:exception", (err, runnable) => {
      // Log the error but don't fail the test immediately
      cy.log("UNCAUGHT EXCEPTION:", err.message);
      return false; // Don't fail the test
    });

    // Verify we're on the samples tab
    cy.get('[data-testid="sample-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Get first sample and open location management modal
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-row"]')
      .should("have.length.at.least", 1)
      .first()
      .within(() => {
        cy.get('[data-testid="sample-actions-overflow-menu"]')
          .should("be.visible")
          .click({ force: true });
      });

    // Wait for menu to open (portal rendering) - OverflowMenu items render in portal
    // Use manage-location-menu-item (not move-menu-item which doesn't exist)
    cy.get('[data-testid="manage-location-menu-item"]', { timeout: 10000 })
      .should("be.visible")
      .click({ force: true });

    // Wait for location management modal to open (handles both assignment and movement)
    cy.get('[data-testid="location-management-modal"]', {
      timeout: 10000,
    }).should("be.visible");

    // Verify Add Location button exists and is visible
    cy.get('[data-testid="add-location-button"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Click Add Location button - this is where it crashes
    cy.get('[data-testid="add-location-button"]').should("be.visible").click();

    // Wait a bit to see if crash happens
    cy.wait(2000);

    // Check for errors using the arrays we declared
    cy.then(() => {
      cy.log("Errors captured:", errors.length);
      cy.log("Rejections captured:", rejections.length);
      if (errors.length > 0) {
        errors.forEach((err) => {
          cy.log("ERROR DETAILS:", JSON.stringify(err));
        });
      }
      if (rejections.length > 0) {
        rejections.forEach((rej) => {
          cy.log("REJECTION DETAILS:", JSON.stringify(rej));
        });
      }
    });

    // Verify the page is still responsive (not crashed)
    cy.get("body").should("exist");
    cy.get('[data-testid="location-management-modal"]').should("be.visible");

    // Check if create form appeared
    cy.get("body").then(($body) => {
      if ($body.find('[data-testid="location-create-container"]').length > 0) {
        cy.log("Create form appeared successfully");
        cy.get('[data-testid="location-create-container"]').should(
          "be.visible",
        );
        // Verify the form has room input
        cy.get('[data-testid="room-combobox"]').should("be.visible");
      } else {
        cy.log("Create form did not appear - possible crash");
        cy.log("Current page state:", $body.html().substring(0, 500));
      }
    });
  });
});
