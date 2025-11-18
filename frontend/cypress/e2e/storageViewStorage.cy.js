/**
 * E2E Test: View Storage Modal
 * Tests core user workflow: Opening location management modal and viewing sample info
 */

describe("View Storage Modal", function () {
  before(() => {
    cy.setupStorageTests();
  });

  after(() => {
    cy.cleanupStorageTests();
  });

  it("Should open location management modal and display sample information", function () {
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

    // Take screenshot: samples loaded
    cy.screenshot("01-samples-loaded");

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

    // Verify sample information section is visible (user-visible behavior)
    cy.get('[data-testid="location-management-modal"]')
      .should("be.visible")
      .within(() => {
        cy.get('[data-testid="sample-info-section"]').should("be.visible");
        cy.get('[data-testid="new-location-section"]').should("be.visible");
      });

    // Take screenshot: modal content visible
    cy.screenshot("04-modal-content-visible");
  });
});
