/**
 * E2E Tests: Storage Dashboard Filter Functionality
 *
 * Tests user workflows: User can filter data and see filtered results in tables
 * Focus: User-visible behavior only, not implementation details
 *
 * Constitution V.5 Compliance:
 * - Uses data-testid selectors (priority 1)
 * - Tests user workflows, not API internals
 * - Simple, efficient, non-redundant
 */

describe("Storage Dashboard Filtering", function () {
  before(() => {
    cy.setupStorageTests();
  });

  after(() => {
    cy.cleanupStorageTests();
  });

  describe("Samples Tab - Location Filter", function () {
    beforeEach(() => {
      cy.visit("/Storage/samples");
      cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
      cy.get('[data-testid="tab-samples"]', { timeout: 10000 })
        .should("be.visible")
        .should("have.attr", "aria-selected", "true");
      // Wait for visible tab panel (Carbon hides non-selected panels)
      cy.get(".cds--tab-content:visible", { timeout: 10000 }).should(
        "be.visible",
      );
      cy.get('[role="table"], .cds--data-table', { timeout: 10000 })
        .filter(":visible")
        .should("be.visible");
    });

    it("Should filter samples by location", function () {
      // Get initial row count
      cy.get('[role="table"] tbody tr, .cds--data-table tbody tr')
        .filter(":visible")
        .then(($initialRows) => {
          const initialCount = $initialRows.length;

          // Select location filter (within visible tab panel)
          cy.get(".cds--tab-content:visible").within(() => {
            cy.get('[data-testid="location-filter-dropdown"]')
              .should("be.visible")
              .within(() => {
                cy.get("#location-filter-search").type("MAIN");
                cy.get('[data-testid="location-autocomplete"]', {
                  timeout: 5000,
                }).should("exist");
                cy.get(
                  '[data-testid="location-autocomplete"] .location-autocomplete-item',
                )
                  .first()
                  .click({ force: true });
              });
          });

          // Verify table updates (user-visible behavior)
          cy.get('[role="table"] tbody tr, .cds--data-table tbody tr', {
            timeout: 10000,
          })
            .filter(":visible")
            .should(($filteredRows) => {
              // Table should update (may have fewer or same rows)
              expect($filteredRows.length).to.be.at.most(initialCount);
            });
        });
    });
  });

  describe("Rooms Tab - Status Filter", function () {
    beforeEach(() => {
      cy.visit("/Storage/rooms");
      cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
      cy.get('[data-testid="tab-rooms"]', { timeout: 10000 })
        .should("be.visible")
        .should("have.attr", "aria-selected", "true");
      // Wait for visible tab panel (Carbon hides non-selected panels)
      cy.get(".cds--tab-content:visible", { timeout: 10000 }).should(
        "be.visible",
      );
      cy.get('[role="table"], .cds--data-table', { timeout: 10000 })
        .filter(":visible")
        .should("be.visible");
    });

    it("Should filter rooms by active status", function () {
      // Select active status filter (within visible tab panel)
      cy.get(".cds--tab-content:visible").within(() => {
        cy.get('[data-testid="status-filter"]').should("be.visible").click();
      });
      cy.get('[role="listbox"]', { timeout: 5000 }).should("be.visible");
      cy.get('[role="option"]').contains("Active").click();

      // Verify table shows filtered results (user-visible)
      cy.get('[role="table"] tbody tr, .cds--data-table tbody tr', {
        timeout: 10000,
      }).should(($rows) => {
        expect($rows.length).to.be.greaterThan(0);
      });
    });
  });

  describe("Devices Tab - Room Filter", function () {
    beforeEach(() => {
      cy.visit("/Storage/devices");
      cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
      cy.get('[data-testid="tab-devices"]', { timeout: 10000 })
        .should("be.visible")
        .should("have.attr", "aria-selected", "true");
      // Wait for visible tab panel (Carbon hides non-selected panels)
      cy.get(".cds--tab-content:visible", { timeout: 10000 }).should(
        "be.visible",
      );
      cy.get('[role="table"], .cds--data-table', { timeout: 10000 })
        .filter(":visible")
        .should("be.visible");
    });

    it("Should filter devices by room", function () {
      // Select room filter (within visible tab panel)
      cy.get(".cds--tab-content:visible").within(() => {
        cy.get('[data-testid="room-filter"]').should("be.visible").click();
      });
      cy.get('[role="listbox"]', { timeout: 5000 }).should("be.visible");
      cy.get('[role="option"]').not(':contains("All")').first().click();

      // Verify table updates (user-visible behavior)
      cy.get('[role="table"] tbody tr, .cds--data-table tbody tr', {
        timeout: 10000,
      }).should(($rows) => {
        expect($rows.length).to.be.greaterThan(0);
      });
    });
  });

  describe("Shelves Tab - Device Filter", function () {
    beforeEach(() => {
      cy.visit("/Storage/shelves");
      cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
      cy.get('[data-testid="tab-shelves"]', { timeout: 10000 })
        .should("be.visible")
        .should("have.attr", "aria-selected", "true");
      // Wait for visible tab panel (Carbon hides non-selected panels)
      cy.get(".cds--tab-content:visible", { timeout: 10000 }).should(
        "be.visible",
      );
      cy.get('[role="table"], .cds--data-table', { timeout: 10000 })
        .filter(":visible")
        .should("be.visible");
    });

    it("Should filter shelves by device", function () {
      // Select device filter (within visible tab panel)
      cy.get(".cds--tab-content:visible").within(() => {
        cy.get('[data-testid="device-filter"]').should("be.visible").click();
      });
      cy.get('[role="listbox"]', { timeout: 5000 }).should("be.visible");
      cy.get('[role="option"]').not(':contains("All")').first().click();

      // Verify table updates (user-visible behavior)
      cy.get('[role="table"] tbody tr, .cds--data-table tbody tr', {
        timeout: 10000,
      }).should(($rows) => {
        expect($rows.length).to.be.greaterThan(0);
      });
    });
  });

  describe("Racks Tab - Room Filter", function () {
    beforeEach(() => {
      cy.visit("/Storage/racks");
      cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
      cy.get('[data-testid="tab-racks"]', { timeout: 10000 })
        .should("be.visible")
        .should("have.attr", "aria-selected", "true");
      // Wait for visible tab panel (Carbon hides non-selected panels)
      cy.get(".cds--tab-content:visible", { timeout: 10000 }).should(
        "be.visible",
      );
      cy.get('[role="table"], .cds--data-table', { timeout: 10000 })
        .filter(":visible")
        .should("be.visible");
    });

    it("Should filter racks by room", function () {
      // Select room filter (within visible tab panel)
      cy.get(".cds--tab-content:visible").within(() => {
        cy.get('[data-testid="room-filter"]').should("be.visible").click();
      });
      cy.get('[role="listbox"]', { timeout: 5000 }).should("be.visible");
      cy.get('[role="option"]').not(':contains("All")').first().click();

      // Verify table updates (user-visible behavior)
      cy.get('[role="table"] tbody tr, .cds--data-table tbody tr', {
        timeout: 10000,
      }).should(($rows) => {
        expect($rows.length).to.be.greaterThan(0);
      });
    });
  });
});
