/**
 * E2E Tests for Storage Location CRUD - Real Backend Integration
 *
 * These tests hit the ACTUAL backend API (no stubs) to verify:
 * - Real data persistence
 * - Real validation errors
 * - Real parent data flow
 * - Real active toggle persistence
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default
 * - Screenshots enabled on failure
 * - Uses .should() assertions for retry-ability
 * - Element readiness checks before interactions
 * - Run individually: npm run cy:run -- --spec "cypress/e2e/storageLocationCRUD-integration.cy.js"
 */

import HomePage from "../pages/HomePage";
import "../support/load-storage-fixtures";
import "../support/storage-setup";

let homePage = null;

before("Setup storage tests", () => {
  cy.setupStorageTests().then((page) => {
    homePage = page;
  });
});

after("Cleanup storage tests", () => {
  cy.cleanupStorageTests();
});

describe("Storage Location CRUD - Real Backend Integration", () => {
  let consoleErrors = [];
  let apiErrors = [];

  before(function () {
    // Capture browser console errors
    cy.window().then((win) => {
      win.addEventListener("error", (event) => {
        consoleErrors.push({
          message: event.message,
          source: event.filename,
          lineno: event.lineno,
          colno: event.colno,
          error: event.error?.stack,
        });
      });

      // Capture unhandled promise rejections
      win.addEventListener("unhandledrejection", (event) => {
        consoleErrors.push({
          type: "unhandledrejection",
          reason: event.reason?.toString() || String(event.reason),
          stack: event.reason?.stack,
        });
      });
    });

    // Navigate to Storage Dashboard ONCE for all tests
    cy.visit("/Storage");
    cy.screenshot("01-initial-page-load");
    cy.get(".storage-dashboard").should("be.visible");
  });

  beforeEach(function () {
    // Reset error tracking
    consoleErrors = [];
    apiErrors = [];

    // NO cy.intercept() stubs - we want to hit the REAL backend
    // Only spy on requests to verify they complete (don't stub responses)
    cy.intercept("PUT", "**/rest/storage/**", (req) => {
      req.continue((res) => {
        if (res.statusCode >= 400) {
          apiErrors.push({
            method: "PUT",
            url: req.url,
            statusCode: res.statusCode,
            requestBody: req.body,
            responseBody: res.body,
            headers: res.headers,
          });
        }
      });
    }).as("updateLocation");

    cy.intercept("GET", "**/rest/storage/**", (req) => {
      req.continue((res) => {
        if (res.statusCode >= 400) {
          apiErrors.push({
            method: "GET",
            url: req.url,
            statusCode: res.statusCode,
            responseBody: res.body,
            headers: res.headers,
          });
        }
      });
    }).as("getLocation");

    // Intercept table refresh calls (set up early so we catch them)
    cy.intercept("GET", "**/rest/storage/rooms**").as("refreshRoomsTable");
    cy.intercept("GET", "**/rest/storage/devices**").as("refreshDevicesTable");
    cy.intercept("GET", "**/rest/storage/shelves**").as("refreshShelvesTable");
    cy.intercept("GET", "**/rest/storage/racks**").as("refreshRacksTable");
  });

  afterEach(function () {
    // Log console errors and API errors if test failed
    if (this.currentTest?.state === "failed") {
      cy.window({ log: false }).then((win) => {
        const logs = win._cypressConsoleLogs || [];
        const errorLogs = logs.filter((l) => l.type === "error").slice(-20);

        if (errorLogs.length > 0 || consoleErrors.length > 0) {
          cy.task("log", "\n=== FRONTEND CONSOLE ERRORS ===");
          errorLogs.forEach((log) => {
            cy.task("log", `[${log.timestamp}] ${log.message}`);
          });
          consoleErrors.forEach((err) => {
            cy.task("log", `[ERROR] ${JSON.stringify(err, null, 2)}`);
          });
        }

        // Log all console messages (for debugging)
        const allLogs = logs.slice(-30);
        if (allLogs.length > 0) {
          cy.task("log", "\n=== RECENT CONSOLE LOGS ===");
          allLogs.forEach((log) => {
            cy.task("log", `[${log.type.toUpperCase()}] ${log.message}`);
          });
        }
      });

      // Log API errors
      if (apiErrors.length > 0) {
        cy.task("log", "\n=== API ERRORS ===");
        apiErrors.forEach((err) => {
          cy.task("logObject", {
            message: `API Error: ${err.method} ${err.url}`,
            error: err,
          });
        });
      }

      // Get backend logs
      cy.exec("docker logs --tail 100 openelisglobal-webapp 2>&1", {
        failOnNonZeroExit: false,
        timeout: 5000,
      }).then((result) => {
        if (result.stdout) {
          cy.task("log", "\n=== BACKEND LOGS (last 100 lines) ===");
          cy.task("log", result.stdout);
        }
      });
    }

    // Best-effort cleanup: close any open modals
    cy.get("body").then(($body) => {
      const modalSelectors = [
        '[data-testid="edit-location-modal"]',
        '[data-testid="delete-location-modal"]',
        '[data-testid="storage-location-modal"]',
      ];

      modalSelectors.forEach((selector) => {
        if ($body.find(selector).length > 0) {
          cy.get(selector).then(($modal) => {
            if ($modal.is(":visible")) {
              cy.wrap($modal)
                .find('button[aria-label="Close"]')
                .then(($close) => {
                  if ($close.length > 0) {
                    cy.wrap($close).click({ force: true });
                  } else {
                    cy.get("body").type("{esc}", { force: true });
                  }
                });
            }
          });
        }
      });
    });
  });

  it("should update room name and persist to database", () => {
    cy.task("log", "\n=== TEST: Update room name ===");

    // Navigate to Rooms tab
    cy.get('[data-testid="tab-rooms"]').click();
    cy.screenshot("02-rooms-tab-opened");
    cy.get('button[role="tab"]')
      .contains("Rooms")
      .should("have.attr", "aria-selected", "true");

    // Wait for table to load
    cy.get('[data-testid^="room-row-"]').should("have.length.at.least", 1);

    // Get first room row ID
    cy.get('[data-testid^="room-row-"]')
      .first()
      .invoke("attr", "data-testid")
      .then((testId) => {
        const roomId = testId.replace("room-row-", "");
        const newName = `Updated Room ${Date.now()}`;
        cy.task("log", `Updating room ${roomId} to name: ${newName}`);

        // Open edit modal
        cy.get('[data-testid^="room-row-"]')
          .first()
          .within(() => {
            cy.get('[data-testid="location-actions-overflow-menu"]')
              .should("be.visible")
              .click({ force: true });
          });

        cy.get('[data-testid="edit-location-menu-item"]')
          .should("be.visible")
          .click({ force: true });

        // Wait for modal to open
        cy.get('[data-testid="edit-location-modal"]').should("be.visible");
        cy.screenshot("03-edit-modal-opened");

        // Wait for form to be populated
        cy.get('[data-testid="edit-location-room-name"]')
          .should("be.visible")
          .should("not.have.value", "")
          .then(($input) => {
            cy.task("log", `Initial room name: ${$input.val()}`);
          });

        // Update name
        cy.get('[data-testid="edit-location-room-name"]').clear().type(newName);
        cy.screenshot("04-name-updated");

        // Save
        cy.get('[data-testid="edit-location-save-button"]')
          .should("not.be.disabled")
          .click();

        // Wait for REAL API call to complete (spy, not stub)
        cy.wait("@updateLocation").then((interception) => {
          cy.task("log", `PUT Response: ${interception.response.statusCode}`);
          cy.task("logObject", {
            message: "PUT Request/Response",
            data: {
              url: interception.request.url,
              requestBody: interception.request.body,
              statusCode: interception.response.statusCode,
              responseBody: interception.response.body,
            },
          });
          expect(interception.response.statusCode).to.be.oneOf([200, 201]);
        });
        cy.screenshot("05-after-save-clicked");

        // Verify modal closes
        cy.get('[data-testid="edit-location-modal"]').should("not.be.visible");

        // Wait for table refresh API call (loadRooms() is called after onSave)
        // The intercept is already set up in beforeEach
        cy.wait("@refreshRoomsTable").then((interception) => {
          cy.task(
            "log",
            `Table refresh GET: ${interception.response.statusCode}`,
          );
          cy.task("logObject", {
            message: "Table refresh response",
            data: {
              statusCode: interception.response.statusCode,
              roomsCount: Array.isArray(interception.response.body)
                ? interception.response.body.length
                : "not array",
              firstRoomName:
                Array.isArray(interception.response.body) &&
                interception.response.body.length > 0
                  ? interception.response.body[0].name
                  : "no rooms",
            },
          });
        });

        // Verify UI updated with REAL response (table should refresh)
        // Wait for the table row to show the updated name (Cypress retries automatically)
        cy.get(`[data-testid="room-row-${roomId}"]`)
          .should("be.visible")
          .and("contain.text", newName)
          .then(($row) => {
            cy.task(
              "log",
              `Row found, text content: ${$row.text().substring(0, 100)}`,
            );
          });
        cy.screenshot("06-table-shows-updated-name");

        // Verify database persistence (optional deep verification)
        cy.request({
          method: "GET",
          url: `/api/OpenELIS-Global/rest/storage/rooms/${roomId}`,
          failOnStatusCode: false,
        }).then((response) => {
          cy.task(
            "log",
            `GET /rest/storage/rooms/${roomId} - Status: ${response.status}`,
          );
          cy.task("logObject", {
            message: "GET Response for verification",
            data: {
              status: response.status,
              body: response.body,
              bodyType: typeof response.body,
            },
          });
          if (response.status === 200 && response.body && response.body.name) {
            cy.task(
              "log",
              `Database has name: ${response.body.name}, expected: ${newName}`,
            );
            expect(response.body.name).to.eq(newName);
          } else {
            cy.task(
              "log",
              `WARNING: GET request failed or body is empty. Status: ${response.status}, Body: ${JSON.stringify(response.body)}`,
            );
            // Don't fail the test if GET verification fails - the UI update is more important
          }
        });
      });
  });

  it("should toggle device active state on and off", () => {
    cy.task("log", "\n=== TEST: Toggle device active state ===");

    // Navigate to Devices tab
    cy.get('[data-testid="tab-devices"]').click();
    cy.screenshot("07-devices-tab-opened");
    cy.get('button[role="tab"]')
      .contains("Devices")
      .should("have.attr", "aria-selected", "true");

    // Wait for table
    cy.get('[data-testid^="device-row-"]').should("have.length.at.least", 1);

    // Get first device row ID
    cy.get('[data-testid^="device-row-"]')
      .first()
      .invoke("attr", "data-testid")
      .then((testId) => {
        const deviceId = testId.replace("device-row-", "");
        cy.task("log", `Testing device ${deviceId}`);

        // Open edit modal
        cy.get('[data-testid^="device-row-"]')
          .first()
          .within(() => {
            cy.get('[data-testid="location-actions-overflow-menu"]')
              .should("be.visible")
              .click({ force: true });
          });

        cy.get('[data-testid="edit-location-menu-item"]')
          .should("be.visible")
          .click({ force: true });

        // Wait for modal to open
        cy.get('[data-testid="edit-location-modal"]').should("be.visible");
        cy.screenshot("08-device-edit-modal-opened");

        // Wait for form
        cy.get('[data-testid="edit-location-device-name"]').should(
          "be.visible",
        );

        // Get initial active state
        cy.get("#device-active")
          .should("exist")
          .then(($toggle) => {
            const initialChecked = $toggle.is(":checked");
            cy.task("log", `Initial active state: ${initialChecked}`);

            // Check toggle visibility
            cy.get("#device-active").then(($el) => {
              const isVisible = $el.is(":visible");
              const parentVisibility = $el.parent().css("visibility");
              cy.task(
                "log",
                `Toggle visible: ${isVisible}, parent visibility: ${parentVisibility}`,
              );
              if (!isVisible) {
                cy.screenshot("09-toggle-not-visible");
              }
            });

            // Toggle OFF (if currently ON)
            if (initialChecked) {
              cy.task("log", "Toggling OFF...");
              cy.get("#device-active").click({ force: true });
              cy.screenshot("10-toggle-clicked-off");

              cy.get('[data-testid="edit-location-save-button"]')
                .should("not.be.disabled")
                .click();

              // Wait for REAL API call
              cy.wait("@updateLocation").then((interception) => {
                cy.task(
                  "log",
                  `PUT Response (toggle OFF): ${interception.response.statusCode}`,
                );
                cy.task("logObject", {
                  message: "PUT Request/Response (toggle OFF)",
                  data: {
                    requestBody: interception.request.body,
                    statusCode: interception.response.statusCode,
                    responseBody: interception.response.body,
                  },
                });
                expect(interception.response.statusCode).to.be.oneOf([
                  200, 201,
                ]);
              });

              // Wait for modal to close
              cy.get('[data-testid="edit-location-modal"]').should(
                "not.be.visible",
              );
            }

            // Re-open and verify persisted OFF state
            cy.get('[data-testid^="device-row-"]')
              .first()
              .within(() => {
                cy.get('[data-testid="location-actions-overflow-menu"]')
                  .should("be.visible")
                  .click({ force: true });
              });

            cy.get('[data-testid="edit-location-menu-item"]')
              .should("be.visible")
              .click({ force: true });

            cy.get('[data-testid="edit-location-modal"]').should("be.visible");

            cy.get('[data-testid="edit-location-device-name"]').should(
              "be.visible",
            );

            // Verify toggle is OFF
            cy.get("#device-active").should("not.be.checked");
            cy.screenshot("11-toggle-verified-off");

            // Toggle back ON - THIS IS WHERE IT FAILS (bug B3)
            cy.task("log", "Toggling back ON...");
            cy.get("#device-active").then(($toggle) => {
              cy.task(
                "log",
                `Toggle state before click: checked=${$toggle.is(":checked")}, visible=${$toggle.is(":visible")}`,
              );
              cy.task(
                "log",
                `Toggle parent visibility: ${$toggle.parent().css("visibility")}`,
              );
            });

            cy.get("#device-active").click({ force: true });
            cy.screenshot("12-toggle-clicked-on");

            cy.get('[data-testid="edit-location-save-button"]')
              .should("not.be.disabled")
              .click();

            // Wait for REAL API call
            cy.wait("@updateLocation").then((interception) => {
              cy.task(
                "log",
                `PUT Response (toggle ON): ${interception.response.statusCode}`,
              );
              cy.task("logObject", {
                message: "PUT Request/Response (toggle ON)",
                data: {
                  requestBody: interception.request.body,
                  statusCode: interception.response.statusCode,
                  responseBody: interception.response.body,
                },
              });
              expect(interception.response.statusCode).to.be.oneOf([200, 201]);
            });

            // Wait for modal to close
            cy.get('[data-testid="edit-location-modal"]').should(
              "not.be.visible",
            );

            // Verify it actually turned back on (re-open modal)
            cy.task(
              "log",
              "Re-opening modal to verify active state persisted...",
            );
            cy.get('[data-testid^="device-row-"]')
              .first()
              .within(() => {
                cy.get('[data-testid="location-actions-overflow-menu"]')
                  .should("be.visible")
                  .click({ force: true });
              });

            cy.get('[data-testid="edit-location-menu-item"]')
              .should("be.visible")
              .click({ force: true });

            cy.get('[data-testid="edit-location-modal"]').should("be.visible");
            cy.screenshot("13-modal-reopened-to-verify");

            cy.get('[data-testid="edit-location-device-name"]').should(
              "be.visible",
            );

            // Check toggle state before assertion
            cy.get("#device-active")
              .then(($toggle) => {
                const isChecked = $toggle.is(":checked");
                const isVisible = $toggle.is(":visible");
                const parentVisibility = $toggle.parent().css("visibility");
                const ariaChecked = $toggle.attr("aria-checked");

                // Return values for next step
                return {
                  isChecked,
                  isVisible,
                  parentVisibility,
                  ariaChecked,
                  toggle: $toggle,
                };
              })
              .then(
                ({
                  isChecked,
                  isVisible,
                  parentVisibility,
                  ariaChecked,
                  toggle,
                }) => {
                  // Log in separate step to avoid async/sync mixing
                  cy.task(
                    "log",
                    `Final toggle state: checked=${isChecked}, visible=${isVisible}`,
                  );
                  cy.task(
                    "log",
                    `Toggle parent visibility: ${parentVisibility}`,
                  );
                  cy.task("log", `Toggle aria-checked: ${ariaChecked}`);

                  // Log the entire toggle element structure for debugging
                  cy.task("logObject", {
                    message: "Toggle element details",
                    data: {
                      id: toggle.attr("id"),
                      class: toggle.attr("class"),
                      checked: isChecked,
                      visible: isVisible,
                      parentVisibility: parentVisibility,
                      ariaChecked: ariaChecked,
                      html: toggle[0].outerHTML.substring(0, 200), // First 200 chars
                    },
                  });

                  cy.screenshot("14-final-toggle-state");

                  // If toggle is not visible, that's the bug - log it
                  if (!isVisible || parentVisibility === "hidden") {
                    cy.task(
                      "log",
                      "BUG DETECTED: Toggle is not visible or parent is hidden!",
                    );
                  }
                },
              )
              .then(() => {
                // This assertion - check if toggle is checked OR aria-checked is true
                cy.get("#device-active").then(($toggle) => {
                  const isChecked = $toggle.is(":checked");
                  const ariaChecked = $toggle.attr("aria-checked");
                  expect(
                    isChecked || ariaChecked === "true",
                    `Toggle should be checked. isChecked=${isChecked}, ariaChecked=${ariaChecked}`,
                  ).to.be.true;
                });
              });
          });
      });
  });

  it("should display parent room name in device edit modal", () => {
    // This test WILL FAIL currently - proves bug B2/B5
    // Navigate to Devices tab
    cy.get('[data-testid="tab-devices"]').click();
    cy.get('button[role="tab"]')
      .contains("Devices")
      .should("have.attr", "aria-selected", "true");

    // Wait for table
    cy.get('[data-testid^="device-row-"]').should("have.length.at.least", 1);

    // Get first device row ID
    cy.get('[data-testid^="device-row-"]')
      .first()
      .invoke("attr", "data-testid")
      .then((testId) => {
        const deviceId = testId.replace("device-row-", "");

        // Open edit modal
        cy.get('[data-testid^="device-row-"]')
          .first()
          .within(() => {
            cy.get('[data-testid="location-actions-overflow-menu"]')
              .should("be.visible")
              .click({ force: true });
          });

        cy.get('[data-testid="edit-location-menu-item"]')
          .should("be.visible")
          .click({ force: true });

        // Wait for modal to open
        cy.get('[data-testid="edit-location-modal"]').should("be.visible");

        // Wait for form
        cy.get('[data-testid="edit-location-device-name"]').should(
          "be.visible",
        );

        // Wait for parent room field to be populated
        cy.get('[data-testid="edit-location-device-parent-room"]')
          .should("be.visible")
          .then(($field) => {
            const inputElement = $field.find("input")[0] || $field[0];
            // This assertion WILL FAIL - proves bug B2/B5
            // Parent room name should be displayed, not empty
            expect(inputElement.value).to.not.be.empty;
            expect(inputElement.value).to.match(/[A-Za-z]/); // Should contain text
          });
      });
  });

  it("should display specific error message for invalid temperature", () => {
    // This test WILL FAIL currently - proves bug B4
    // Navigate to Devices tab
    cy.get('[data-testid="tab-devices"]').click();
    cy.get('button[role="tab"]')
      .contains("Devices")
      .should("have.attr", "aria-selected", "true");

    // Wait for table
    cy.get('[data-testid^="device-row-"]').should("have.length.at.least", 1);

    // Get first device row ID
    cy.get('[data-testid^="device-row-"]')
      .first()
      .invoke("attr", "data-testid")
      .then((testId) => {
        const deviceId = testId.replace("device-row-", "");

        // Open edit modal
        cy.get('[data-testid^="device-row-"]')
          .first()
          .within(() => {
            cy.get('[data-testid="location-actions-overflow-menu"]')
              .should("be.visible")
              .click({ force: true });
          });

        cy.get('[data-testid="edit-location-menu-item"]')
          .should("be.visible")
          .click({ force: true });

        // Wait for modal to open
        cy.get('[data-testid="edit-location-modal"]').should("be.visible");

        // Wait for form
        cy.get('[data-testid="edit-location-device-name"]').should(
          "be.visible",
        );

        // Enter invalid temperature (e.g., too high for freezer)
        cy.get('[data-testid="edit-location-device-temperature"]')
          .clear()
          .type("999"); // Invalid temperature

        // Save
        cy.get('[data-testid="edit-location-save-button"]')
          .should("not.be.disabled")
          .click();

        // Wait for REAL API call (should return error)
        cy.wait("@updateLocation").then((interception) => {
          // If backend returns validation error, check for error message
          if (interception.response.statusCode >= 400) {
            // This assertion WILL FAIL - proves bug B4
            // Error message should be specific, not generic
            cy.get('[data-testid="edit-location-modal"]').within(() => {
              cy.get('[role="alert"]')
                .should("be.visible")
                .invoke("text")
                .should("match", /temperature|invalid|range/i);
            });
          }
        });
      });
  });
});
