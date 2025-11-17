/**
 * E2E Tests for Barcode Scanning and Label Printing Workflows
 * Tests barcode auto-open behavior and simplified label printing
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Focused on happy paths (user workflows, not implementation details)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/barcodeWorkflow.cy.js"
 */

let homePage = null;

before("Setup storage tests", () => {
  cy.setupStorageTests().then((page) => {
    homePage = page;
  });
});

after("Cleanup storage tests", () => {
  cy.cleanupStorageTests();
});

describe("Barcode Scan Auto-Open", () => {
  beforeEach(() => {
    // Set up intercepts BEFORE actions (Constitution V.5)
    // Note: Backend expects POST with JSON body
    cy.intercept("POST", "**/rest/storage/barcode/validate**").as(
      "validateBarcode",
    );
    cy.intercept("GET", "**/rest/storage/devices**").as("getDevices");

    // Navigate to Storage Dashboard
    cy.visit("/Storage");
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
  });

  it("should auto-open location form when barcode has valid partial hierarchy", () => {
    // Core functionality: Scan barcode → form auto-opens
    // Navigate to Samples tab where LocationManagementModal can be opened
    cy.get('[data-testid="tab-samples"]').click();
    cy.screenshot("01-samples-tab-clicked");

    // Wait for samples table to load
    cy.get('[data-testid="sample-row"]', { timeout: 10000 }).should("exist");
    cy.screenshot("02-samples-table-loaded");

    // Open LocationManagementModal via Carbon OverflowMenu pattern
    // 1. Find overflow menu button in first sample row
    cy.get('[data-testid="sample-row"]')
      .first()
      .find('[data-testid="sample-actions-overflow-menu"]')
      .click();

    // 2. Wait for menu to render in portal (Carbon OverflowMenu pattern)
    cy.get('[role="menu"]').should("be.visible");
    cy.screenshot("03-overflow-menu-opened");

    // 3. Click "Manage Location" menu item
    cy.get('[data-testid="manage-location-menu-item"]').click();
    cy.screenshot("03b-after-manage-location-click");

    // 4. Wait for modal to open - wait for dialog first, then the barcode input
    // Carbon ComposedModal renders in a portal, wait for dialog to be visible
    cy.get('[data-testid="location-management-modal"]', {
      timeout: 10000,
    }).should("be.visible");

    // Then wait for the barcode input to be visible and interactable
    cy.get('[data-testid="location-management-modal"]')
      .find("#barcode-input")
      .should("be.visible")
      .should("not.be.disabled");
    cy.screenshot("04-modal-opened-barcode-input-visible");

    // Scan barcode - scope to modal to ensure we get the right input
    // Focus and type - Enter key triggers validation via handleKeyDown
    cy.get('[data-testid="location-management-modal"]')
      .find("#barcode-input")
      .focus()
      .clear()
      .type("MAIN-FRZ01")
      .type("{enter}");
    cy.screenshot("05-barcode-scanned");

    cy.wait("@validateBarcode", { timeout: 10000 });

    // Verify form auto-opens with pre-filled hierarchy
    cy.get('[data-testid="room-combobox"]').should("be.visible");
    cy.get('[data-testid="device-combobox"]').should("be.visible");
    cy.screenshot("06-form-auto-opened-with-prefilled-hierarchy");
  });

  it("should show error and NOT open form when invalid format (no hyphens) is entered", () => {
    // Navigate to Samples tab
    cy.get('[data-testid="tab-samples"]').click();

    // Wait for samples table to load
    cy.get('[data-testid="sample-row"]', { timeout: 10000 }).should("exist");

    // Open LocationManagementModal
    cy.get('[data-testid="sample-row"]')
      .first()
      .find('[data-testid="sample-actions-overflow-menu"]')
      .click();

    cy.get('[role="menu"]').should("be.visible");
    cy.get('[data-testid="manage-location-menu-item"]').click();

    // Wait for modal to open
    cy.get('[data-testid="location-management-modal"]', {
      timeout: 10000,
    }).should("be.visible");
    cy.get('[data-testid="location-management-modal"]')
      .find("#barcode-input")
      .should("be.visible");

    // Type invalid format (no hyphens)
    cy.get('[data-testid="location-management-modal"]')
      .find("#barcode-input")
      .focus()
      .clear()
      .type("234")
      .type("{enter}");

    cy.wait("@validateBarcode", { timeout: 10000 });

    // Verify error is displayed
    cy.get('[data-testid="unified-barcode-input"]')
      .find('[data-state="error"]')
      .should("be.visible");

    // Verify form does NOT auto-open
    cy.get('[data-testid="room-combobox"]').should("not.exist");
  });

  it("should show error and NOT open form when invalid barcode (hyphens but invalid location) is entered", () => {
    // Navigate to Samples tab
    cy.get('[data-testid="tab-samples"]').click();

    // Wait for samples table to load
    cy.get('[data-testid="sample-row"]', { timeout: 10000 }).should("exist");

    // Open LocationManagementModal
    cy.get('[data-testid="sample-row"]')
      .first()
      .find('[data-testid="sample-actions-overflow-menu"]')
      .click();

    cy.get('[role="menu"]').should("be.visible");
    cy.get('[data-testid="manage-location-menu-item"]').click();

    // Wait for modal to open
    cy.get('[data-testid="location-management-modal"]', {
      timeout: 10000,
    }).should("be.visible");
    cy.get('[data-testid="location-management-modal"]')
      .find("#barcode-input")
      .should("be.visible");

    // Type invalid barcode (hyphens but invalid location)
    cy.get('[data-testid="location-management-modal"]')
      .find("#barcode-input")
      .focus()
      .clear()
      .type("INVALID-CODE")
      .type("{enter}");

    cy.wait("@validateBarcode", { timeout: 10000 });

    // Verify error is displayed
    cy.get('[data-testid="unified-barcode-input"]')
      .find('[data-state="error"]')
      .should("be.visible");

    // Verify form does NOT auto-open
    cy.get('[data-testid="room-combobox"]').should("not.exist");
  });
});

describe("Label Printing", () => {
  beforeEach(() => {
    // Set up intercepts BEFORE actions
    cy.intercept("GET", "**/rest/storage/devices/**").as("getDevice");
    cy.intercept("PUT", "**/rest/storage/devices/**").as("updateDevice");
    // Intercept print label endpoint - pattern: /rest/storage/{type}/{id}/print-label
    // Intercept all POST requests to print-label (match any path containing print-label)
    cy.intercept("POST", "**/print-label", (req) => {
      // Use console.log instead of cy.log in intercept callback
      console.log("Intercepted print-label request:", req.url);
      console.log("Request method:", req.method);
      console.log("Request body:", req.body);
      req.continue((res) => {
        console.log("Print label response status:", res.statusCode);
        console.log(
          "Print label response headers:",
          JSON.stringify(res.headers),
        );
        if (res.statusCode !== 200) {
          console.log("Print label response body:", res.body);
        }
      });
    }).as("printLabel");

    // Navigate to Storage Dashboard
    cy.visit("/Storage");
    cy.get(".storage-dashboard").should("be.visible");
  });

  it("should print label after updating code via Edit form", () => {
    // Core functionality: Edit → update code → print label
    cy.get('[data-testid="tab-devices"]').click();
    cy.screenshot("label-01-devices-tab-clicked");

    // Wait for table to load and find first device row
    cy.get('[data-testid^="device-row-"]', { timeout: 10000 })
      .first()
      .within(() => {
        cy.get('[data-testid="location-actions-overflow-menu"]')
          .should("be.visible")
          .click();
      });
    cy.screenshot("label-02-device-row-overflow-menu-clicked");

    // Wait for menu to render in portal (Carbon OverflowMenu pattern)
    cy.get('[role="menu"]').should("be.visible");
    cy.screenshot("label-03-overflow-menu-opened");

    cy.get('[data-testid="edit-location-menu-item"]').click();

    cy.get('[role="dialog"]').should("be.visible");
    cy.wait("@getDevice", { timeout: 10000 });
    cy.screenshot("label-04-edit-modal-opened");

    // Verify code field exists and is editable
    cy.get('[data-testid="edit-location-device-code"]')
      .should("be.visible")
      .should("not.be.disabled");

    // Update code (ensure it's ≤10 chars)
    cy.get('[data-testid="edit-location-device-code"]')
      .clear()
      .type("TEST-FRZ01");
    cy.screenshot("label-05-code-entered");

    cy.get('[data-testid="edit-location-save-button"]')
      .should("not.be.disabled")
      .click();
    cy.wait("@updateDevice", { timeout: 10000 }).then((interception) => {
      console.log("Device update response:", interception.response?.statusCode);
      console.log(
        "Device update body:",
        JSON.stringify(interception.response?.body),
      );
    });
    cy.screenshot("label-06-device-saved");

    // Wait a bit for the database transaction to commit and device to be reloaded
    cy.wait(500);

    // Print label - find the row again after save
    // Wait for modal to close (check for modal container to not be visible)
    cy.get('[data-testid="location-management-modal"]', {
      timeout: 10000,
    }).should("not.be.visible");
    cy.get('[data-testid^="device-row-"]', { timeout: 10000 })
      .first()
      .should("be.visible")
      .scrollIntoView()
      .within(() => {
        cy.get('[data-testid="location-actions-overflow-menu"]')
          .should("be.visible")
          .should("not.be.disabled")
          .click({ force: true });
      });
    cy.screenshot("label-07-overflow-menu-opened-again");

    // Wait for menu to render in portal
    cy.get('[role="menu"]').should("be.visible");
    cy.screenshot("label-08-menu-visible");

    cy.get('[data-testid="print-label-menu-item"]').click({ force: true });
    cy.screenshot("label-09-print-label-clicked");

    // Wait for print label confirmation dialog (now at parent level in StorageDashboard)
    cy.get('[data-testid="print-label-confirmation-dialog"]', {
      timeout: 10000,
    }).should("be.visible");
    cy.screenshot("label-10-confirmation-dialog-opened");

    cy.get('[data-testid="confirm-print-button"]')
      .should("not.be.disabled")
      .click();
    cy.screenshot("label-11-print-confirmed");

    // Wait for the print label request - verify response
    cy.wait("@printLabel", { timeout: 10000 }).then((interception) => {
      const requestUrl = interception.request.url;
      // Use console.log instead of cy.log inside .then() callback
      console.log("Print label request URL:", requestUrl);
      console.log("Print label request method:", interception.request.method);
      console.log(
        "Print label response status:",
        interception.response?.statusCode,
      );
      console.log(
        "Print label response headers:",
        JSON.stringify(interception.response?.headers),
      );
      if (interception.response?.statusCode !== 200) {
        console.log(
          "Response body:",
          JSON.stringify(interception.response?.body),
        );
        // For 400 errors, log the error message to understand validation failure
        if (interception.response?.statusCode === 400) {
          const body = interception.response?.body;
          if (typeof body === "string") {
            try {
              const errorJson = JSON.parse(body);
              console.log(
                "Error message:",
                errorJson.error || errorJson.message,
              );
            } catch (e) {
              console.log("Error response (not JSON):", body);
            }
          } else if (body && body.error) {
            console.log("Error message:", body.error);
          }
        }
      }
      // If 404, check if the URL pattern matches what we expect
      if (interception.response?.statusCode === 404) {
        console.log("404 error - checking if URL pattern is correct");
        console.log("Expected pattern: /rest/storage/{type}/{id}/print-label");
        console.log("Actual URL:", requestUrl);
      }
      expect(interception.response.statusCode).to.equal(200);
      expect(interception.response.headers["content-type"]).to.include("pdf");
    });
    cy.screenshot("label-12-label-printed-success");
  });
});
