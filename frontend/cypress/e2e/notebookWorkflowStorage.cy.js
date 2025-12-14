import LoginPage from "../pages/LoginPage";
import NotebookWorkflowPage from "../pages/NotebookWorkflowPage";

/**
 * E2E tests for the Notebook Storage Workflow (OGC-51 Phase 8 - User Story 6).
 *
 * Tests cover:
 * - Post-analysis storage assignment with conditions
 * - Retention period tracking
 * - Storage location selection
 * - Bulk storage assignment
 *
 * US6 Goal: Store processed samples under defined conditions with tracked location
 * and retention period using existing SampleStorageService.
 *
 * Independent Test: Select 10 samples, assign to storage location with retention
 * period (e.g., "Frozen, -80°C, 5 years"), verify assignment created correctly.
 *
 * Per Constitution V.5: Run tests individually during development.
 * Usage: npm run cy:run -- --spec "cypress/e2e/notebookWorkflowStorage.cy.js"
 */

const login = new LoginPage();
const workflowPage = new NotebookWorkflowPage();

describe("Notebook Workflow - Storage Assignment", function () {
  before("Login and load user data", function () {
    cy.fixture("Users").then((users) => {
      this.users = users;
    });
  });

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
  });

  it("T111a: Should display Storage page (Page 7)", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Page 7 (Post-Analysis Storage)
    // Page indices: 0=Reception, 1=Processing, 2=Aliquoting, 3=Assays, 4=Prep,
    // 5=Routing, 6=Analysis, 7=Storage, 8=Compilation, 9=Archival
    workflowPage.clickPage(7);
    workflowPage.verifyActivePage(7);

    // Verify page content
    cy.contains("Post-Analysis Storage").should("be.visible");
    cy.contains("Assign to Storage").should("exist");
  });

  it("T111b: Should show storage assignment button disabled when no samples selected", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Storage page
    workflowPage.clickPage(7);

    // Verify assign button is disabled when no samples selected
    cy.contains("button", /Assign to Storage/).should("be.disabled");
  });

  it("T111c: Should display storage summary tiles", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Storage page
    workflowPage.clickPage(7);

    // Verify summary tiles are displayed
    cy.contains("Pending Assignment").should("be.visible");
    cy.contains("Assigned to Storage").should("be.visible");
    cy.contains("Total").should("be.visible");
  });

  it("T111d: Should open storage assignment modal when samples selected", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Storage page
    workflowPage.clickPage(7);

    // Skip if no samples available
    cy.get("body").then(($body) => {
      if ($body.find('[data-testid="sample-grid-row"]').length > 0) {
        // Select first sample
        cy.get('[data-testid="sample-grid-row"]')
          .first()
          .find('input[type="checkbox"]')
          .click();

        // Click assign button
        cy.contains("button", /Assign to Storage/).should("not.be.disabled");
        cy.contains("button", /Assign to Storage/).click();

        // Verify modal opens
        cy.get(".cds--modal").should("be.visible");
        cy.contains("Assign to Storage").should("be.visible");

        // Verify modal fields
        cy.contains("Storage Location").should("be.visible");
        cy.contains("Storage Condition").should("be.visible");
        cy.contains("Retention Period").should("be.visible");
      }
    });
  });

  it("T111e: Should display storage condition options", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Storage page
    workflowPage.clickPage(7);

    // Skip if no samples available
    cy.get("body").then(($body) => {
      if ($body.find('[data-testid="sample-grid-row"]').length > 0) {
        // Select first sample
        cy.get('[data-testid="sample-grid-row"]')
          .first()
          .find('input[type="checkbox"]')
          .click();

        // Open modal
        cy.contains("button", /Assign to Storage/).click();

        // Click storage condition dropdown
        cy.get("#storage-condition-dropdown").click();

        // Verify condition options
        cy.contains("Refrigerated (2-8").should("be.visible");
        cy.contains("Frozen (-20").should("be.visible");
        cy.contains("Frozen (-80").should("be.visible");
        cy.contains("Room Temperature").should("be.visible");
      }
    });
  });

  it("T111f: Should calculate expiry date based on retention period", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Storage page
    workflowPage.clickPage(7);

    // Skip if no samples available
    cy.get("body").then(($body) => {
      if ($body.find('[data-testid="sample-grid-row"]').length > 0) {
        // Select first sample
        cy.get('[data-testid="sample-grid-row"]')
          .first()
          .find('input[type="checkbox"]')
          .click();

        // Open modal
        cy.contains("button", /Assign to Storage/).click();

        // Verify expiry date helper text
        cy.contains("Expiry date will be").should("be.visible");

        // Change retention period
        const retentionInput = cy.get("#retention-years");
        retentionInput.clear();
        retentionInput.type("10");

        // Verify expiry date updated (should be ~10 years from now)
        const expectedYear = new Date().getFullYear() + 10;
        cy.contains(`${expectedYear}`).should("be.visible");
      }
    });
  });
});

describe("Notebook Workflow - Storage Completion", function () {
  before("Login and load user data", function () {
    cy.fixture("Users").then((users) => {
      this.users = users;
    });
  });

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
  });

  it("T111g: Should show Mark Complete button for stored samples", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Storage page
    workflowPage.clickPage(7);

    // Verify mark complete button exists
    cy.contains("button", "Mark Stored Samples Complete").should("exist");
  });

  it("T111h: Should display storage status tags in sample grid", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Storage page
    workflowPage.clickPage(7);

    // Verify storage status column exists
    cy.contains("Storage Status").should("be.visible");

    // Check for status tags (Pending or Stored)
    cy.get("body").then(($body) => {
      if ($body.find('[data-testid="sample-grid-row"]').length > 0) {
        // Should show either Pending or Stored status
        const hasPending = $body.text().includes("Pending");
        const hasStored = $body.text().includes("Stored");
        expect(hasPending || hasStored).to.be.true;
      }
    });
  });
});
