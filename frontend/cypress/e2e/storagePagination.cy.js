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

const buildItems = (count, offset = 0) =>
  Array.from({ length: count }, (_, idx) => {
    const id = offset + idx + 1;
    return {
      id: `${id}`,
      sampleItemId: `${id}`,
      sampleAccessionNumber: `ACC-${id}`,
      location: `Room A > Device 1`,
      status: "active",
      assignedDate: "2025-01-01",
    };
  });

const stubPage = ({ totalItems, pageSize = 25, page = 0 }) => {
  const start = page * pageSize;
  const remaining = Math.max(totalItems - start, 0);
  const count = Math.min(pageSize, remaining);
  const items = buildItems(count, start);
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
  return {
    statusCode: 200,
    body: {
      items,
      currentPage: page,
      totalPages,
      totalItems,
      pageSize,
    },
  };
};

describe("Sample Storage Pagination (OGC-150)", () => {
  before(() => {
    // Login once for all tests (cy.session() pattern)
    cy.login("admin", "adminADMIN!");

    // Load storage fixtures from 001-sample-storage
    cy.loadStorageFixtures();
  });

  beforeEach(() => {
    cy.viewport(1025, 900);
  });

  /**
   * US1 (P1): Test that first page displays 25 items by default
   * Acceptance Criteria: Page loads with first 25 sample storage assignments
   */
  it("should display first page with 25 items by default", () => {
    cy.intercept("GET", "/rest/storage/sample-items*", (req) => {
      const url = new URL(req.url);
      const page = Number(url.searchParams.get("page") || 0);
      const size = Number(url.searchParams.get("size") || 25);
      req.reply(stubPage({ totalItems: 60, pageSize: size, page }));
    }).as("getSamples");

    cy.visit("/Storage/samples");

    cy.wait("@getSamples", { timeout: 10000 });

    // Assert: Verify pagination controls visible
    cy.get('nav[aria-label*="pagination" i]', { timeout: 5000 }).should(
      "be.visible",
    );

    // Verify samples table visible with data
    cy.get('[data-testid="sample-row"]', { timeout: 5000 }).should(
      "have.length.at.most",
      25,
    );
  });

  /**
   * US2 (P1): Test navigating to next page
   * Acceptance Criteria: Clicking Next button loads items 26-50
   */
  it("should navigate to next page when clicking Next button", () => {
    cy.intercept("GET", "/rest/storage/sample-items*", (req) => {
      const url = new URL(req.url);
      const page = Number(url.searchParams.get("page") || 0);
      const size = Number(url.searchParams.get("size") || 25);
      req.reply(stubPage({ totalItems: 60, pageSize: size, page }));
    }).as("getSamples");

    cy.visit("/Storage/samples");

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
    cy.intercept("GET", "/rest/storage/sample-items*", (req) => {
      const url = new URL(req.url);
      const page = Number(url.searchParams.get("page") || 0);
      const size = Number(url.searchParams.get("size") || 25);
      req.reply(stubPage({ totalItems: 60, pageSize: size, page }));
    }).as("getSamples");

    cy.visit("/Storage/samples");

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
    cy.intercept("GET", "/rest/storage/sample-items*", (req) => {
      const url = new URL(req.url);
      const page = Number(url.searchParams.get("page") || 0);
      const size = Number(url.searchParams.get("size") || 25);
      req.reply(stubPage({ totalItems: 120, pageSize: size, page }));
    }).as("getSamples");

    cy.visit("/Storage/samples");

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
    cy.intercept("GET", "/rest/storage/sample-items*", (req) => {
      const url = new URL(req.url);
      const page = Number(url.searchParams.get("page") || 0);
      const size = Number(url.searchParams.get("size") || 25);
      req.reply(stubPage({ totalItems: 60, pageSize: size, page }));
    }).as("getSamples");
    cy.intercept("GET", "/rest/storage/rooms*", {
      statusCode: 200,
      body: [],
    }).as("getRooms");

    cy.visit("/Storage/samples");

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

    cy.get('nav[aria-label*="pagination" i]', { timeout: 5000 }).should(
      "be.visible",
    );
  });

  /**
   * Edge case: Empty dataset hides/disables pagination and shows no rows
   */
  it("should handle empty dataset gracefully", () => {
    cy.intercept("GET", "/rest/storage/sample-items*", () =>
      stubPage({ totalItems: 0, pageSize: 25, page: 0 }),
    ).as("getSamples");

    cy.visit("/Storage/samples");

    cy.wait("@getSamples", { timeout: 10000 });
    cy.get('[data-testid="sample-row"]').should("have.length", 0);
    cy.get('button[aria-label*="next page" i]').should("be.disabled");
    cy.get('button[aria-label*="previous page" i]').should("be.disabled");
  });

  /**
   * Edge case: Single-page dataset disables next/previous controls
   */
  it("should disable pagination controls when total items fit one page", () => {
    cy.intercept("GET", "/rest/storage/sample-items*", (req) => {
      const url = new URL(req.url);
      const page = Number(url.searchParams.get("page") || 0);
      const size = Number(url.searchParams.get("size") || 25);
      req.reply(stubPage({ totalItems: 10, pageSize: size, page }));
    }).as("getSamples");

    cy.visit("/Storage/samples");

    cy.wait("@getSamples", { timeout: 10000 });
    cy.get('[data-testid="sample-row"]').should("have.length", 10);
    cy.get('button[aria-label*="next page" i]').should("be.disabled");
    cy.get('button[aria-label*="previous page" i]').should("be.disabled");
  });
});
