/**
 * E2E Test: Location Management Modal Form Behavior
 * Tests core workflow: Opening modal and selecting location
 */

describe("Location Management Modal Form Behavior", function () {
  before(() => {
    cy.setupStorageTests();
  });

  after(() => {
    cy.cleanupStorageTests();
  });

  it("Should open location management modal and show location selector", function () {
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

    // Take screenshot: samples table loaded
    cy.screenshot("01-samples-table-loaded");

    // Click overflow menu on first sample
    cy.get('[data-testid="sample-row"]')
      .filter(":visible")
      .first()
      .within(() => {
        cy.get('[data-testid="sample-actions-overflow-menu"]')
          .should("be.visible")
          .click({ force: true });
      });

    // Click Manage Location option
    cy.get('[data-testid="manage-location-menu-item"]', { timeout: 10000 })
      .should("be.visible")
      .click({ force: true });

    // Take screenshot: menu clicked
    cy.screenshot("02-menu-clicked");

    // Verify location management modal opens
    cy.get('[data-testid="location-management-modal"]', {
      timeout: 10000,
    }).should("be.visible");

    // Take screenshot: modal opened
    cy.screenshot("03-modal-opened");

    // Verify location selector is visible
    cy.get('[data-testid="location-management-modal"]')
      .should("be.visible")
      .within(() => {
        cy.get('[data-testid="location-filter-dropdown"]').should("be.visible");
        cy.get('[data-testid="new-location-section"]').should("be.visible");
      });

    // Take screenshot: location selector visible
    cy.screenshot("04-location-selector-visible");
  });
});
