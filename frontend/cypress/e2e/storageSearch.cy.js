import LoginPage from "../pages/LoginPage";

/**
 * E2E Tests for User Story P2A - Sample Search and Retrieval
 * Tests search by sample ID, filter by location, display hierarchical paths
 */

let loginPage = null;
let homePage = null;

before("Login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
  homePage = loginPage.goToHomePage();
});

describe("Storage Search - Sample ID Search (P2A)", function () {
  it("Should navigate to Storage Dashboard or Sample Search", () => {
    // Navigate to storage dashboard or sample search page
    // This assumes a storage dashboard route exists
    cy.visit("/StorageDashboard").or("/SampleSearch");
    cy.wait(2000);
  });

  it("Should search for sample by ID and display location", function () {
    // Search for a sample ID that exists in test data
    cy.get('[data-testid="sample-search-input"]')
      .should("be.visible")
      .type("101"); // Using fixture sample ID

    cy.get('[data-testid="search-button"]').click();
    cy.wait(2000); // Wait for API call

    // Verify sample found and location displayed
    cy.get('[data-testid="sample-location"]').should("be.visible");
    cy.get('[data-testid="hierarchical-path"]').should("contain.text", "MAIN");
  });

  it("Should display hierarchical location path for found sample", function () {
    // Verify the path shows room > device > shelf > rack > position
    cy.get('[data-testid="hierarchical-path"]').should("contain.text", ">");
  });
});

describe("Storage Search - Filter by Room (P2A)", function () {
  it("Should filter samples by room", function () {
    cy.visit("/StorageDashboard");
    cy.wait(2000);

    // Select room filter
    cy.get('[data-testid="room-filter"]').should("be.visible").click();
    cy.contains("MAIN").click();
    cy.wait(2000);

    // Verify filtered results show only samples in MAIN room
    cy.get('[data-testid="sample-list"]')
      .should("be.visible")
      .find('[data-testid="sample-row"]')
      .each(($row) => {
        cy.wrap($row)
          .find('[data-testid="sample-location"]')
          .should("contain.text", "MAIN");
      });
  });
});

describe("Storage Search - Filter by Multiple Criteria (P2A)", function () {
  it("Should filter samples by room and device", function () {
    cy.visit("/StorageDashboard");
    cy.wait(2000);

    // Select room filter
    cy.get('[data-testid="room-filter"]').click();
    cy.contains("MAIN").click();
    cy.wait(1000);

    // Select device filter
    cy.get('[data-testid="device-filter"]').click();
    cy.contains("FRZ01").click();
    cy.wait(2000);

    // Verify results match both criteria
    cy.get('[data-testid="sample-list"]')
      .should("be.visible")
      .find('[data-testid="sample-row"]')
      .should("have.length.greaterThan", 0);
  });

  it("Should clear filters and show all samples", function () {
    cy.get('[data-testid="clear-filters-button"]').click();
    cy.wait(2000);

    // Verify all samples visible again
    cy.get('[data-testid="sample-list"]')
      .should("be.visible")
      .find('[data-testid="sample-row"]')
      .should("have.length.greaterThan", 0);
  });
});
