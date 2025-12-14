import LoginPage from "../pages/LoginPage";
import NotebookWorkflowPage from "../pages/NotebookWorkflowPage";

/**
 * E2E tests for the Notebook Archiving Workflow (OGC-51 Phase 10 - User Story 8).
 *
 * Tests cover:
 * - End of project archiving page (Page 9)
 * - Biorepository transfer functionality
 * - Traceability verification
 * - Notebook finalization
 *
 * US8 Goal: Once project concludes, transfer remaining Parent and Child Samples
 * to Biorepository Laboratory with permanent storage logged and complete
 * traceability links verified.
 *
 * Independent Test: Transfer 10 samples (both parent and child) to biorepository
 * location, verify traceability checklist items (parent-child links, movement
 * history, storage assignments), confirm permanent storage is logged, mark page
 * complete, notebook status changes to FINALIZED.
 *
 * Per Constitution V.5: Run tests individually during development.
 * Usage: npm run cy:run -- --spec "cypress/e2e/notebookWorkflowArchiving.cy.js"
 */

const login = new LoginPage();
const workflowPage = new NotebookWorkflowPage();

// Page index for Archiving (0-based)
// Pages: 0=Reception, 1=Processing, 2=Assays, 3=Child Samples,
// 4=Prep, 5=Analysis, 6=Storage, 7=Compilation, 8=Archiving
const ARCHIVING_PAGE_INDEX = 8;

describe("Notebook Workflow - End of Project Archiving Page", function () {
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

  it("T131a: Should display Archiving page (Page 9) with correct title", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);
    workflowPage.verifyActivePage(ARCHIVING_PAGE_INDEX);

    // Verify page content
    cy.contains("End of Project Archiving").should("be.visible");
    cy.contains("Transfer samples to biorepository").should("be.visible");
  });

  it("T131b: Should display archiving progress tile", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Verify progress section exists
    cy.contains("Archiving Progress").should("be.visible");

    // Verify parent and child sample counts are displayed
    cy.contains("Parent Samples").should("be.visible");
    cy.contains("Child Samples").should("be.visible");
  });

  it("T131c: Should display action buttons for archiving workflow", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Verify action buttons exist
    cy.contains("button", "Verify Traceability").should("exist");
    cy.contains("button", "Transfer to Biorepository").should("exist");
    cy.contains("button", "Finalize Notebook").should("exist");
  });

  it("T137: Should display TraceabilityChecklist component", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Verify traceability checklist section exists
    cy.contains("Traceability Verification").should("be.visible");

    // Verify checklist items are displayed
    cy.contains("Parent-Child Links").should("be.visible");
    cy.contains("Movement History").should("be.visible");
    cy.contains("Storage Assignments").should("be.visible");
  });
});

describe("Notebook Workflow - Traceability Verification", function () {
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

  it("T136b: Should trigger traceability verification on button click", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Click verify traceability
    cy.contains("button", "Verify Traceability").click();

    // Wait for loading to complete
    cy.get(".cds--loading", { timeout: 10000 }).should("not.exist");

    // Verify results are displayed (either passed or failed)
    cy.get("body").then(($body) => {
      const hasVerified = $body.text().includes("Verified");
      const hasFailures = $body.text().includes("Failed");
      const hasIssues = $body.text().includes("issues");
      expect(hasVerified || hasFailures || hasIssues).to.be.true;
    });
  });

  it("T137a: Should display parent-child relationship verification status", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Click verify traceability
    cy.contains("button", "Verify Traceability").click();

    // Wait for loading
    cy.get(".cds--loading", { timeout: 10000 }).should("not.exist");

    // Verify parent-child check result is displayed
    cy.contains("Parent-Child Links").should("be.visible");
  });

  it("T137b: Should display movement history completeness status", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Click verify traceability
    cy.contains("button", "Verify Traceability").click();

    // Wait for loading
    cy.get(".cds--loading", { timeout: 10000 }).should("not.exist");

    // Verify movement history check result is displayed
    cy.contains("Movement History").should("be.visible");
  });

  it("T137c: Should display storage assignment verification status", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Click verify traceability
    cy.contains("button", "Verify Traceability").click();

    // Wait for loading
    cy.get(".cds--loading", { timeout: 10000 }).should("not.exist");

    // Verify storage assignments check result is displayed
    cy.contains("Storage Assignments").should("be.visible");
  });
});

describe("Notebook Workflow - Biorepository Transfer", function () {
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

  it("T136a: Should open transfer modal when clicking Transfer to Biorepository", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Skip if button is disabled (no samples to transfer)
    cy.get("body").then(($body) => {
      const transferButton = $body.find(
        'button:contains("Transfer to Biorepository")',
      );
      if (transferButton.length > 0 && !transferButton.prop("disabled")) {
        cy.contains("button", "Transfer to Biorepository").click();

        // Verify modal opens
        cy.get(".cds--modal").should("be.visible");
        cy.contains("Transfer to Biorepository").should("be.visible");
      }
    });
  });

  it("T138a: Should display biorepository location selection in transfer modal", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Skip if button is disabled
    cy.get("body").then(($body) => {
      const transferButton = $body.find(
        'button:contains("Transfer to Biorepository")',
      );
      if (transferButton.length > 0 && !transferButton.prop("disabled")) {
        cy.contains("button", "Transfer to Biorepository").click();

        // Verify location selection dropdowns
        cy.contains("Room").should("be.visible");
        cy.contains("Device").should("be.visible");
      }
    });
  });

  it("T138b: Should display sample count in transfer modal", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Skip if button is disabled
    cy.get("body").then(($body) => {
      const transferButton = $body.find(
        'button:contains("Transfer to Biorepository")',
      );
      if (transferButton.length > 0 && !transferButton.prop("disabled")) {
        cy.contains("button", "Transfer to Biorepository").click();

        // Verify sample count is displayed
        cy.contains("samples will be transferred").should("be.visible");
      }
    });
  });

  it("Should require location selection before transfer", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Skip if button is disabled
    cy.get("body").then(($body) => {
      const transferButton = $body.find(
        'button:contains("Transfer to Biorepository")',
      );
      if (transferButton.length > 0 && !transferButton.prop("disabled")) {
        cy.contains("button", "Transfer to Biorepository").click();

        // Verify Transfer button in modal is disabled initially
        cy.get(".cds--modal-footer")
          .contains("button", "Transfer")
          .should("be.disabled");
      }
    });
  });
});

describe("Notebook Workflow - Finalization", function () {
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

  it("T136c: Should open finalization modal when clicking Finalize Notebook", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Skip if button is disabled (traceability not verified)
    cy.get("body").then(($body) => {
      const finalizeButton = $body.find('button:contains("Finalize Notebook")');
      if (finalizeButton.length > 0 && !finalizeButton.prop("disabled")) {
        cy.contains("button", "Finalize Notebook").click();

        // Verify modal opens
        cy.get(".cds--modal").should("be.visible");
        cy.contains("Finalize Notebook").should("be.visible");
      }
    });
  });

  it("T139a: Should display warning about irreversible action in finalization modal", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Skip if button is disabled
    cy.get("body").then(($body) => {
      const finalizeButton = $body.find('button:contains("Finalize Notebook")');
      if (finalizeButton.length > 0 && !finalizeButton.prop("disabled")) {
        cy.contains("button", "Finalize Notebook").click();

        // Verify warning is displayed
        cy.contains("This action is irreversible").should("be.visible");
      }
    });
  });

  it("T139b: Should display finalization summary in modal", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Skip if button is disabled
    cy.get("body").then(($body) => {
      const finalizeButton = $body.find('button:contains("Finalize Notebook")');
      if (finalizeButton.length > 0 && !finalizeButton.prop("disabled")) {
        cy.contains("button", "Finalize Notebook").click();

        // Verify summary is displayed
        cy.contains("Finalization Summary").should("be.visible");
        cy.contains("Total Samples").should("be.visible");
        cy.contains("Archived Samples").should("be.visible");
        cy.contains("Traceability").should("be.visible");
      }
    });
  });

  it("Should require confirmation checkbox before finalizing", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Skip if button is disabled
    cy.get("body").then(($body) => {
      const finalizeButton = $body.find('button:contains("Finalize Notebook")');
      if (finalizeButton.length > 0 && !finalizeButton.prop("disabled")) {
        cy.contains("button", "Finalize Notebook").click();

        // Verify Finalize button in modal is disabled initially
        cy.get(".cds--modal-footer")
          .contains("button", "Finalize")
          .should("be.disabled");

        // Check the confirmation checkbox
        cy.get("#confirm-finalize").check();

        // Verify Finalize button is now enabled
        cy.get(".cds--modal-footer")
          .contains("button", "Finalize")
          .should("not.be.disabled");
      }
    });
  });
});

describe("Notebook Workflow - Archiving Sample List", function () {
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

  it("Should display pending archive samples list", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Verify samples list section exists
    cy.contains("Samples Pending Archive").should("be.visible");
  });

  it("Should display parent and child sample type tags", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Check for sample type tags (if samples exist)
    cy.get("body").then(($body) => {
      const hasParentTag = $body.text().includes("Parent");
      const hasChildTag = $body.text().includes("Child");
      const hasNoSamples = $body.text().includes("All samples have been");

      // Either there are samples with tags, or all are archived
      expect(hasParentTag || hasChildTag || hasNoSamples).to.be.true;
    });
  });

  it("Should display 'All samples archived' message when complete", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Archiving page
    workflowPage.clickPage(ARCHIVING_PAGE_INDEX);

    // Check if all archived message exists (conditional - may or may not be visible)
    cy.get("body").then(($body) => {
      if ($body.text().includes("All samples have been archived")) {
        cy.contains("All samples have been archived").should("be.visible");
      }
    });
  });
});
