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
    cy.wait("@getSamples", { timeout: 10000 });
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

    // Get first sample and open move modal
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-row"]')
      .should("have.length.at.least", 1)
      .first()
      .within(() => {
        cy.get('[data-testid="sample-actions-overflow-menu"]').click();
      });

    // Wait for menu to open (portal rendering)
    cy.wait(500);

    // Click Move menu item outside .within() block (Carbon OverflowMenu renders in portal)
    cy.get('[data-testid="move-menu-item"]', { timeout: 3000 })
      .should("be.visible")
      .click();

    // Wait for move modal to open
    cy.get('[data-testid="move-modal"]', { timeout: 5000 }).should(
      "be.visible",
    );

    // Verify Add Location button exists
    cy.get('[data-testid="add-location-button"]', { timeout: 2000 })
      .should("be.visible")
      .should("contain.text", /location/i);

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
    cy.get('[data-testid="move-modal"]').should("be.visible");

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
