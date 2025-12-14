/**
 * E2E Tests for Pharmaceutical Laboratory Dashboard
 * Tests dashboard loading, metric cards, charts, and export functionality
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Focused on happy paths (user workflows, not implementation details)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/pharmaceuticalDashboard.cy.js"
 */

describe("Pharmaceutical Laboratory Dashboard", function () {
  beforeEach(() => {
    // Set up API intercepts BEFORE navigation
    cy.intercept("GET", "**/rest/pharmaceutical/reports/dashboard**").as(
      "getDashboard",
    );
    cy.intercept("GET", "**/rest/pharmaceutical/samples**").as("getSamples");
    cy.intercept("GET", "**/rest/pharmaceutical/excursions/active**").as(
      "getActiveExcursions",
    );

    // Login before each test
    cy.login("admin", "adminADMIN!");
  });

  describe("Dashboard Loading", () => {
    it("should navigate to Pharmaceutical Dashboard and verify it loads", () => {
      // Navigate directly to Pharmaceutical page
      cy.visit("/Pharmaceutical");

      // Wait for page to load
      cy.url().should("include", "/Pharmaceutical");

      // Verify main dashboard container is visible
      cy.get("[class*='pharmaceutical']", { timeout: 10000 }).should("exist");
    });

    it("should display loading indicator while fetching data", () => {
      // Delay the API response
      cy.intercept("GET", "**/rest/pharmaceutical/reports/dashboard**", {
        delay: 1000,
        body: {},
      }).as("slowDashboard");

      cy.visit("/Pharmaceutical");

      // Should show loading state
      cy.get(".cds--loading", { timeout: 5000 }).should("be.visible");
    });
  });

  describe("Metric Tiles", () => {
    beforeEach(() => {
      // Mock dashboard data
      cy.intercept("GET", "**/rest/pharmaceutical/reports/dashboard**", {
        statusCode: 200,
        body: {
          totalSamples: 150,
          activeSamples: 120,
          pendingQC: 15,
          qcPassed: 100,
          qcFailed: 5,
          pendingDisposal: 8,
          activeExcursions: 2,
          expiringSoon: 12,
          oosCount: 3,
          avgTAT: 24.5,
          qcPassRate: 95.2,
          slaCompliance: 98.0,
        },
      }).as("getDashboardData");

      cy.visit("/Pharmaceutical");
      cy.wait("@getDashboardData");
    });

    it("should display key metric tiles", () => {
      // Verify metric tiles are visible
      cy.get(".cds--tile", { timeout: 10000 }).should(
        "have.length.at.least",
        4,
      );

      // Verify labels are translated, not showing raw i18n keys
      cy.get(".cds--tile").first().should("not.contain", "pharmaceutical.");
    });

    it("should show active excursions alert when excursions exist", () => {
      // Look for excursion alert (warning indicator)
      cy.get("[class*='alert']").should("exist");
    });

    it("should display pending disposal count", () => {
      // Look for pending disposal metric
      cy.contains("Pending Disposal").should("be.visible");
    });
  });

  describe("Charts and Visualizations", () => {
    beforeEach(() => {
      cy.intercept("GET", "**/rest/pharmaceutical/reports/dashboard**", {
        statusCode: 200,
        body: {
          totalSamples: 150,
          samplesByLabType: [
            { group: "PHARMA", value: 80 },
            { group: "BIOLOGICAL", value: 40 },
            { group: "ENVIRONMENTAL", value: 30 },
          ],
          excursionsByStatus: [
            { group: "ACTIVE", value: 2 },
            { group: "RESOLVED", value: 15 },
          ],
          assaysByType: [
            { group: "HPLC", value: 45 },
            { group: "GC", value: 30 },
          ],
        },
      }).as("getChartData");

      cy.visit("/Pharmaceutical");
      cy.wait("@getChartData");
    });

    it("should render chart components", () => {
      // Wait for charts to render (Carbon Charts uses SVG)
      cy.get("svg", { timeout: 10000 }).should("exist");
    });

    it("should display samples by lab type chart", () => {
      cy.contains("Samples by Lab Type").should("be.visible");
    });
  });

  describe("Export Functionality", () => {
    beforeEach(() => {
      cy.intercept("GET", "**/rest/pharmaceutical/reports/dashboard**", {
        statusCode: 200,
        body: { totalSamples: 100 },
      }).as("getDashboardData");

      cy.visit("/Pharmaceutical");
      cy.wait("@getDashboardData");
    });

    it("should display export reports section", () => {
      cy.contains("Export Reports").should("be.visible");
    });

    it("should have CSV export button for excursion report", () => {
      cy.contains("Excursion Report").should("be.visible");
      cy.get("button")
        .contains(/download|export/i)
        .should("exist");
    });
  });

  describe("Tab Navigation", () => {
    beforeEach(() => {
      cy.intercept("GET", "**/rest/pharmaceutical/reports/dashboard**", {
        statusCode: 200,
        body: { totalSamples: 100 },
      }).as("getDashboardData");

      cy.visit("/Pharmaceutical");
      cy.wait("@getDashboardData");
    });

    it("should have Dashboard tab selected by default", () => {
      cy.get(".cds--tabs__nav-link--selected").should("contain", "Dashboard");
    });

    it("should switch to Samples tab when clicked", () => {
      cy.intercept("GET", "**/rest/pharmaceutical/samples**", {
        body: [],
      }).as("getSamples");

      cy.contains("Samples").click();
      cy.wait("@getSamples");

      // Verify samples content is visible
      cy.get("[class*='sample']").should("exist");
    });
  });

  describe("Error Handling", () => {
    it("should display error notification when API fails", () => {
      cy.intercept("GET", "**/rest/pharmaceutical/reports/dashboard**", {
        statusCode: 500,
        body: { error: "Internal Server Error" },
      }).as("failedDashboard");

      cy.visit("/Pharmaceutical");

      // Should show error notification
      cy.get(".cds--inline-notification--error", { timeout: 10000 }).should(
        "be.visible",
      );
    });
  });

  describe("Accessibility", () => {
    beforeEach(() => {
      cy.intercept("GET", "**/rest/pharmaceutical/reports/dashboard**", {
        statusCode: 200,
        body: { totalSamples: 100 },
      }).as("getDashboardData");

      cy.visit("/Pharmaceutical");
      cy.wait("@getDashboardData");
    });

    it("should have accessible metric tiles", () => {
      // Verify tiles have proper structure
      cy.get(".cds--tile").each(($tile) => {
        // Each tile should have text content
        cy.wrap($tile).should("not.be.empty");
      });
    });
  });
});

describe("Pharmaceutical Sample Management", function () {
  beforeEach(() => {
    cy.intercept("GET", "**/rest/pharmaceutical/samples**").as("getSamples");
    cy.login("admin", "adminADMIN!");
  });

  describe("Sample Registration", () => {
    it("should open sample registration modal", () => {
      cy.intercept("GET", "**/rest/pharmaceutical/samples**", {
        body: [],
      }).as("getSamplesList");

      cy.visit("/Pharmaceutical");

      // Navigate to Samples tab
      cy.contains("Samples").click();
      cy.wait("@getSamplesList");

      // Click register button
      cy.contains("Register Sample").click();

      // Verify modal is open
      cy.get(".cds--modal--open").should("be.visible");
    });
  });

  describe("Sample Search", () => {
    it("should filter samples by search query", () => {
      cy.intercept("GET", "**/rest/pharmaceutical/samples**", {
        body: [
          { id: 1, sampleName: "Aspirin Test", status: "REGISTERED" },
          { id: 2, sampleName: "Ibuprofen Sample", status: "QC_PASSED" },
        ],
      }).as("getSamplesList");

      cy.visit("/Pharmaceutical");
      cy.contains("Samples").click();
      cy.wait("@getSamplesList");

      // Search for sample
      cy.get('input[type="search"]').type("Aspirin");

      // Verify filtered results
      cy.contains("Aspirin Test").should("be.visible");
    });
  });
});

describe("Environmental Excursion Management", function () {
  beforeEach(() => {
    cy.intercept("GET", "**/rest/pharmaceutical/excursions**").as(
      "getExcursions",
    );
    cy.login("admin", "adminADMIN!");
  });

  describe("Excursion List", () => {
    it("should display excursion list with status tags", () => {
      cy.intercept("GET", "**/rest/pharmaceutical/excursions**", {
        body: [
          {
            id: 1,
            deviceId: "FREEZER-001",
            alertType: "TEMPERATURE_HIGH",
            status: "ACTIVE",
          },
          {
            id: 2,
            deviceId: "FREEZER-002",
            alertType: "POWER_FAILURE",
            status: "RESOLVED",
          },
        ],
      }).as("getExcursionsList");

      cy.visit("/Pharmaceutical");
      cy.contains("Excursions").click();
      cy.wait("@getExcursionsList");

      // Verify excursions are displayed
      cy.contains("FREEZER-001").should("be.visible");

      // Verify status tags
      cy.get(".cds--tag").should("exist");
    });
  });

  describe("Excursion Acknowledgement", () => {
    it("should allow acknowledging an active excursion", () => {
      cy.intercept("GET", "**/rest/pharmaceutical/excursions**", {
        body: [
          {
            id: 1,
            deviceId: "FREEZER-001",
            alertType: "TEMPERATURE_HIGH",
            status: "ACTIVE",
          },
        ],
      }).as("getExcursionsList");

      cy.intercept(
        "POST",
        "**/rest/pharmaceutical/excursions/1/acknowledge**",
        {
          statusCode: 200,
          body: { status: "ACKNOWLEDGED" },
        },
      ).as("acknowledgeExcursion");

      cy.visit("/Pharmaceutical");
      cy.contains("Excursions").click();
      cy.wait("@getExcursionsList");

      // Click acknowledge button
      cy.contains("Acknowledge").click();

      // Fill acknowledgement notes if modal opens
      cy.get(".cds--modal--open").then(($modal) => {
        if ($modal.length) {
          cy.get("textarea").type("Acknowledged and investigating");
          cy.get(".cds--modal--open").contains("Submit").click();
        }
      });

      cy.wait("@acknowledgeExcursion");
    });
  });
});

describe("Disposal Workflow", function () {
  beforeEach(() => {
    cy.intercept("GET", "**/rest/pharmaceutical/disposal**").as(
      "getDisposalRecords",
    );
    cy.login("admin", "adminADMIN!");
  });

  describe("Disposal Request", () => {
    it("should display pending disposal requests", () => {
      cy.intercept("GET", "**/rest/pharmaceutical/disposal**", {
        body: [
          {
            id: 1,
            sample: { sampleName: "Expired Sample" },
            reason: "EXPIRED",
            status: "PENDING_APPROVAL",
          },
        ],
      }).as("getDisposalList");

      cy.visit("/Pharmaceutical");
      cy.contains("Disposal").click();
      cy.wait("@getDisposalList");

      // Verify pending disposal is displayed
      cy.contains("Expired Sample").should("be.visible");
      cy.contains("PENDING").should("be.visible");
    });
  });

  describe("Disposal Approval", () => {
    it("should allow approving a disposal request", () => {
      cy.intercept("GET", "**/rest/pharmaceutical/disposal**", {
        body: [
          {
            id: 1,
            sample: { sampleName: "Expired Sample" },
            reason: "EXPIRED",
            status: "PENDING_APPROVAL",
          },
        ],
      }).as("getDisposalList");

      cy.intercept("POST", "**/rest/pharmaceutical/disposal/1/approve**", {
        statusCode: 200,
        body: { status: "APPROVED" },
      }).as("approveDisposal");

      cy.visit("/Pharmaceutical");
      cy.contains("Disposal").click();
      cy.wait("@getDisposalList");

      // Click approve button
      cy.contains("Approve").click();

      cy.wait("@approveDisposal");
    });
  });
});
