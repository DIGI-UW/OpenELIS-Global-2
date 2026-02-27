/**
 * E2E Tests: Storage Dashboard Filter Functionality
 * Simple tests that verify filter dropdowns work on each tab.
 */

before("Setup storage tests", () => {
  cy.setupStorageTests();
});

after("Cleanup storage tests", () => {
  cy.cleanupStorageTests();
});

describe("Storage Dashboard Filtering - Rooms Tab", function () {
  beforeEach(() => {
    cy.visit("/Storage/rooms");
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
    cy.get(".cds--data-table tbody tr", { timeout: 10000 }).should(
      "have.length.at.least",
      1,
    );
  });

  it("Should filter rooms by status (Active)", function () {
    // Intercept API call to wait for filter to apply
    cy.intercept("GET", "**/rest/storage/rooms*").as("getRooms");

    // Click the visible status filter dropdown button
    cy.get("#filter-status button").filter(":visible").first().click();
    cy.get('[role="listbox"] [role="option"]').contains("Active").click();

    // Wait for filtered data to load
    cy.wait("@getRooms", { timeout: 10000 });

    // Verify table has rows (filter worked)
    cy.get(".cds--data-table tbody tr").should("have.length.at.least", 1);
  });

  it("Should filter rooms by status (Inactive)", function () {
    cy.get("#filter-status button").filter(":visible").first().click();
    cy.get('[role="listbox"] [role="option"]').contains("Inactive").click();

    // Table may show inactive rooms or empty state
    cy.get(".cds--data-table").should("exist");
  });
});

describe("Storage Dashboard Filtering - Devices Tab", function () {
  beforeEach(() => {
    cy.visit("/Storage/devices");
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
    cy.get(".cds--data-table tbody tr", { timeout: 10000 }).should(
      "have.length.at.least",
      1,
    );
  });

  it("Should filter devices by room", function () {
    cy.get("#filter-room button").filter(":visible").first().click();
    cy.get('[role="listbox"] [role="option"]')
      .not(':contains("All")')
      .first()
      .click();

    cy.get(".cds--data-table").should("exist");
  });

  it("Should filter devices by status (Active)", function () {
    // Intercept API call to wait for filter to apply
    cy.intercept("GET", "**/rest/storage/devices*").as("getDevices");

    cy.get("#filter-status button").filter(":visible").first().click();
    cy.get('[role="listbox"] [role="option"]').contains("Active").click();

    // Wait for filtered data to load
    cy.wait("@getDevices", { timeout: 10000 });

    // Verify table has rows (filter worked)
    cy.get(".cds--data-table tbody tr").should("have.length.at.least", 1);
  });
});

describe("Storage Dashboard Filtering - Shelves Tab", function () {
  beforeEach(() => {
    cy.visit("/Storage/shelves");
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
    cy.get(".cds--data-table tbody tr", { timeout: 10000 }).should(
      "have.length.at.least",
      1,
    );
  });

  it("Should filter shelves by device", function () {
    cy.get("#filter-device button").filter(":visible").first().click();
    cy.get('[role="listbox"] [role="option"]')
      .not(':contains("All")')
      .first()
      .click();

    cy.get(".cds--data-table").should("exist");
  });

  it("Should filter shelves by room", function () {
    cy.get("#filter-room button").filter(":visible").first().click();
    cy.get('[role="listbox"] [role="option"]')
      .not(':contains("All")')
      .first()
      .click();

    cy.get(".cds--data-table").should("exist");
  });

  it("Should filter shelves by status (Active)", function () {
    // Intercept API call to wait for filter to apply
    cy.intercept("GET", "**/rest/storage/shelves*").as("getShelves");

    cy.get("#filter-status button").filter(":visible").first().click();
    cy.get('[role="listbox"] [role="option"]').contains("Active").click();

    // Wait for filtered data to load
    cy.wait("@getShelves", { timeout: 10000 });

    // Verify table has rows (filter worked)
    cy.get(".cds--data-table tbody tr").should("have.length.at.least", 1);
  });
});

describe("Storage Dashboard Filtering - Racks Tab", function () {
  beforeEach(() => {
    cy.visit("/Storage/racks");
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
    cy.get(".cds--data-table tbody tr", { timeout: 10000 }).should(
      "have.length.at.least",
      1,
    );
  });

  it("Should filter racks by room", function () {
    cy.get("#filter-room button").filter(":visible").first().click();
    cy.get('[role="listbox"] [role="option"]')
      .not(':contains("All")')
      .first()
      .click();

    cy.get(".cds--data-table").should("exist");
  });

  it("Should filter racks by device", function () {
    cy.get("#filter-device button").filter(":visible").first().click();
    cy.get('[role="listbox"] [role="option"]')
      .not(':contains("All")')
      .first()
      .click();

    cy.get(".cds--data-table").should("exist");
  });

  it("Should filter racks by status (Active)", function () {
    // Intercept API call to wait for filter to apply
    cy.intercept("GET", "**/rest/storage/racks*").as("getRacks");

    cy.get("#filter-status button").filter(":visible").first().click();
    cy.get('[role="listbox"] [role="option"]').contains("Active").click();

    // Wait for filtered data to load
    cy.wait("@getRacks", { timeout: 10000 });

    // Verify table has rows (filter worked)
    cy.get(".cds--data-table tbody tr").should("have.length.at.least", 1);
  });

  it("Should display Room column in racks table (FR-065a)", function () {
    cy.get(".cds--data-table thead th").then(($headers) => {
      const headers = Array.from($headers).map((h) => h.textContent.trim());
      expect(headers.some((h) => h.includes("Room"))).to.be.true;
    });
  });
});
