/**
 * E2E Tests for Location CRUD Operations
 * Tests edit and delete workflows - simple, efficient, focused on user workflows
 *
 * Constitution V.5 Compliance:
 * - Uses data-testid selectors (priority 1)
 * - Tests user-visible behavior, not implementation details
 * - Simple, non-redundant tests
 */

describe("Location CRUD Operations", function () {
  before(() => {
    cy.setupStorageTests();
  });

  after(() => {
    cy.cleanupStorageTests();
  });

  describe("Edit Location", function () {
    it("Should edit room and verify update in table", function () {
      cy.visit("/Storage/rooms");
      cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
      cy.get('[data-testid="tab-rooms"]', { timeout: 10000 })
        .should("be.visible")
        .should("have.attr", "aria-selected", "true");
      cy.get(".cds--tab-content:visible", { timeout: 10000 }).should(
        "be.visible",
      );

      // Wait for table to load
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 })
        .filter(":visible")
        .should("have.length.at.least", 1)
        .first()
        .invoke("attr", "data-testid")
        .then((testId) => {
          const roomId = testId.replace("room-row-", "");
          const newName = `Updated Room ${Date.now()}`;

          // Get initial name from table
          cy.get(`[data-testid="room-row-${roomId}"]`)
            .filter(":visible")
            .should("exist")
            .then(($row) => {
              const initialText = $row.text();

              // Open edit modal
              cy.get(`[data-testid="room-row-${roomId}"]`)
                .filter(":visible")
                .within(() => {
                  cy.get('[data-testid="location-actions-overflow-menu"]')
                    .should("be.visible")
                    .click({ force: true });
                });

              cy.get('[data-testid="edit-location-menu-item"]')
                .should("be.visible")
                .click({ force: true });

              // Wait for modal to open
              cy.get('[data-testid="edit-location-modal"]', {
                timeout: 10000,
              }).should("be.visible");

              // Wait for form to be populated
              cy.get('[data-testid="edit-location-room-name"]', {
                timeout: 10000,
              })
                .should("be.visible")
                .should("not.have.value", "");

              // Update name
              cy.get('[data-testid="edit-location-room-name"]')
                .clear()
                .type(newName);

              // Save
              cy.get('[data-testid="edit-location-save-button"]')
                .should("not.be.disabled")
                .click();

              // Verify modal closes
              cy.get('[data-testid="edit-location-modal"]', {
                timeout: 10000,
              }).should("not.be.visible");

              // Verify table shows updated name (user-visible behavior)
              cy.get(`[data-testid="room-row-${roomId}"]`, {
                timeout: 10000,
              })
                .filter(":visible")
                .should("exist")
                .and("contain.text", newName);
            });
        });
    });
  });

  describe("Delete Location", function () {
    it("Should open delete modal and show delete workflow", function () {
      cy.visit("/Storage/rooms");
      cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
      cy.get('[data-testid="tab-rooms"]', { timeout: 10000 })
        .should("be.visible")
        .should("have.attr", "aria-selected", "true");
      cy.get(".cds--tab-content:visible", { timeout: 10000 }).should(
        "be.visible",
      );

      // Wait for table to load
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 })
        .filter(":visible")
        .should("have.length.at.least", 1)
        .first()
        .invoke("attr", "data-testid")
        .then((testId) => {
          const roomId = testId.replace("room-row-", "");

          // Open delete modal
          cy.get(`[data-testid="room-row-${roomId}"]`)
            .filter(":visible")
            .within(() => {
              cy.get('[data-testid="location-actions-overflow-menu"]')
                .should("be.visible")
                .click({ force: true });
            });

          cy.get('[data-testid="delete-location-menu-item"]')
            .should("be.visible")
            .click({ force: true });

          // Verify modal opens (user-visible behavior)
          cy.get('[data-testid="delete-location-modal"]', {
            timeout: 10000,
          }).should("be.visible");

          // Verify modal has cancel button (user-visible behavior)
          cy.get('[data-testid="delete-location-cancel-button"]')
            .should("be.visible")
            .click();

          // Verify modal closes (user-visible behavior)
          cy.get('[data-testid="delete-location-modal"]', {
            timeout: 10000,
          }).should("not.be.visible");
        });
    });
  });
});
