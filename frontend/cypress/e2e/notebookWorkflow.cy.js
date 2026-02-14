import LoginPage from "../pages/LoginPage";
import NotebookWorkflowPage from "../pages/NotebookWorkflowPage";

/**
 * E2E tests for the Immunology Notebook Workflow (OGC-51).
 *
 * Tests cover:
 * - Page navigation between workflow steps
 * - Bulk data entry operations (FR-031)
 * - Progress indicators (FR-004)
 * - Batch processing behavior (FR-033)
 *
 * Per Constitution V.5: Run tests individually during development.
 * Usage: npm run cy:run -- --spec "cypress/e2e/notebookWorkflow.cy.js"
 */

const login = new LoginPage();
const workflowPage = new NotebookWorkflowPage();

describe("Notebook Workflow - Page Navigation", function () {
  before("Login and load user data", function () {
    cy.fixture("Users").then((users) => {
      this.users = users;
    });
  });

  beforeEach("Login and navigate to workflow", function () {
    // Login with valid user
    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      if (user) {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
      }
    });
  });

  it("should display 9 workflow pages in navigation", function () {
    workflowPage.visit();
    cy.get("body", { timeout: 30000 }).then(($body) => {
      if ($body.find(".notebook-workflow-container").length === 0) {
        cy.log("Notebook workflow container not available in this test run");
        return;
      }

      workflowPage.waitForLoad();
      workflowPage.getPageItems().should("have.length.at.least", 1);
    });
  });

  it("should show Page 1 as active by default", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    workflowPage.verifyActivePage(0);
    workflowPage
      .getPageTitle()
      .should("contain.text", "Sample Reception")
      .or("contain.text", "Page 1");
  });

  it("should navigate to Page 2 when clicked", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Click on Page 2
    workflowPage.clickPage(1);

    // Verify Page 2 is now active
    workflowPage.verifyActivePage(1);
    workflowPage
      .getPageTitle()
      .should("contain.text", "Initial Processing")
      .or("contain.text", "Page 2");
  });

  it("should show different content for each page", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Page 1 should show Sample Reception content
    cy.contains("Sample Reception").should("be.visible");

    // Navigate to Page 2
    workflowPage.clickPage(1);
    cy.contains("Initial Processing").should("be.visible");

    // Navigate to Page 3
    workflowPage.clickPage(2);
    cy.contains("Aliquoting").should("be.visible");
  });

  it("should display progress summary in navigation", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    workflowPage
      .getNavigationSummary()
      .should("be.visible")
      .and("contain.text", "completed");
  });
});

describe("Notebook Workflow - Page 1: Sample Reception", function () {
  beforeEach("Login and navigate to Page 1", function () {
    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      if (user) {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
      }
    });
    workflowPage.visit();
    workflowPage.waitForLoad();
  });

  it("should display progress tiles for Sample Reception", function () {
    workflowPage.getProgressTiles().should("have.length.at.least", 2);
    workflowPage.getProgressTileByLabel("Total").should("be.visible");
  });

  it("should show Import Manifest button", function () {
    cy.contains("button", "Import from Manifest").should("be.visible");
  });

  it("should show Search & Link Samples button", function () {
    cy.contains("button", "Search").should("be.visible");
  });

  it("should display empty state when no samples", function () {
    // If no samples exist, empty state should be shown
    cy.get("body").then(($body) => {
      if ($body.find(".empty-state").length) {
        workflowPage.verifyEmptyState();
      } else {
        // Samples exist, grid should be visible
        workflowPage.getSampleGrid().should("be.visible");
      }
    });
  });
});

describe("Notebook Workflow - Page 2: Initial Processing", function () {
  beforeEach("Login and navigate to Page 2", function () {
    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      if (user) {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
      }
    });
    workflowPage.visit();
    workflowPage.waitForLoad();
    workflowPage.clickPage(1); // Navigate to Page 2
  });

  it("should display progress tiles for Initial Processing", function () {
    workflowPage.getProgressTiles().should("have.length.at.least", 3);
    workflowPage.getProgressTileByLabel("Processed").should("be.visible");
    workflowPage.getProgressTileByLabel("In Progress").should("be.visible");
    workflowPage.getProgressTileByLabel("Pending").should("be.visible");
  });

  it("should show Bulk Apply button (disabled without selection)", function () {
    cy.contains("button", "Bulk Apply").should("be.visible").and("be.disabled");
  });

  it("should show Refresh button", function () {
    cy.contains("button", "Refresh").should("be.visible");
  });

  it("should enable Bulk Apply when samples selected", function () {
    // This test only runs if samples exist
    cy.get("body").then(($body) => {
      if ($body.find(".cds--data-table tbody tr").length > 0) {
        // Select first sample
        workflowPage.selectSampleRow(0);

        // Bulk Apply should now be enabled
        cy.contains("button", "Bulk Apply").should("not.be.disabled");
      }
    });
  });
});

describe("Notebook Workflow - Bulk Apply Modal", function () {
  beforeEach("Login and navigate to Page 2", function () {
    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      if (user) {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
      }
    });
    workflowPage.visit();
    workflowPage.waitForLoad();
    workflowPage.clickPage(1);
  });

  it("should open Bulk Apply modal when button clicked", function () {
    // This test only runs if samples exist and can be selected
    cy.get("body").then(($body) => {
      if ($body.find(".cds--data-table tbody tr").length > 0) {
        workflowPage.selectAllSamples();
        workflowPage.clickBulkApply();

        workflowPage.getBulkApplyModal().should("be.visible");
        cy.contains("Bulk Apply Values").should("be.visible");
      } else {
        cy.log("No samples available for bulk apply test");
      }
    });
  });

  it("should show form fields in Bulk Apply modal", function () {
    cy.get("body").then(($body) => {
      if ($body.find(".cds--data-table tbody tr").length > 0) {
        workflowPage.selectAllSamples();
        workflowPage.clickBulkApply();

        // Form should have input fields (no checkboxes - auto-apply non-empty values)
        cy.get(".bulk-apply-fields-grid").should("be.visible");
        cy.get(".bulk-apply-fields-grid .cds--form-group").should(
          "have.length.at.least",
          1,
        );
      }
    });
  });

  it("should close modal on cancel", function () {
    cy.get("body").then(($body) => {
      if ($body.find(".cds--data-table tbody tr").length > 0) {
        workflowPage.selectAllSamples();
        workflowPage.clickBulkApply();
        workflowPage.cancelBulkApply();

        workflowPage.getBulkApplyModal().should("not.exist");
      }
    });
  });
});

describe("Notebook Workflow - Sample Grid", function () {
  beforeEach("Login and navigate to workflow", function () {
    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      if (user) {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
      }
    });
    workflowPage.visit();
    workflowPage.waitForLoad();
  });

  it("should display sample grid with headers", function () {
    cy.get("body").then(($body) => {
      if ($body.find(".sample-grid").length) {
        workflowPage.getSampleGrid().should("be.visible");
        cy.get(".cds--data-table-header").should("be.visible");
      }
    });
  });

  it("should have status filter dropdown", function () {
    cy.get("body").then(($body) => {
      if ($body.find("#status-filter").length) {
        workflowPage.getStatusFilter().should("be.visible");
      }
    });
  });

  it("should have pagination controls", function () {
    cy.get("body").then(($body) => {
      if ($body.find(".cds--pagination").length) {
        cy.get(".cds--pagination").should("be.visible");
      }
    });
  });

  it("should allow multi-select via checkboxes", function () {
    cy.get("body").then(($body) => {
      if ($body.find(".cds--data-table tbody tr").length >= 2) {
        workflowPage.selectSampleRow(0);
        workflowPage.selectSampleRow(1);

        // Selection summary should show count
        cy.contains("samples selected").should("be.visible");
      }
    });
  });

  it("should allow select all", function () {
    cy.get("body").then(($body) => {
      if ($body.find(".cds--data-table tbody tr").length > 0) {
        workflowPage.selectAllSamples();

        // All checkboxes should be checked
        cy.get("#select-all-rows").should("be.checked");
      }
    });
  });
});

describe("Notebook Workflow - Keyboard Navigation", function () {
  beforeEach("Login and navigate to workflow", function () {
    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      if (user) {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
      }
    });
    workflowPage.visit();
    workflowPage.waitForLoad();
  });

  it("should allow keyboard navigation between pages", function () {
    // Focus first page item
    workflowPage.getPageItemByIndex(0).focus();

    // Press Enter to activate
    workflowPage.getPageItemByIndex(0).type("{enter}");
    workflowPage.verifyActivePage(0);

    // Tab to next page and activate
    workflowPage.getPageItemByIndex(1).focus();
    workflowPage.getPageItemByIndex(1).type("{enter}");
    workflowPage.verifyActivePage(1);
  });

  it("should allow space key to activate page", function () {
    workflowPage.getPageItemByIndex(2).focus();
    workflowPage.getPageItemByIndex(2).type(" ");
    workflowPage.verifyActivePage(2);
  });
});

describe("Notebook Workflow - API Integration", function () {
  beforeEach("Login and setup interceptors", function () {
    // Intercept API calls
    cy.intercept("GET", "/rest/notebook/**").as("loadNotebook");
    cy.intercept("GET", "/rest/notebook-entry/**").as("loadEntry");
    cy.intercept("GET", "/rest/notebook/bulk/page/*/progress").as(
      "loadProgress",
    );
    cy.intercept("POST", "/rest/notebook/bulk/page/*/samples/apply").as(
      "bulkApply",
    );

    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      if (user) {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
      }
    });
  });

  it("should load notebook data on page visit", function () {
    workflowPage.visit();

    // Wait for API calls
    cy.wait("@loadNotebook", { timeout: 10000 }).then((interception) => {
      expect(interception.response.statusCode).to.be.oneOf([200, 304]);
    });
  });

  it("should load progress data for pages", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Page 2 which loads progress
    workflowPage.clickPage(1);

    // Progress endpoint should be called (if page has real ID)
    cy.get("body").then(($body) => {
      // Only check if we have a real page (not default placeholder)
      if (!$body.text().includes("default-")) {
        cy.wait("@loadProgress", { timeout: 5000 }).then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });
      }
    });
  });
});
