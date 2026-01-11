import HomePage from "../pages/HomePage";
import "../support/load-storage-fixtures";
import "../support/storage-setup";

/**
 * Smoke test for Storage Location CRUD - Quick verification that modals work
 * Run this first to verify basic functionality before running full suite
 * Usage: npm run cy:run -- --spec "cypress/e2e/storageLocationCRUD-smoke.cy.js"
 */

let homePage = null;

before("Setup storage tests", () => {
  cy.setupStorageTests().then((page) => {
    homePage = page;
  });
});

after("Cleanup storage tests", () => {
  cy.cleanupStorageTests();
});

describe("Storage Location CRUD - Smoke Test", function () {
  before(function () {
    // Use large viewport to ensure all modal content (including warnings/checkboxes) is visible
    cy.viewport(1920, 1080);
    cy.visit("/Storage");
    cy.get(".storage-dashboard", { timeout: 3000 }).should("be.visible");
  });

  afterEach(function () {
    // Verify no modals remain open (test isolation check)
    cy.get("body").then(($body) => {
      const modalSelectors = [
        '[data-testid="edit-location-modal"]',
        '[data-testid="delete-location-modal"]',
        '[data-testid="storage-location-modal"]',
      ];

      modalSelectors.forEach((selector) => {
        const modal = $body.find(selector);
        if (modal.length > 0) {
          cy.log(`WARNING: Modal ${selector} still exists after test`);
          if (modal.is(":visible")) {
            const closeBtn = modal.find('button[aria-label="Close"]');
            if (closeBtn.length > 0) {
              cy.wrap(closeBtn.first()).click({ force: true });
              cy.get(selector, { timeout: 3000 }).should("not.exist");
            } else {
              cy.get("body").type("{esc}", { force: true });
              cy.get(selector, { timeout: 3000 }).should("not.exist");
            }
          }
        }
      });
    });
  });

  it("should open create room modal, fill form, and close properly", function () {
    cy.get('[data-testid="tab-rooms"]').click();
    cy.get('button[role="tab"]')
      .contains("Rooms")
      .should("have.attr", "aria-selected", "true");

    // Verify no modal exists before opening
    cy.get("body").then(($body) => {
      const modal = $body.find('[data-testid="storage-location-modal"]');
      expect(modal.length).to.equal(0);
    });

    cy.get('[data-testid="add-room-button"]')
      .should("be.visible")
      .should("not.be.disabled")
      .click();

    // Wait for modal to open - check existence, aria-hidden, then visibility
    cy.get('[data-testid="storage-location-modal"]', {
      timeout: 3000,
    })
      .should("exist")
      .should("have.attr", "aria-hidden", "false")
      .should("be.visible");

    // Verify form fields are visible
    cy.get("#room-name").should("be.visible");
    cy.get("#room-code").should("be.visible");

    // Fill in some data to simulate real usage
    cy.get("#room-name").type("Test Room");
    cy.get("#room-code").type("TR-001");

    // Close modal and verify it unmounts completely
    cy.get('[data-testid="storage-location-modal"]')
      .find('button[aria-label="Close"]')
      .click();

    // Modal should completely unmount (not just hide)
    cy.get('[data-testid="storage-location-modal"]', { timeout: 3000 }).should(
      "not.exist",
    );

    // Verify we can interact with the page after modal closes
    cy.get('[data-testid="tab-rooms"]').should("be.visible");
  });

  it("should open edit modal, load data, interact with code change warning, and close properly", function () {
    cy.get('[data-testid="tab-rooms"]').click();

    // Verify no modal exists before opening
    cy.get("body").then(($body) => {
      const modal = $body.find('[data-testid="edit-location-modal"]');
      expect(modal.length).to.equal(0);
    });

    // Wait for table to load
    cy.get('[data-testid^="room-row-"]', { timeout: 3000 })
      .should("have.length.at.least", 1)
      .first()
      .within(() => {
        cy.get('[data-testid="location-actions-overflow-menu"]')
          .should("be.visible")
          .click({ force: true });
      });

    // Wait for overflow menu to fully open (Carbon portal)
    cy.get('[data-testid="edit-location-menu-item"]', { timeout: 3000 })
      .should("be.visible")
      .click({ force: true });

    // Wait for modal to open with full checks
    cy.get('[data-testid="edit-location-modal"]', {
      timeout: 3000,
    })
      .should("exist")
      .should("have.attr", "aria-hidden", "false")
      .should("be.visible");

    // Wait for form to load (API call completes)
    cy.get('[data-testid="edit-location-room-name"]', { timeout: 3000 }).should(
      "be.visible",
    );

    // Verify we can interact with the form
    cy.get("#room-active", { timeout: 3000 }).should("exist");

    // TEST: Change the code to trigger warning and verify checkbox appears
    cy.get('[data-testid="edit-location-room-code"]')
      .clear({ force: true })
      .type("NEWCODE", { force: true });

    // Verify code change warning appears (may be below fold, so use force)
    cy.get('[data-testid="code-change-acknowledge-checkbox"]', {
      timeout: 3000,
    })
      .should("exist")
      .scrollIntoView({ offset: { top: -100, left: 0 } });

    // Verify save button is disabled before acknowledging
    cy.get('[data-testid="edit-location-save-button"]').should("be.disabled");

    // TEST: Check the acknowledgment checkbox (use force since it might be covered)
    cy.get('[data-testid="code-change-acknowledge-checkbox"]')
      .check({ force: true })
      .should("be.checked");

    // Verify save button is now enabled after acknowledgment
    cy.get('[data-testid="edit-location-save-button"]', { timeout: 3000 })
      .should("be.visible")
      .should("not.be.disabled");

    // Close modal and verify complete unmount
    cy.get('[data-testid="edit-location-modal"]')
      .find('button[aria-label="Close"]')
      .click();

    // Modal should completely unmount (not just hide)
    cy.get('[data-testid="edit-location-modal"]', { timeout: 3000 }).should(
      "not.exist",
    );

    // Verify we can interact with page after modal closes (no overlay blocking)
    cy.get('[data-testid="tab-rooms"]').should("be.visible").click();
    cy.get('button[role="tab"]')
      .contains("Rooms")
      .should("have.attr", "aria-selected", "true");
  });
});
