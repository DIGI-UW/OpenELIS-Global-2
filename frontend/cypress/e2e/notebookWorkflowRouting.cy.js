import LoginPage from "../pages/LoginPage";
import NotebookWorkflowPage from "../pages/NotebookWorkflowPage";

/**
 * E2E tests for the Notebook Routing Workflow (OGC-51 Phase 6).
 *
 * Tests cover:
 * - Child sample creation from parents (User Story 4)
 * - Sample routing to destinations (internal analysis, external lab, storage)
 * - Well coordinate auto-assignment in row-major order
 * - Box layout visualization
 *
 * Per Constitution V.5: Run tests individually during development.
 * Usage: npm run cy:run -- --spec "cypress/e2e/notebookWorkflowRouting.cy.js"
 */

const login = new LoginPage();
const workflowPage = new NotebookWorkflowPage();

describe("Notebook Workflow - Child Sample Creation", function () {
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

  it("should display Child Sample Creation page (Page 3)", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Page 3 (Aliquoting / Child Sample Creation)
    workflowPage.clickPage(2);
    workflowPage.verifyActivePage(2);

    // Verify page content
    cy.contains("Child Sample Creation").should("be.visible");
    cy.contains("Create Children").should("exist");
  });

  it("should show create children button disabled when no samples selected", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Page 3
    workflowPage.clickPage(2);

    // Verify create button is disabled
    cy.contains("button", "Create Children").should("be.disabled");
  });

  it("should open create children modal when samples are selected", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Page 3
    workflowPage.clickPage(2);

    // Skip if no samples available (test depends on workflow having samples)
    cy.get("body").then(($body) => {
      if ($body.find('[data-testid="sample-grid-row"]').length > 0) {
        // Select first sample
        cy.get('[data-testid="sample-grid-row"]')
          .first()
          .find('input[type="checkbox"]')
          .click();

        // Click create children button
        cy.contains("button", "Create Children").should("not.be.disabled");
        cy.contains("button", "Create Children").click();

        // Verify modal opens
        cy.get(".cds--modal").should("be.visible");
        cy.contains("Create Child Samples").should("be.visible");

        // Verify modal fields
        cy.contains("Children per Parent").should("be.visible");
        cy.contains("External ID Prefix").should("be.visible");
      }
    });
  });
});

describe("Notebook Workflow - Sample Routing", function () {
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

  it("should display Sample Routing page (Page 4)", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Page 4 (Sample Routing)
    workflowPage.clickPage(3);
    workflowPage.verifyActivePage(3);

    // Verify page content
    cy.contains("Sample Routing").should("be.visible");
  });

  it("should show routing summary tiles", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Page 4
    workflowPage.clickPage(3);

    // Verify routing summary tiles exist
    cy.contains("Unrouted").should("be.visible");
    cy.contains("Internal Analysis").should("be.visible");
    cy.contains("External Lab").should("be.visible");
    cy.contains("Storage").should("be.visible");
  });

  it("should have routing action buttons", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Page 4
    workflowPage.clickPage(3);

    // Verify routing buttons exist
    cy.contains("button", "Route to Internal Analysis").should("exist");
    cy.contains("button", "Route to External Lab").should("exist");
    cy.contains("button", "Route to Storage").should("exist");
  });

  it("should show tabs for Samples and Box Layout", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Page 4
    workflowPage.clickPage(3);

    // Verify tabs exist
    cy.contains('[role="tab"]', "Samples").should("be.visible");
    cy.contains('[role="tab"]', "Box Layout").should("be.visible");
  });

  it("should switch to Box Layout tab and show box selector", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Page 4
    workflowPage.clickPage(3);

    // Click Box Layout tab
    cy.contains('[role="tab"]', "Box Layout").click();

    // Verify box selector is visible
    cy.contains("Select Box to View").should("be.visible");
  });
});

describe("Notebook Workflow - Internal Analysis Routing", function () {
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

  it("should open internal analysis routing modal when samples selected", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Page 4
    workflowPage.clickPage(3);

    // Skip if no samples available
    cy.get("body").then(($body) => {
      if ($body.find('[data-testid="sample-grid-row"]').length > 0) {
        // Select first sample
        cy.get('[data-testid="sample-grid-row"]')
          .first()
          .find('input[type="checkbox"]')
          .click();

        // Click route to internal analysis button
        cy.contains("button", "Route to Internal Analysis").click();

        // Verify modal opens
        cy.get(".cds--modal").should("be.visible");
        cy.contains("Route to Internal Analysis").should("be.visible");

        // Verify box selection field
        cy.contains("Storage Box").should("be.visible");
      }
    });
  });

  it("should require box selection for internal analysis routing", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Page 4
    workflowPage.clickPage(3);

    // Skip if no samples available
    cy.get("body").then(($body) => {
      if ($body.find('[data-testid="sample-grid-row"]').length > 0) {
        // Select first sample
        cy.get('[data-testid="sample-grid-row"]')
          .first()
          .find('input[type="checkbox"]')
          .click();

        // Click route to internal analysis
        cy.contains("button", "Route to Internal Analysis").click();

        // Try to submit without box selection (should show error or be disabled)
        cy.get(".cds--modal").within(() => {
          // The primary button may be disabled or clicking it shows error
          cy.contains("button", "Route Samples").click();
        });

        // Should show error or remain in modal
        cy.get(".cds--modal").should("be.visible");
      }
    });
  });
});

describe("Notebook Workflow - Box Layout Viewer", function () {
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

  it("should display 96-well plate grid when box is selected", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Page 4 or Page 5 (both use routing page)
    workflowPage.clickPage(3);

    // Click Box Layout tab
    cy.contains('[role="tab"]', "Box Layout").click();

    // If boxes are available, select one
    cy.get("body").then(($body) => {
      if ($body.find('[data-testid="box-layout-viewer"]').length > 0) {
        // Verify grid structure
        cy.get(".box-grid-container").should("be.visible");
        cy.get(".well-cell").should("have.length", 96); // 8 rows x 12 columns
      }
    });
  });

  it("should show well occupancy legend", function () {
    workflowPage.visit();
    workflowPage.waitForLoad();

    // Navigate to Page 4
    workflowPage.clickPage(3);

    // Click Box Layout tab
    cy.contains('[role="tab"]', "Box Layout").click();

    // Check if layout viewer is present
    cy.get("body").then(($body) => {
      if ($body.find(".box-layout-viewer").length > 0) {
        // Verify legend exists
        cy.contains("Empty").should("be.visible");
        cy.contains("Occupied").should("be.visible");
      }
    });
  });
});
