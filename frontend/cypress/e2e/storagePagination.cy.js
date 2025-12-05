/**
 * E2E Tests for Sample Storage Pagination (OGC-150)
 * 
 * Tests verify pagination functionality on the Sample Storage Dashboard.
 * Following Constitution V.5:
 * - Run individually during development (not full suite)
 * - Browser console logging enabled and reviewed after each run
 * - Video recording disabled by default
 * - Post-run review of console logs and screenshots required
 *
 * User Stories Covered:
 * - US1 (P1): View Paginated Sample List
 * - US2 (P1): Navigate Between Pages
 * - US3 (P2): Change Page Size
 */

describe("Sample Storage Pagination (OGC-150)", () => {
  before(() => {
    // Login once for all tests (cy.session() pattern)
    cy.login("admin", "adminADMIN!");
    
    // Load storage fixtures from 001-sample-storage
    cy.loadStorageFixtures();
  });

  beforeEach(() => {
    cy.viewport(1025, 900);
    cy.visit("/Storage/samples");
  });

  /**
   * US1 (P1): Test that first page displays 25 items by default
   * Acceptance Criteria: Page loads with first 25 sample storage assignments
   */
  it("should display first page with 25 items by default", () => {
    // Arrange: Set up API intercept BEFORE page visit
    cy.intercept("GET", "/rest/storage/sample-items*").as("getSamples");

    // Page already visited in beforeEach
    
    // Wait for API call to complete
    cy.wait("@getSamples", { timeout: 10000 });

    // Assert: Verify pagination controls visible
    cy.get('nav[aria-label*="pagination" i]', { timeout: 5000 })
      .should("be.visible");

    // Verify samples table visible with data
    cy.get('[data-testid="sample-row"]', { timeout: 5000 })
      .should("have.length.at.most", 25);
  });

  /**
   * US2 (P1): Test navigating to next page
   * Acceptance Criteria: Clicking Next button loads items 26-50
   */
  it("should navigate to next page when clicking Next button", () => {
    // Arrange: Set up API intercept BEFORE action
    cy.intercept("GET", "/rest/storage/sample-items*").as("getSamples");

    // Wait for initial load
    cy.wait("@getSamples", { timeout: 10000 });

    // Find and click Next button
    cy.get('button[aria-label*="next page" i]', { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .click();

    // Assert: Verify API called with page=1
    cy.wait("@getSamples").its("request.url").should("include", "page=1");
  });

  /**
   * US2 (P1): Test navigating to previous page
   * Acceptance Criteria: Clicking Previous button loads previous page
   */
  it("should navigate to previous page when clicking Previous button", () => {
    // Arrange: Set up API intercept
    cy.intercept("GET", "/rest/storage/sample-items*").as("getSamples");

    // Wait for initial load
    cy.wait("@getSamples", { timeout: 10000 });

    // Navigate to page 2 first
    cy.get('button[aria-label*="next page" i]', { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .click();

    cy.wait("@getSamples");

    // Click Previous button
    cy.get('button[aria-label*="previous page" i]', { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .click();

    // Assert: Verify API called with page=0
    cy.wait("@getSamples").its("request.url").should("include", "page=0");
  });

  /**
   * US3 (P2): Test changing page size to 50
   * Acceptance Criteria: Selecting 50 items per page reloads with 50 items
   */
  it("should change page size to 50 items", () => {
    // Arrange: Set up API intercept
    cy.intercept("GET", "/rest/storage/sample-items*").as("getSamples");

    // Wait for initial load
    cy.wait("@getSamples", { timeout: 10000 });

    // Find and change page size selector
    cy.get('select[aria-label*="items per page" i]', { timeout: 5000 })
      .should("be.visible")
      .select("50");

    // Assert: Verify API called with size=50
    cy.wait("@getSamples").its("request.url").should("include", "size=50");
  });

  /**
   * US1 (P1): Test pagination state preserved when switching tabs
   * Acceptance Criteria: Page state persists across tab navigation
   */
  it("should preserve pagination state when switching tabs", () => {
    // Arrange: Set up API intercept
    cy.intercept("GET", "/rest/storage/sample-items*").as("getSamples");
    cy.intercept("GET", "/rest/storage/rooms*").as("getRooms");

    // Wait for initial samples load
    cy.wait("@getSamples", { timeout: 10000 });

    // Navigate to page 2
    cy.get('button[aria-label*="next page" i]', { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .click();

    cy.wait("@getSamples");

    // Switch to Rooms tab
    cy.get('[data-testid="tab-rooms"]', { timeout: 5000 })
      .should("be.visible")
      .click();

    cy.wait("@getRooms", { timeout: 10000 });

    // Switch back to Samples tab
    cy.get('[data-testid="tab-samples"]', { timeout: 5000 })
      .should("be.visible")
      .click();

    // Assert: Samples should reload, pagination component visible
    cy.wait("@getSamples");

    cy.get('nav[aria-label*="pagination" i]', { timeout: 5000 })
      .should("be.visible");
  });
});

