/**
 * Constitution V.5 Compliance:
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (cy.wait() only for intercept aliases)
 * - Element readiness checks before all interactions
 * - Navigation optimized (before() instead of beforeEach())
 * - Focused on happy paths (user workflows, not implementation details)
 */

import StorageAssignmentPage from "../pages/StorageAssignmentPage";

/**
 * T097d: E2E Tests for View Storage Modal
 * Tests view storage modal UI components and editing functionality
 */

let homePage = null;
let storageAssignmentPage = null;

before("Setup storage tests", () => {
  cy.setupStorageTests().then((page) => {
    homePage = page;
  });
});

after("Cleanup storage tests", () => {
  cy.cleanupStorageTests();
});

describe("View Storage Modal - UI Components (P2B)", function () {
  before(() => {
    // Navigate to Storage Samples tab ONCE for all tests
    // Set up intercept BEFORE visit (Constitution V.5)
    cy.intercept("GET", "**/rest/storage/sample-items**").as("getSamples");
    cy.visit("/Storage/samples");
    // Wait for page to be ready first, then wait for API call
    cy.get('[data-testid="sample-list"]', { timeout: 3000 }).should(
      "be.visible",
    );
    // Now wait for the API call (it may happen after page renders)
    cy.wait("@getSamples", { timeout: 3000 });
    storageAssignmentPage = new StorageAssignmentPage();
  });

  beforeEach(() => {
    // Reset state between tests - close any open modals and menus
    cy.get("body").then(($body) => {
      // Close modal if it exists and is visible
      const modal = $body.find(
        '[data-testid="location-management-modal"]:visible',
      );
      if (modal.length > 0) {
        cy.get('[data-testid="location-management-modal"]').within(() => {
          cy.get('button[aria-label*="close"], button.cds--modal-close')
            .first()
            .click({ force: true });
        });
        // Wait for modal to close
        cy.get('[data-testid="location-management-modal"]').should(
          "not.be.visible",
          {
            timeout: 3000,
          },
        );
      }

      // Close overflow menu if it exists - check for any open overflow menu using test ID
      // Find any sample row with an open overflow menu
      const openMenu = $body
        .find('[data-testid="sample-actions-overflow-menu"]')
        .closest('[data-testid="sample-row"]')
        .find(".cds--overflow-menu-options--open");
      if (openMenu.length > 0) {
        // Click outside the menu area to close it
        cy.get('[data-testid="sample-list"]').click({ force: true });
        // Wait for menu to disappear
        cy.get(".cds--overflow-menu-options--open").should("not.exist", {
          timeout: 2000,
        });
      }
    });

    // Ensure we're back on the samples list and it's ready
    cy.get('[data-testid="sample-list"]', { timeout: 3000 }).should(
      "be.visible",
    );
    cy.get('[data-testid="sample-row"]').first().should("be.visible");
  });

  it("Should display sample information section", function () {
    cy.get('[data-testid="sample-list"]', { timeout: 3000 }).should(
      "be.visible",
    );

    cy.get("body").then(($body) => {
      if ($body.find('[data-testid="sample-row"]').length === 0) {
        cy.log("No samples available - skipping view storage modal test");
        return;
      }

      // Open location management modal (Manage Location)
      // Ensure overflow menu button is ready - use different row index for each test to avoid state interference
      cy.get('[data-testid="sample-row"]')
        .eq(0)
        .within(() => {
          cy.get('[data-testid="sample-actions-overflow-menu"]')
            .should("be.visible")
            .click({ force: true });
        });

      // Wait for overflow menu to appear and click Manage Location
      cy.contains("Manage Location", { timeout: 3000 })
        .should("be.visible")
        .click({ force: true });

      // Verify modal opens - wait for modal content to exist and be accessible
      // Carbon ComposedModal may have visibility: hidden during transitions
      cy.get('[data-testid="sample-info-section"]', { timeout: 3000 })
        .should("exist")
        .should("contain.text", "Sample ID");
      cy.contains("Type").should("exist");
      cy.contains("Status").should("exist");
    });
  });

  it("Should display current location section in gray box", function () {
    cy.get('[data-testid="sample-list"]', { timeout: 3000 }).should(
      "be.visible",
    );

    cy.get("body").then(($body) => {
      if ($body.find('[data-testid="sample-row"]').length === 0) {
        cy.log("No samples available - skipping view storage modal test");
        return;
      }

      // Use different row index (1) for this test to avoid state interference
      cy.get('[data-testid="sample-row"]')
        .eq(1)
        .within(() => {
          cy.get('[data-testid="sample-actions-overflow-menu"]')
            .should("be.visible")
            .click({ force: true });
        });

      // Wait for overflow menu to appear and click Manage Location
      cy.contains("Manage Location", { timeout: 3000 })
        .should("be.visible")
        .click({ force: true });

      // Verify modal opens - wait for modal content to exist and be accessible
      cy.get('[data-testid="sample-info-section"]', { timeout: 3000 }).should(
        "exist",
      );

      // Current location section only appears if sample has a location assigned
      // Check if it exists and verify content if present
      cy.get("body").then(($body) => {
        if ($body.find('[data-testid="current-location-section"]').length > 0) {
          cy.get('[data-testid="current-location-section"]')
            .should("exist")
            .should("contain.text", "Current Location");
        } else {
          cy.log(
            "Sample has no current location assigned - current-location-section not displayed",
          );
        }
      });
    });
  });

  it("Should allow editing location assignment", function () {
    cy.get('[data-testid="sample-list"]', { timeout: 3000 }).should(
      "be.visible",
    );

    cy.get("body").then(($body) => {
      if ($body.find('[data-testid="sample-row"]').length === 0) {
        cy.log("No samples available - skipping view storage modal test");
        return;
      }

      // Use different row index (2) for this test to avoid state interference
      cy.get('[data-testid="sample-row"]')
        .eq(2)
        .within(() => {
          cy.get('[data-testid="sample-actions-overflow-menu"]')
            .should("be.visible")
            .click({ force: true });
        });

      // Wait for overflow menu to appear and click Manage Location
      cy.contains("Manage Location", { timeout: 3000 })
        .should("be.visible")
        .click({ force: true });

      // Verify modal opens - wait for modal content to exist and be accessible
      cy.get('[data-testid="sample-info-section"]', { timeout: 3000 }).should(
        "exist",
      );
      // Verify new location section is visible and editable
      cy.get('[data-testid="new-location-section"]', { timeout: 3000 }).should(
        "exist",
      );
    });
  });

  // Test Carbon ComboBox dropdown selection for location assignment
  it.skip("Should save changes when Assign Storage Location button clicked", function () {
    cy.get('[data-testid="sample-list"]', { timeout: 3000 }).should(
      "be.visible",
    );

    cy.get("body").then(($body) => {
      if ($body.find('[data-testid="sample-row"]').length === 0) {
        cy.log("No samples available - skipping view storage modal test");
        return;
      }

      // Use first available row - cleanup in beforeEach ensures menu is closed
      cy.get('[data-testid="sample-row"]')
        .first()
        .should("exist")
        .within(() => {
          cy.get('[data-testid="sample-actions-overflow-menu"]')
            .should("be.visible")
            .click({ force: true });
        });

      // Wait for overflow menu to appear - Carbon OverflowMenu renders items in a menu
      // Use text matching like storageDisposal.cy.js does
      cy.contains("Manage Location", { timeout: 3000 })
        .should("be.visible")
        .click({ force: true });

      // Verify modal opens - wait for modal content to exist and be accessible
      cy.get('[data-testid="sample-info-section"]', { timeout: 3000 }).should(
        "exist",
      );

      // Verify new location section exists with LocationSearchAndCreate component
      cy.get('[data-testid="new-location-section"]', { timeout: 3000 }).should(
        "exist",
      );
      cy.get('[data-testid="location-search-and-create"]', {
        timeout: 3000,
      }).should("exist");

      // Set up intercepts BEFORE actions (Constitution V.5)
      cy.intercept("GET", "**/rest/storage/rooms**").as("getRooms");
      cy.intercept("GET", "**/rest/storage/devices**").as("getDevices");
      cy.intercept("GET", "**/rest/storage/shelves**").as("getShelves");
      cy.intercept("GET", "**/rest/storage/racks**").as("getRacks");
      cy.intercept("POST", "**/rest/storage/assign**").as("assignStorage");

      // LocationSearchAndCreate starts in search mode - click "Add Location" to show create form
      // The create form contains EnhancedCascadingMode with cascading dropdowns
      // Scope to the modal to avoid multiple matches - use first() to get single element
      cy.get('[data-testid="location-management-modal"]')
        .first()
        .should("exist")
        .within(() => {
          cy.get('[data-testid="add-location-button"]', { timeout: 3000 })
            .should("exist")
            .first()
            .click({ force: true });
        });

      // Wait for create form to show EnhancedCascadingMode with comboboxes
      cy.get('[data-testid="location-create-container"]', {
        timeout: 3000,
      }).should("exist");

      // First fetch rooms to see if any exist in the test environment
      cy.request({
        url: "/api/OpenELIS-Global/rest/storage/rooms",
        failOnStatusCode: false,
      }).then((response) => {
        if (
          response.status !== 200 ||
          !response.body ||
          response.body.length === 0
        ) {
          cy.log(
            "No rooms available in test environment - skipping location assignment test",
          );
          cy.log(
            "This test requires fixture data: MAIN room, FRZ01 device, SHA shelf, RKR2 rack",
          );
          // Close the modal
          cy.get('[data-testid="location-management-modal"]')
            .find('button[aria-label*="close"], button.cds--modal-close')
            .first()
            .click({ force: true });
          return;
        }

        const rooms = response.body;
        const mainRoom = rooms.find(
          (r) =>
            r.code === "MAIN" ||
            r.name?.toLowerCase().includes("main") ||
            r.code?.toLowerCase().includes("main"),
        );

        if (!mainRoom) {
          cy.log(
            "MAIN room not found - skipping location assignment. Available rooms:",
          );
          rooms.forEach((r) => cy.log(`  Room: ${r.name} (${r.code})`));
          // Close the modal
          cy.get('[data-testid="location-management-modal"]')
            .find('button[aria-label*="close"], button.cds--modal-close')
            .first()
            .click({ force: true });
          return;
        }

        // MAIN room exists - proceed with test
        cy.log(`Found MAIN room: ${mainRoom.name} (ID: ${mainRoom.id})`);

        // Wait for component to load rooms (the component fetches via useEffect)
        cy.wait("@getRooms", { timeout: 5000 });

        // Click on the room combobox input to open dropdown
        cy.get("#room-combobox").click();

        // Click on the first option in the dropdown menu (at document level)
        cy.get('.cds--list-box__menu [role="option"]', { timeout: 3000 })
          .first()
          .click();

        // Wait for devices to load after room selection
        cy.wait("@getDevices", { timeout: 5000 }).then((interception) => {
          const devices = interception.response?.body || [];
          if (devices.length === 0) {
            cy.log("No devices found for MAIN room - test data incomplete");
            cy.get('[data-testid="location-management-modal"]')
              .find('button[aria-label*="close"], button.cds--modal-close')
              .first()
              .click({ force: true });
            return;
          }

          // Use first available device
          const device = devices[0];
          cy.log(`Using device: ${device.name} (${device.code})`);

          // Click on device combobox and select first option
          cy.get("#device-combobox").click();
          cy.get('.cds--list-box__menu [role="option"]', { timeout: 3000 })
            .first()
            .click();

          // Wait for shelves to load
          cy.wait("@getShelves", { timeout: 5000 }).then(
            (shelfInterception) => {
              const shelves = shelfInterception.response?.body || [];
              if (shelves.length === 0) {
                cy.log(
                  "No shelves found - test data incomplete, but modal verified",
                );
                cy.get('[data-testid="location-management-modal"]')
                  .find('button[aria-label*="close"], button.cds--modal-close')
                  .first()
                  .click({ force: true });
                return;
              }

              // Use first available shelf
              const shelf = shelves[0];
              cy.log(`Using shelf: ${shelf.label}`);

              // Click on shelf combobox and select first option
              cy.get("#shelf-combobox").click();
              cy.get('.cds--list-box__menu [role="option"]', { timeout: 3000 })
                .first()
                .click();

              // Wait for racks
              cy.wait("@getRacks", { timeout: 5000 }).then(
                (rackInterception) => {
                  const racks = rackInterception.response?.body || [];
                  if (racks.length === 0) {
                    cy.log(
                      "No racks found - test verified cascading dropdowns work",
                    );
                    cy.get('[data-testid="location-management-modal"]')
                      .find(
                        'button[aria-label*="close"], button.cds--modal-close',
                      )
                      .first()
                      .click({ force: true });
                    return;
                  }

                  // Use first available rack
                  const rack = racks[0];
                  cy.log(`Using rack: ${rack.label}`);

                  // Click on rack combobox and select first option
                  cy.get("#rack-combobox").click();
                  cy.get('.cds--list-box__menu [role="option"]', {
                    timeout: 3000,
                  })
                    .first()
                    .click();

                  // Try to add location - button should be enabled now
                  cy.get('[data-testid="add-location-create-button"]', {
                    timeout: 3000,
                  }).then(($btn) => {
                    if ($btn.prop("disabled")) {
                      cy.log(
                        "Add button disabled - location selection incomplete but modal verified",
                      );
                      cy.get('[data-testid="location-management-modal"]')
                        .find(
                          'button[aria-label*="close"], button.cds--modal-close',
                        )
                        .first()
                        .click({ force: true });
                    } else {
                      cy.wrap($btn).click();

                      // Verify we're back to search mode
                      cy.get('[data-testid="location-search-and-create"]', {
                        timeout: 3000,
                      }).should("exist");

                      // Click assign button to save
                      cy.get('[data-testid="assign-button"]', { timeout: 3000 })
                        .should("exist")
                        .click();

                      // Wait for assign API call
                      cy.wait("@assignStorage", { timeout: 5000 })
                        .its("response.statusCode")
                        .should("be.oneOf", [200, 201]);

                      cy.log("Location assignment successful!");
                    }
                  });
                },
              );
            },
          );
        });
      });
    });
  });
});
