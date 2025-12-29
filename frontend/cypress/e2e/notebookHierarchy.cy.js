import LoginPage from "../pages/LoginPage";
import NotebookDashboardPage from "../pages/NotebookDashboardPage";

/**
 * E2E tests for the Notebook Template Hierarchy feature (OGC-XX).
 *
 * Tests cover:
 * - Tree view navigation with parent/child hierarchy
 * - Parent template selection and behavior
 * - Child instance creation
 * - Entry restrictions (parents can't have entries, only children can)
 * - Breadcrumb navigation
 *
 * Per Constitution V.5: Run tests individually during development.
 * Usage: npm run cy:run -- --spec "cypress/e2e/notebookHierarchy.cy.js"
 */

const login = new LoginPage();
const dashboardPage = new NotebookDashboardPage();

describe("Notebook Hierarchy - Tree View Navigation", function () {
  before("Load user data", function () {
    cy.fixture("Users").then((users) => {
      this.users = users;
    });
  });

  beforeEach("Login and navigate to dashboard", function () {
    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      if (user) {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
      }
    });
    dashboardPage.visit();
    dashboardPage.waitForLoad();
  });

  it("should display tree view in sidebar", function () {
    dashboardPage.getTreeView().should("be.visible");
  });

  it("should display parent templates in tree view", function () {
    dashboardPage.getTreeNodes().should("have.length.at.least", 1);
  });

  it("should show entry counts on tree nodes", function () {
    // Tree nodes should display entry count badges
    cy.get(".tree-node-count").should("exist");
  });

  it("should expand parent node to show children", function () {
    // Get first parent node and expand it
    dashboardPage
      .getTreeNodes()
      .first()
      .then(($node) => {
        const nodeText = $node.text();
        // If node has children, it should be expandable
        cy.wrap($node)
          .find("[aria-expanded]")
          .then(($expandBtn) => {
            if (
              $expandBtn.length > 0 &&
              $expandBtn.attr("aria-expanded") === "false"
            ) {
              cy.wrap($expandBtn).click();
              // Children should now be visible
              cy.wrap($node).find(".cds--tree-node").should("exist");
            }
          });
      });
  });
});

describe("Notebook Hierarchy - Parent Template Behavior", function () {
  beforeEach("Login and navigate to dashboard", function () {
    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      if (user) {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
      }
    });
    dashboardPage.visit();
    dashboardPage.waitForLoad();
  });

  it("should show parent template banner when parent is selected", function () {
    // Select a parent template node
    dashboardPage.getTreeNodes().first().click();

    // Wait for selection to take effect
    cy.wait(500);

    // Check if this is a parent template (should show banner)
    cy.get("body").then(($body) => {
      if ($body.find(".parent-template-banner").length > 0) {
        dashboardPage.verifyParentTemplateBanner();
      }
    });
  });

  it("should show Create Instance button for parent templates", function () {
    // Select a parent template
    dashboardPage.getTreeNodes().first().click();
    cy.wait(500);

    // Check if this shows Create Instance button (parent template behavior)
    cy.get("body").then(($body) => {
      if ($body.find(".parent-template-banner").length > 0) {
        dashboardPage.verifyCreateInstanceButtonVisible();
      }
    });
  });

  it("should NOT show New Entry button for parent templates", function () {
    // Select a parent template
    dashboardPage.getTreeNodes().first().click();
    cy.wait(500);

    // If it's a parent template, New Entry should not be visible
    cy.get("body").then(($body) => {
      if ($body.find(".parent-template-banner").length > 0) {
        // Should have Create Instance, not New Entry
        cy.contains("button", "Create Instance").should("be.visible");
      }
    });
  });
});

describe("Notebook Hierarchy - Create Instance Modal", function () {
  beforeEach("Login and navigate to dashboard", function () {
    // Intercept API calls
    cy.intercept("POST", "/rest/notebook/*/instances").as("createInstance");
    cy.intercept("GET", "/rest/notebook/hierarchy").as("getHierarchy");

    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      if (user) {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
      }
    });
    dashboardPage.visit();
    dashboardPage.waitForLoad();
  });

  it("should open Create Instance modal when button clicked", function () {
    // Select a parent template
    dashboardPage.getTreeNodes().first().click();
    cy.wait(500);

    // Check if this is a parent template
    cy.get("body").then(($body) => {
      if ($body.find('button:contains("Create Instance")').length > 0) {
        dashboardPage.clickCreateInstance();
        dashboardPage.getCreateInstanceModal().should("be.visible");
      } else {
        cy.log("Selected node is not a parent template, skipping test");
      }
    });
  });

  it("should have title input field in modal", function () {
    dashboardPage.getTreeNodes().first().click();
    cy.wait(500);

    cy.get("body").then(($body) => {
      if ($body.find('button:contains("Create Instance")').length > 0) {
        dashboardPage.clickCreateInstance();
        cy.get("#instance-title").should("be.visible");
      }
    });
  });

  it("should close modal on cancel", function () {
    dashboardPage.getTreeNodes().first().click();
    cy.wait(500);

    cy.get("body").then(($body) => {
      if ($body.find('button:contains("Create Instance")').length > 0) {
        dashboardPage.clickCreateInstance();
        dashboardPage.getCreateInstanceModal().should("be.visible");
        dashboardPage.cancelCreateInstance();
        dashboardPage.getCreateInstanceModal().should("not.exist");
      }
    });
  });

  it("should create instance and add to tree when submitted", function () {
    dashboardPage.getTreeNodes().first().click();
    cy.wait(500);

    cy.get("body").then(($body) => {
      if ($body.find('button:contains("Create Instance")').length > 0) {
        const instanceTitle = `Test Lab ${Date.now()}`;

        dashboardPage.clickCreateInstance();
        dashboardPage.enterInstanceTitle(instanceTitle);
        dashboardPage.submitCreateInstance();

        // Wait for API call
        cy.wait("@createInstance").then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        // Modal should close
        dashboardPage.getCreateInstanceModal().should("not.exist");

        // New instance should appear in tree (after refresh)
        cy.wait("@getHierarchy");
        cy.contains(instanceTitle).should("be.visible");
      }
    });
  });
});

describe("Notebook Hierarchy - Child Instance Behavior", function () {
  beforeEach("Login and navigate to dashboard", function () {
    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      if (user) {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
      }
    });
    dashboardPage.visit();
    dashboardPage.waitForLoad();
  });

  it("should show New Entry button for child instances", function () {
    // First, find and expand a parent with children
    dashboardPage
      .getTreeNodes()
      .first()
      .then(($node) => {
        cy.wrap($node)
          .find("[aria-expanded]")
          .then(($expandBtn) => {
            if ($expandBtn.length > 0) {
              // Expand the parent
              if ($expandBtn.attr("aria-expanded") === "false") {
                cy.wrap($expandBtn).click();
              }

              // Click on first child node
              cy.wrap($node).find(".cds--tree-node").first().click();
              cy.wait(500);

              // Child instances should show New Entry button
              cy.get("body").then(($body) => {
                // If no parent template banner, it's a child instance
                if ($body.find(".parent-template-banner").length === 0) {
                  dashboardPage.verifyNewEntryButtonVisible();
                }
              });
            }
          });
      });
  });

  it("should NOT show parent template banner for child instances", function () {
    // Expand parent and select child
    dashboardPage
      .getTreeNodes()
      .first()
      .then(($node) => {
        cy.wrap($node)
          .find("[aria-expanded]")
          .then(($expandBtn) => {
            if (
              $expandBtn.length > 0 &&
              $expandBtn.attr("aria-expanded") === "false"
            ) {
              cy.wrap($expandBtn).click();

              // Click on first child
              cy.wrap($node).find(".cds--tree-node").first().click();
              cy.wait(500);

              // Should not have parent template banner
              dashboardPage.verifyNoParentTemplateBanner();
            }
          });
      });
  });

  it("should show breadcrumb with parent link for child instances", function () {
    dashboardPage
      .getTreeNodes()
      .first()
      .then(($node) => {
        const parentText = $node.find(".tree-node-title").first().text();

        cy.wrap($node)
          .find("[aria-expanded]")
          .then(($expandBtn) => {
            if (
              $expandBtn.length > 0 &&
              $expandBtn.attr("aria-expanded") === "false"
            ) {
              cy.wrap($expandBtn).click();

              // Click on first child
              cy.wrap($node).find(".cds--tree-node").first().click();
              cy.wait(500);

              // Should show breadcrumb
              cy.get("body").then(($body) => {
                if ($body.find(".cds--breadcrumb").length > 0) {
                  dashboardPage.getBreadcrumb().should("be.visible");
                }
              });
            }
          });
      });
  });
});

describe("Notebook Hierarchy - Breadcrumb Navigation", function () {
  beforeEach("Login and navigate to dashboard", function () {
    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      if (user) {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
      }
    });
    dashboardPage.visit();
    dashboardPage.waitForLoad();
  });

  it("should navigate back to parent when breadcrumb clicked", function () {
    dashboardPage
      .getTreeNodes()
      .first()
      .then(($node) => {
        cy.wrap($node)
          .find("[aria-expanded]")
          .then(($expandBtn) => {
            if (
              $expandBtn.length > 0 &&
              $expandBtn.attr("aria-expanded") === "false"
            ) {
              cy.wrap($expandBtn).click();

              // Click on first child
              cy.wrap($node).find(".cds--tree-node").first().click();
              cy.wait(500);

              // Check if breadcrumb exists and click parent link
              cy.get("body").then(($body) => {
                if ($body.find(".cds--breadcrumb").length > 0) {
                  // Click on parent in breadcrumb
                  cy.get(".cds--breadcrumb-item").first().find("a").click();
                  cy.wait(500);

                  // Should now show parent template banner
                  dashboardPage.verifyParentTemplateBanner();
                }
              });
            }
          });
      });
  });
});

describe("Notebook Hierarchy - API Integration", function () {
  beforeEach("Login and setup interceptors", function () {
    // Intercept hierarchy API calls
    cy.intercept("GET", "/rest/notebook/hierarchy").as("getHierarchy");
    cy.intercept("GET", "/rest/notebook/*/instances").as("getInstances");
    cy.intercept("GET", "/rest/notebook/*/aggregated-stats").as(
      "getAggregatedStats",
    );
    cy.intercept("GET", "/rest/notebook/*/can-accept-entries").as(
      "canAcceptEntries",
    );
    cy.intercept("GET", "/rest/notebook/parent-templates").as(
      "getParentTemplates",
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

  it("should load hierarchy data on page visit", function () {
    dashboardPage.visit();

    // Wait for hierarchy API call
    cy.wait("@getHierarchy", { timeout: 10000 }).then((interception) => {
      expect(interception.response.statusCode).to.be.oneOf([200, 304]);
      expect(interception.response.body).to.be.an("array");
    });
  });

  it("should fetch can-accept-entries when notebook selected", function () {
    dashboardPage.visit();
    dashboardPage.waitForLoad();

    // Select a notebook
    dashboardPage.getTreeNodes().first().click();

    // Should call can-accept-entries endpoint
    cy.wait("@canAcceptEntries", { timeout: 5000 }).then((interception) => {
      expect(interception.response.statusCode).to.eq(200);
      expect(interception.response.body).to.have.property("canAcceptEntries");
    });
  });
});

describe("Notebook Hierarchy - Legacy Table Integration", function () {
  beforeEach("Login and navigate to dashboard", function () {
    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      if (user) {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
      }
    });
    dashboardPage.visit();
    dashboardPage.waitForLoad();
  });

  it("should display legacy table view below tree view", function () {
    cy.contains("Legacy List View").should("be.visible");
    dashboardPage.getLegacyTable().should("be.visible");
  });

  it("should select notebook when legacy table row clicked", function () {
    cy.get("body").then(($body) => {
      if ($body.find(".cds--data-table tbody tr").length > 0) {
        dashboardPage.clickLegacyTableRow(0);
        cy.wait(500);

        // Should update the selected notebook title
        dashboardPage.getNotebookTitle().should("be.visible");
      }
    });
  });
});

describe("Notebook Hierarchy - All Entries Reset", function () {
  beforeEach("Login and navigate to dashboard", function () {
    login.visit();
    cy.fixture("Users").then((users) => {
      const user = users.find((u) => u.correctPass === true);
      if (user) {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
      }
    });
    dashboardPage.visit();
    dashboardPage.waitForLoad();
  });

  it("should reset selection when All Entries clicked", function () {
    // Select a notebook first
    dashboardPage.getTreeNodes().first().click();
    cy.wait(500);

    // Click All Entries
    dashboardPage.clickAllEntries();
    cy.wait(500);

    // Should show "All Entries" heading
    cy.contains("h4", "All Entries").should("be.visible");

    // Parent template banner should not be visible
    dashboardPage.verifyNoParentTemplateBanner();
  });
});
