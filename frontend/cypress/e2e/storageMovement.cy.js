/**
 * E2E Tests for Sample Movement
 * Tests core user workflow: Move sample between locations
 *
 * Constitution V.5 Compliance:
 * - Uses data-testid selectors (priority 1)
 * - Tests user-visible behavior, not implementation details
 * - Simple, efficient, non-redundant
 */

import StorageAssignmentPage from "../pages/StorageAssignmentPage";

describe("Storage Movement", function () {
  let storageAssignmentPage = null;

  before(() => {
    cy.setupStorageTests();
  });

  after(() => {
    cy.cleanupStorageTests();
  });

  it("Should open location management modal for moving sample", function () {
    cy.setupStorageIntercepts();
    cy.visit("/Storage/samples");
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
    cy.get('[data-testid="tab-samples"]', { timeout: 10000 })
      .should("be.visible")
      .should("have.attr", "aria-selected", "true");
    cy.get(".cds--tab-content:visible", { timeout: 10000 }).should(
      "be.visible",
    );

    // Wait for samples to load
    cy.get('[data-testid="sample-list"]', { timeout: 10000 }).should(
      "be.visible",
    );
    cy.get('[data-testid="sample-row"]', { timeout: 10000 })
      .filter(":visible")
      .should("have.length.at.least", 1);

    // Click overflow menu on first sample
    cy.get('[data-testid="sample-row"]')
      .filter(":visible")
      .first()
      .within(() => {
        cy.get('[data-testid="sample-actions-overflow-menu"]')
          .should("be.visible")
          .click({ force: true });
      });

    // Click Manage Location option (menu items render in portal)
    cy.get('[data-testid="manage-location-menu-item"]', { timeout: 10000 })
      .should("be.visible")
      .click({ force: true });

    // Verify location management modal opens (user-visible behavior)
    cy.get('[data-testid="location-management-modal"]', {
      timeout: 10000,
    }).should("be.visible");

    // Verify location selector is visible (user-visible behavior)
    cy.get('[data-testid="location-management-modal"]')
      .should("be.visible")
      .within(() => {
        cy.get('[data-testid="location-filter-dropdown"]').should("be.visible");
        cy.get('[data-testid="new-location-section"]').should("be.visible");
      });
  });
});
