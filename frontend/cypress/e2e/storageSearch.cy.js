/**
 * E2E Tests for Storage Search
 * Tests core user workflow: User can search and see results
 */

describe("Storage Search", function () {
  before(() => {
    cy.setupStorageTests();
  });

  after(() => {
    cy.cleanupStorageTests();
  });

  describe("Samples Tab Search", function () {
    beforeEach(() => {
      cy.visit("/Storage/samples");
      cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
      cy.get('[data-testid="tab-samples"]', { timeout: 10000 })
        .should("be.visible")
        .should("have.attr", "aria-selected", "true");
      cy.get(".cds--tab-content:visible", { timeout: 10000 }).should(
        "be.visible",
      );
      cy.get('[data-testid="sample-list"]', { timeout: 10000 }).should(
        "be.visible",
      );
    });

    it("Should search for samples and display results", function () {
      // Take screenshot: before search
      cy.screenshot("01-before-search");

      // Search for sample
      cy.get('[data-testid="sample-search-input"]', { timeout: 10000 })
        .should("be.visible")
        .clear()
        .type("101");

      // Take screenshot: after typing
      cy.screenshot("02-after-typing");

      // Verify search input has value (user-visible behavior)
      cy.get('[data-testid="sample-search-input"]').should("have.value", "101");

      // Verify table updates (user-visible behavior)
      cy.get('[role="table"] tbody tr, .cds--data-table tbody tr', {
        timeout: 10000,
      })
        .filter(":visible")
        .should("exist");

      // Take screenshot: search results
      cy.screenshot("03-search-results");
    });
  });

  describe("Rooms Tab Search", function () {
    beforeEach(() => {
      cy.visit("/Storage/rooms");
      cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
      cy.get('[data-testid="tab-rooms"]', { timeout: 10000 })
        .should("be.visible")
        .should("have.attr", "aria-selected", "true");
      cy.get(".cds--tab-content:visible", { timeout: 10000 }).should(
        "be.visible",
      );
    });

    it("Should search for rooms and display results", function () {
      // Search for room
      cy.get('[data-testid="room-search-input"]', { timeout: 10000 })
        .should("be.visible")
        .clear()
        .type("Main");

      // Verify search input has value (user-visible behavior)
      cy.get('[data-testid="room-search-input"]').should("have.value", "Main");

      // Verify table updates (user-visible behavior)
      cy.get('[role="table"] tbody tr, .cds--data-table tbody tr', {
        timeout: 10000,
      })
        .filter(":visible")
        .should("exist");
    });
  });
});
