/**
 * E2E Test: Samples Table Display
 * Tests core workflow: Samples table is visible and displays data
 */

describe("Samples Table Display", function () {
  before(() => {
    cy.setupStorageTests();
  });

  after(() => {
    cy.cleanupStorageTests();
  });

  it("Should display samples table with structure", function () {
    cy.visit("/Storage/samples");
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
    cy.get('[data-testid="tab-samples"]', { timeout: 10000 })
      .should("be.visible")
      .should("have.attr", "aria-selected", "true");
    cy.get(".cds--tab-content:visible", { timeout: 10000 }).should(
      "be.visible",
    );

    // Take screenshot: dashboard loaded
    cy.screenshot("01-dashboard-loaded");

    // Verify sample list container is visible
    cy.get('[data-testid="sample-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Take screenshot: sample list visible
    cy.screenshot("02-sample-list-visible");

    // Verify table structure exists (user-visible behavior)
    cy.get('[role="table"], .cds--data-table', { timeout: 10000 })
      .filter(":visible")
      .should("be.visible");

    // Verify table has headers (user-visible behavior)
    cy.get('[role="table"] thead, .cds--data-table thead', { timeout: 10000 })
      .filter(":visible")
      .should("exist");

    // Take screenshot: table structure
    cy.screenshot("03-table-structure");

    // Verify table has body (even if empty, structure should exist)
    cy.get('[role="table"] tbody, .cds--data-table tbody', { timeout: 10000 })
      .filter(":visible")
      .should("exist");
  });
});
