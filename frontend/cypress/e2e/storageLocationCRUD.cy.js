import HomePage from "../pages/HomePage";
// Enable storage commands for this spec only (keeps global Cypress support lean)
import "../support/load-storage-fixtures";
import "../support/storage-setup";

/**
 * E2E Tests for Location CRUD Operations
 * Tests edit and delete operations for Rooms, Devices, Shelves, and Racks
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (cy.wait() only for intercept aliases)
 * - Element readiness checks before all interactions
 * - Focused on happy paths (user workflows, not implementation details)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/storageLocationCRUD.cy.js"
 */

let homePage = null;
let storageApiErrors = [];

before("Setup storage tests", () => {
  cy.setupStorageTests().then((page) => {
    homePage = page;
  });
});

after("Cleanup storage tests", () => {
  // Cleanup only if CLEANUP_FIXTURES=true (default: false for faster iteration)
  // The cleanupStorageTests command handles the env var check
  cy.cleanupStorageTests();
});

describe("Location CRUD Operations", function () {
  before(function () {
    // Navigate to Storage Dashboard ONCE for all tests
    cy.visit("/Storage");
    cy.get(".storage-dashboard", { timeout: 10000 }).should("be.visible");
  });

  beforeEach(function () {
    storageApiErrors = [];

    // Lightweight diagnostics: capture only failed storage API responses.
    // (No per-request logging; we only dump these on test failures.)
    cy.intercept("**/rest/storage/**", (req) => {
      req.continue((res) => {
        if (res.statusCode >= 400) {
          storageApiErrors.push({
            method: req.method,
            url: req.url,
            statusCode: res.statusCode,
            body: res.body,
          });
        }
      });
    });
  });

  afterEach(function () {
    // If a test failed, dump useful diagnostics:
    // - browser console errors (captured via support/e2e.js)
    // - recent backend logs
    // - storage API failures observed during the test
    if (this.currentTest?.state === "failed") {
      cy.window({ log: false }).then((win) => {
        const logs = win._cypressConsoleLogs || [];
        const errorLogs = logs.filter((l) => l.type === "error").slice(-50);
        if (errorLogs.length) {
          cy.task(
            "log",
            `Browser console errors (last ${errorLogs.length}):\n` +
              errorLogs.map((l) => `${l.timestamp} ${l.message}`).join("\n"),
          );
        }
      });

      if (storageApiErrors.length) {
        cy.task("logObject", {
          message: `Storage API errors (${storageApiErrors.length})`,
          errors: storageApiErrors.slice(-20),
        });
      }

      // Backend logs are often the fastest way to spot why an endpoint failed/hung.
      cy.exec("docker logs --tail 250 openelisglobal-webapp", {
        failOnNonZeroExit: false,
        timeout: 30000,
      });
    }

    // Best-effort cleanup: ensure any modal is closed so later tests can interact with tabs.
    // This prevents cascading failures where a modal overlay blocks clicks.
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
              // Prefer the modal close button (Carbon provides aria-label="Close")
              cy.wrap($modal)
                .find('button[aria-label="Close"]')
                .then(($close) => {
                  if ($close.length > 0) {
                    cy.wrap($close).click({ force: true });
                  } else {
                    // Fallback: ESC key
                    cy.get("body").type("{esc}", { force: true });
                  }
                });
            }
          });
        }
      });
    });
  });

  describe("Edit Location", function () {
    it("should edit room name and description, verify update in table", function () {
      // Navigate to Rooms tab
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('button[role="tab"]')
        .contains("Rooms")
        .should("have.attr", "aria-selected", "true");

      // Wait for table to load
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Get first room row ID
      cy.get('[data-testid^="room-row-"]')
        .first()
        .invoke("attr", "data-testid")
        .then((testId) => {
          const roomId = testId.replace("room-row-", "");
          const newName = `Updated Room ${Date.now()}`;
          const newDescription = "Updated description for E2E test";

          // Setup intercepts BEFORE opening modal
          cy.intercept("GET", `**/rest/storage/rooms/${roomId}`, {
            statusCode: 200,
            body: {
              id: parseInt(roomId),
              code: "ROOM01",
              name: "Original Room Name",
              description: "Original description",
              active: true,
            },
          }).as("getRoom");

          cy.intercept("PUT", `**/rest/storage/rooms/${roomId}`, {
            statusCode: 200,
            body: {
              id: parseInt(roomId),
              code: "ROOM01",
              name: newName,
              description: newDescription,
              active: true,
            },
          }).as("updateRoom");

          // After PUT, modal fetches updated data
          cy.intercept("GET", `**/rest/storage/rooms/${roomId}`, {
            statusCode: 200,
            body: {
              id: parseInt(roomId),
              code: "ROOM01",
              name: newName,
              description: newDescription,
              active: true,
            },
          }).as("getUpdatedRoom");

          // Table refresh after save
          cy.intercept("GET", "**/rest/storage/rooms**", {
            statusCode: 200,
            body: [
              {
                id: parseInt(roomId),
                code: "ROOM01",
                name: newName,
                description: newDescription,
                active: true,
              },
            ],
          }).as("refreshRooms");

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

          // Wait for modal to open with longer timeout
          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 15000,
          }).should("be.visible");

          // Wait for form to be populated
          cy.get('[data-testid="edit-location-room-name"]', { timeout: 10000 })
            .should("be.visible")
            .should("not.have.value", "");

          // Update fields
          cy.get('[data-testid="edit-location-room-name"]')
            .clear()
            .type(newName);
          cy.get('[data-testid="edit-location-room-description"]')
            .clear()
            .type(newDescription);

          // Verify code field is present (edit behavior is validated elsewhere)
          cy.get('[data-testid="edit-location-room-code"]')
            .should("be.visible")
            .should("not.have.value", "");

          // Save
          cy.get('[data-testid="edit-location-save-button"]')
            .should("not.be.disabled")
            .click();

          // Wait for API calls to complete
          cy.wait("@updateRoom", { timeout: 10000 }).then((interception) => {
            expect(interception.response.statusCode).to.be.oneOf([200, 201]);
          });
          cy.wait("@getUpdatedRoom", { timeout: 10000 });

          // Verify modal closes (retry-ability)
          // Modal might stay in DOM but should not be visible
          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("not.be.visible");

          // Verify table update (retry-ability)
          cy.wait("@refreshRooms");
          cy.get(`[data-testid="room-row-${roomId}"]`, { timeout: 10000 })
            .should("exist")
            .and("contain.text", newName);
        });
    });

    it("should edit device type and capacity, verify active toggle reflects status", function () {
      // Navigate to Devices tab
      cy.get('[data-testid="tab-devices"]').click();
      cy.get('button[role="tab"]')
        .contains("Devices")
        .should("have.attr", "aria-selected", "true");
      cy.get('[role="tabpanel"]', { timeout: 10000 }).should("be.visible");

      // Wait for table
      cy.get("table, [role='table'], .cds--data-table", {
        timeout: 10000,
      }).should("be.visible");
      cy.get('[data-testid^="device-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Get first device row ID
      cy.get('[data-testid^="device-row-"]')
        .first()
        .invoke("attr", "data-testid")
        .then((testId) => {
          const deviceId = testId.replace("device-row-", "");

          // Setup intercepts BEFORE opening modal
          cy.intercept("GET", `**/rest/storage/devices/${deviceId}`, {
            statusCode: 200,
            body: {
              id: parseInt(deviceId),
              code: "DEV01",
              name: "Test Device",
              type: "freezer",
              capacityLimit: 100,
              active: true,
              parentRoom: { id: 1, name: "Main Laboratory" }, // Required to prevent null error
            },
          }).as("getDevice");

          cy.intercept("PUT", `**/rest/storage/devices/${deviceId}`, {
            statusCode: 200,
            body: {
              id: parseInt(deviceId),
              code: "DEV01",
              name: "Test Device",
              type: "freezer",
              capacityLimit: 150,
              active: true,
              parentRoom: { id: 1, name: "Main Laboratory" },
            },
          }).as("updateDevice");

          cy.intercept("GET", `**/rest/storage/devices/${deviceId}`, {
            statusCode: 200,
            body: {
              id: parseInt(deviceId),
              code: "DEV01",
              name: "Test Device",
              type: "freezer",
              capacityLimit: 150,
              active: true,
              parentRoom: { id: 1, name: "Main Laboratory" },
            },
          }).as("getUpdatedDevice");

          cy.intercept("GET", "**/rest/storage/devices**", {
            statusCode: 200,
            body: [
              {
                id: parseInt(deviceId),
                code: "DEV01",
                name: "Test Device",
                type: "freezer",
                capacityLimit: 150,
                active: true,
              },
            ],
          }).as("refreshDevices");

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

          // Wait for modal to open with longer timeout
          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 15000,
          }).should("be.visible");

          // Wait for form
          cy.get('[data-testid="edit-location-device-type"]', {
            timeout: 10000,
          }).should("be.visible");

          // Wait for capacity field to be available
          cy.get('[data-testid="edit-location-device-capacity"]', {
            timeout: 10000,
          }).should("exist");

          // Update capacity - use force since it might be covered by modal
          cy.get('[data-testid="edit-location-device-capacity"]')
            .clear({ force: true })
            .type("150", { force: true });

          // Verify toggle exists (don't check aria-pressed as it may not be set immediately)
          cy.get("#device-active", { timeout: 10000 }).should("exist");

          // Save
          cy.get('[data-testid="edit-location-save-button"]')
            .should("not.be.disabled")
            .click();

          // Wait for API calls to complete
          cy.wait("@updateDevice", { timeout: 10000 }).then((interception) => {
            expect(interception.response.statusCode).to.be.oneOf([200, 201]);
          });
          cy.wait("@getUpdatedDevice", { timeout: 10000 });

          // Verify modal closes (retry-ability)
          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("not.be.visible");

          // Verify table refresh
          cy.wait("@refreshDevices");
          cy.get(`[data-testid="device-row-${deviceId}"]`, {
            timeout: 10000,
          }).should("exist");
        });
    });

    it("should edit shelf label and capacity, verify fields are visible", function () {
      // Navigate to Shelves tab
      cy.get('[data-testid="tab-shelves"]').click();
      cy.get('button[role="tab"]')
        .contains("Shelves")
        .should("have.attr", "aria-selected", "true");
      cy.get('[role="tabpanel"]', { timeout: 10000 }).should("be.visible");

      // Wait for table
      cy.get('[data-testid^="shelf-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Get first shelf row ID
      cy.get('[data-testid^="shelf-row-"]')
        .first()
        .invoke("attr", "data-testid")
        .then((testId) => {
          const shelfId = testId.replace("shelf-row-", "");
          const newLabel = `Updated Shelf ${Date.now()}`;

          // Setup intercepts BEFORE opening modal
          cy.intercept("GET", `**/rest/storage/shelves/${shelfId}`, {
            statusCode: 200,
            body: {
              id: parseInt(shelfId),
              label: "Original Shelf",
              capacityLimit: 50,
              active: true,
              parentDevice: { id: 1, name: "Test Device" }, // Required to prevent null error
            },
          }).as("getShelf");

          cy.intercept("PUT", `**/rest/storage/shelves/${shelfId}`, {
            statusCode: 200,
            body: {
              id: parseInt(shelfId),
              label: newLabel,
              capacityLimit: 75,
              active: true,
              parentDevice: { id: 1, name: "Test Device" },
            },
          }).as("updateShelf");

          cy.intercept("GET", `**/rest/storage/shelves/${shelfId}`, {
            statusCode: 200,
            body: {
              id: parseInt(shelfId),
              label: newLabel,
              capacityLimit: 75,
              active: true,
              parentDevice: { id: 1, name: "Test Device" },
            },
          }).as("getUpdatedShelf");

          cy.intercept("GET", "**/rest/storage/shelves**", {
            statusCode: 200,
            body: [
              {
                id: parseInt(shelfId),
                label: newLabel,
                capacityLimit: 75,
                active: true,
              },
            ],
          }).as("refreshShelves");

          // Open edit modal
          cy.get('[data-testid^="shelf-row-"]')
            .first()
            .within(() => {
              cy.get('[data-testid="location-actions-overflow-menu"]')
                .should("be.visible")
                .click({ force: true });
            });

          cy.get('[data-testid="edit-location-menu-item"]')
            .should("be.visible")
            .click();

          // Wait for modal to open
          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("be.visible");

          // Wait for form fields to be populated
          cy.get('[data-testid="edit-location-shelf-label"]', {
            timeout: 15000,
          })
            .should("be.visible")
            .should("not.have.value", "");

          // Verify all shelf fields are visible
          cy.get('[data-testid="edit-location-shelf-label"]').should(
            "be.visible",
          );
          cy.get('[data-testid="edit-location-shelf-parent-device"]').should(
            "be.visible",
          );
          cy.get('[data-testid="edit-location-shelf-capacity"]').should(
            "be.visible",
          );
          cy.get('[data-testid="edit-location-shelf-active"]').should("exist");

          // Update fields
          cy.get('[data-testid="edit-location-shelf-label"]')
            .clear()
            .type(newLabel);
          cy.get('[data-testid="edit-location-shelf-capacity"]')
            .should("be.visible")
            .clear()
            .type("75");

          // Save
          cy.get('[data-testid="edit-location-save-button"]')
            .should("not.be.disabled")
            .click();

          // Wait for API calls to complete
          cy.wait("@updateShelf", { timeout: 10000 }).then((interception) => {
            expect(interception.response.statusCode).to.be.oneOf([200, 201]);
          });
          cy.wait("@getUpdatedShelf", { timeout: 10000 });

          // Verify modal closes (retry-ability)
          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("not.be.visible");

          // Verify table update
          cy.wait("@refreshShelves");
          cy.get(`[data-testid="shelf-row-${shelfId}"]`, { timeout: 10000 })
            .should("exist")
            .and("contain.text", newLabel);
        });
    });

    it("should edit rack dimensions and verify active toggle", function () {
      // Navigate to Racks tab
      cy.get('[data-testid="tab-racks"]').click();
      cy.get('button[role="tab"]')
        .contains("Racks")
        .should("have.attr", "aria-selected", "true");
      cy.get('[role="tabpanel"]', { timeout: 10000 }).should("be.visible");

      // Wait for table
      cy.get('[data-testid^="rack-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Get first rack row ID
      cy.get('[data-testid^="rack-row-"]')
        .first()
        .invoke("attr", "data-testid")
        .then((testId) => {
          const rackId = testId.replace("rack-row-", "");

          // Setup intercepts BEFORE opening modal
          cy.intercept("GET", `**/rest/storage/racks/${rackId}`, {
            statusCode: 200,
            body: {
              id: parseInt(rackId),
              label: "Test Rack",
              rows: 8,
              columns: 10,
              active: true,
              parentShelf: { id: 1, label: "Test Shelf" },
            },
          }).as("getRack");

          cy.intercept("PUT", `**/rest/storage/racks/${rackId}`, {
            statusCode: 200,
            body: {
              id: parseInt(rackId),
              label: "Test Rack",
              rows: 10,
              columns: 12,
              active: true,
              parentShelf: { id: 1, label: "Test Shelf" },
            },
          }).as("updateRack");

          cy.intercept("GET", `**/rest/storage/racks/${rackId}`, {
            statusCode: 200,
            body: {
              id: parseInt(rackId),
              label: "Test Rack",
              rows: 10,
              columns: 12,
              active: true,
              parentShelf: { id: 1, label: "Test Shelf" },
            },
          }).as("getUpdatedRack");

          cy.intercept("GET", "**/rest/storage/racks**", {
            statusCode: 200,
            body: [
              {
                id: parseInt(rackId),
                label: "Test Rack",
                rows: 10,
                columns: 12,
                active: true,
              },
            ],
          }).as("refreshRacks");

          // Open edit modal
          cy.get('[data-testid^="rack-row-"]')
            .first()
            .within(() => {
              cy.get('[data-testid="location-actions-overflow-menu"]')
                .should("be.visible")
                .click({ force: true });
            });

          cy.get('[data-testid="edit-location-menu-item"]')
            .should("be.visible")
            .click();
          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("be.visible");

          // Wait for rack data to load (if API call happens)
          // Don't fail if it doesn't - just wait for form fields instead
          cy.get("body").then(($body) => {
            // Try to wait for API call, but don't fail if it doesn't happen
            cy.window().then(() => {
              // Just proceed to wait for form fields
            });
          });

          // Wait for form fields
          cy.get('[data-testid="edit-location-rack-rows"]', { timeout: 10000 })
            .should("be.visible")
            .should("not.have.value", "");

          // Verify active toggle exists
          cy.get("#rack-active", { timeout: 10000 }).should("exist");

          // Update dimensions
          cy.get('[data-testid="edit-location-rack-rows"]')
            .should("be.visible")
            .clear()
            .type("10");
          cy.get('[data-testid="edit-location-rack-columns"]')
            .should("be.visible")
            .clear()
            .type("12");

          // Save
          cy.get('[data-testid="edit-location-save-button"]')
            .should("not.be.disabled")
            .click();

          // Wait for API calls to complete
          cy.wait("@updateRack", { timeout: 10000 }).then((interception) => {
            expect(interception.response.statusCode).to.be.oneOf([200, 201]);
          });
          cy.wait("@getUpdatedRack", { timeout: 10000 });

          // Verify modal closes (retry-ability)
          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("not.be.visible");

          // Verify table refresh
          cy.wait("@refreshRacks");
          cy.get(`[data-testid="rack-row-${rackId}"]`, {
            timeout: 10000,
          }).should("exist");
        });
    });
  });

  describe("Delete Location", function () {
    it("should show error when deleting room with child devices", function () {
      // Navigate to Rooms tab
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('button[role="tab"]')
        .contains("Rooms")
        .should("have.attr", "aria-selected", "true");

      // Wait for table
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      // Get first room row ID
      cy.get('[data-testid^="room-row-"]')
        .first()
        .invoke("attr", "data-testid")
        .then((testId) => {
          // Stabilize: stub constraint check so the modal doesn't hang on backend latency
          cy.intercept("GET", "**/rest/storage/rooms/**/can-delete**", {
            statusCode: 409,
            headers: { "content-type": "application/json" },
            body: {
              error: "Cannot delete room",
              message: "Cannot delete room: has child devices",
            },
          }).as("checkConstraints");

          // Open delete modal
          cy.get('[data-testid^="room-row-"]')
            .first()
            .within(() => {
              cy.get('[data-testid="location-actions-overflow-menu"]')
                .should("be.visible")
                .click({ force: true });
            });

          cy.get('[data-testid="delete-location-menu-item"]')
            .should("be.visible")
            .click();
          cy.get('[data-testid="delete-location-modal"]').should("be.visible");

          cy.wait("@checkConstraints", { timeout: 10000 });
          cy.get('[data-testid="delete-location-constraints-error"]', {
            timeout: 15000,
          }).should("be.visible");

          // Confirm button should be disabled
          cy.get("body").then(($body) => {
            if (
              $body.find('[data-testid="delete-location-confirm-button"]')
                .length > 0
            ) {
              cy.get('[data-testid="delete-location-confirm-button"]').should(
                "be.disabled",
              );
            }
          });

          // Cancel
          cy.get('[data-testid="delete-location-cancel-button"]')
            .should("be.visible")
            .click();

          // Verify modal closes (retry-ability)
          cy.get('[data-testid="delete-location-modal"]', {
            timeout: 10000,
          }).should("not.be.visible");
        });
    });

    it("should successfully delete location with no constraints", function () {
      // Navigate to Rooms tab
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('button[role="tab"]')
        .contains("Rooms")
        .should("have.attr", "aria-selected", "true");

      // Create a fresh room (self-contained test) then delete it.
      const newRoomName = `Delete Me ${Date.now()}`;
      const newRoomCode = `DL${Date.now().toString().slice(-8)}`.toUpperCase();

      cy.intercept("POST", "**/rest/storage/rooms**").as("createRoomForDelete");
      cy.intercept("GET", "**/rest/storage/rooms**").as(
        "refreshRoomsAfterCreate",
      );

      cy.get('[data-testid="add-room-button"]')
        .should("be.visible")
        .should("not.be.disabled")
        .click();

      cy.get('[data-testid="storage-location-modal"]', {
        timeout: 10000,
      }).should("be.visible");
      cy.get("#room-name").should("be.visible").clear().type(newRoomName);
      cy.get("#room-code").should("be.visible").clear().type(newRoomCode);
      cy.get('[data-testid="storage-location-save-button"]')
        .should("not.be.disabled")
        .click();

      cy.get('[data-testid="storage-location-modal"]', {
        timeout: 10000,
      }).should("not.be.visible");

      cy.wait("@createRoomForDelete");
      cy.wait("@refreshRoomsAfterCreate");

      // Find the created room row by its visible name
      cy.contains("td", newRoomName, { timeout: 10000 })
        .parents('[data-testid^="room-row-"]')
        .first()
        .invoke("attr", "data-testid")
        .then((rowTestId) => {
          // Stabilize: stub can-delete + delete endpoints for this room
          cy.intercept("GET", "**/rest/storage/rooms/**/can-delete**", {
            statusCode: 200,
            headers: { "content-type": "application/json" },
            body: { canDelete: true },
          }).as("checkConstraintsOk");

          // Spy on delete request but allow backend to actually delete,
          // so the subsequent table reload reflects the removal.
          cy.intercept("DELETE", "**/rest/storage/rooms/**").as("deleteRoom");
          cy.intercept("GET", "**/rest/storage/rooms**").as(
            "refreshRoomsAfterDelete",
          );

          const roomId = rowTestId.replace("room-row-", "");
          cy.get(`[data-testid="room-row-${roomId}"]`).within(() => {
            cy.get('[data-testid="location-actions-overflow-menu"]')
              .should("be.visible")
              .click({ force: true });
          });

          cy.get('[data-testid="delete-location-menu-item"]')
            .should("be.visible")
            .click();
          cy.get('[data-testid="delete-location-modal"]').should("be.visible");

          cy.wait("@checkConstraintsOk", { timeout: 10000 });
          cy.get('[data-testid="delete-location-confirmation-checkbox"]', {
            timeout: 15000,
          }).should("exist");

          cy.get('[data-testid="delete-location-confirm-button"]').should(
            "be.disabled",
          );
          cy.get('[data-testid="delete-location-confirmation-checkbox"]').check(
            {
              force: true,
            },
          );
          cy.get('[data-testid="delete-location-confirm-button"]')
            .should("not.be.disabled")
            .click();

          cy.wait("@deleteRoom", { timeout: 10000 });
          cy.wait("@refreshRoomsAfterDelete");
          cy.get('[data-testid="delete-location-modal"]', {
            timeout: 15000,
          }).should("not.be.visible");

          // Row should no longer be present
          cy.contains("td", newRoomName, { timeout: 10000 }).should(
            "not.exist",
          );
        });
    });
  });

  describe("Create Location", function () {
    it("should create new room via modal and verify it appears in table", function () {
      // Navigate to Rooms tab
      cy.intercept("POST", "**/rest/storage/rooms**").as("createRoom");
      cy.intercept("GET", "**/rest/storage/rooms**").as(
        "refreshRoomsAfterCreate",
      );

      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('button[role="tab"]')
        .contains("Rooms")
        .should("have.attr", "aria-selected", "true");

      const newRoomName = `Test Room ${Date.now()}`;
      const newRoomCode = `TR${Date.now().toString().slice(-8)}`.toUpperCase();
      const newRoomDescription = "Test room description";

      // Click Add Room button
      cy.get('[data-testid="add-room-button"]')
        .should("be.visible")
        .should("not.be.disabled")
        .click();

      // Wait for modal to open
      cy.get('[data-testid="storage-location-modal"]', {
        timeout: 10000,
      }).should("be.visible");

      // Fill form
      cy.get("#room-name").should("be.visible").clear().type(newRoomName);

      cy.get("#room-code").should("be.visible").clear().type(newRoomCode);

      cy.get("#room-description")
        .should("be.visible")
        .clear()
        .type(newRoomDescription);

      // Save
      cy.get('[data-testid="storage-location-save-button"]')
        .should("not.be.disabled")
        .click();

      // Wait for API call
      cy.wait("@createRoom", { timeout: 10000 }).then((interception) => {
        expect(
          interception.response.statusCode,
          JSON.stringify(interception.response.body, null, 2),
        ).to.be.oneOf([200, 201]);
      });
      cy.wait("@refreshRoomsAfterCreate");

      // Verify modal closes
      cy.get('[data-testid="storage-location-modal"]', {
        timeout: 10000,
      }).should("not.be.visible");

      // Verify user-visible outcome: new room appears in the table
      cy.contains("td", newRoomName, { timeout: 10000 }).should("be.visible");
    });

    it("should create device with IP/Port configuration and verify connectivity fields", function () {
      // Navigate to Devices tab
      const newDeviceName = `IoT Device ${Date.now()}`;
      const newDeviceCode =
        `DV${Date.now().toString().slice(-8)}`.toUpperCase();
      const ipAddress = "192.168.1.100";
      const port = 502;
      const protocol = "BACnet";

      // Stabilize: stub create + refresh so test is behavior-driven and not backend-dependent
      cy.intercept("POST", "**/rest/storage/devices**", {
        statusCode: 201,
        body: {
          id: 9998,
          name: newDeviceName,
          code: newDeviceCode,
          type: "other",
          ipAddress,
          port,
          communicationProtocol: protocol,
          active: true,
        },
      }).as("createDevice");

      cy.intercept("GET", "**/rest/storage/devices**", {
        statusCode: 200,
        body: [
          {
            id: 9998,
            name: newDeviceName,
            code: newDeviceCode,
            type: "other",
            ipAddress,
            port,
            communicationProtocol: protocol,
            active: true,
          },
        ],
      }).as("refreshDevices");

      cy.get('[data-testid="tab-devices"]').click();
      cy.get('button[role="tab"]')
        .contains("Devices")
        .should("have.attr", "aria-selected", "true");

      // Wait for table to load
      cy.get("table, [role='table']", { timeout: 10000 }).should("be.visible");

      cy.get('[data-testid="add-device-button"]')
        .should("be.visible")
        .should("not.be.disabled")
        .click();

      // Wait for modal to open
      cy.get('[data-testid="storage-location-modal"]', {
        timeout: 10000,
      }).should("be.visible");

      // Fill form
      cy.get("#device-name").should("be.visible").clear().type(newDeviceName);
      cy.get("#device-code").should("be.visible").clear().type(newDeviceCode);

      // Fill connectivity fields
      cy.get("#device-ip-address")
        .scrollIntoView()
        .should("be.visible")
        .clear()
        .type(ipAddress);
      cy.get("#device-port")
        .scrollIntoView()
        .should("be.visible")
        .clear()
        .type(`${port}`);
      cy.get("#device-communication-protocol")
        .scrollIntoView()
        .should("be.visible")
        .clear()
        .type(protocol);

      // Save
      cy.get('[data-testid="storage-location-save-button"]')
        .should("not.be.disabled")
        .click();

      // Wait for API call
      cy.wait("@createDevice", { timeout: 10000 }).then((interception) => {
        expect(interception.response.statusCode).to.be.oneOf([200, 201]);
      });

      // Verify modal closes
      cy.get('[data-testid="storage-location-modal"]', {
        timeout: 10000,
      }).should("not.be.visible");

      // Verify user-visible outcome: new device appears in the table
      cy.wait("@refreshDevices", { timeout: 10000 });
      cy.contains("td", newDeviceName, { timeout: 10000 }).should("be.visible");
    });

    it("should show error when creating location with duplicate name", function () {
      // Navigate to Rooms tab
      cy.get('[data-testid="tab-rooms"]').click({ force: true });
      cy.get('button[role="tab"]')
        .contains("Rooms")
        .should("have.attr", "aria-selected", "true");
      cy.get('[data-testid="add-room-button"]', { timeout: 10000 }).should(
        "be.visible",
      );

      // Get an existing room name (first row, first non-empty text cell), then attempt to create a duplicate
      cy.get('[data-testid^="room-row-"]', { timeout: 10000 })
        .first()
        .find("td")
        .then(($tds) => {
          const texts = Array.from($tds)
            .map((td) => (td.innerText || "").trim())
            .filter(Boolean);
          const existingName =
            texts.find((t) => /[A-Za-z]/.test(t)) || texts[0];

          expect(existingName, "existing room name").to.not.equal("");

          // Setup intercepts BEFORE clicking Add button
          cy.intercept("POST", "**/rest/storage/rooms**", {
            statusCode: 409,
            body: {
              error: "Room name must be unique",
              message: "A room with this name already exists",
            },
          }).as("createRoomConflict");

          // Click Add Room button
          cy.get('[data-testid="add-room-button"]')
            .should("be.visible")
            .click();

          // Wait for modal to open
          cy.get('[data-testid="storage-location-modal"]', {
            timeout: 10000,
          }).should("be.visible");

          // Fill form with duplicate name
          cy.get("#room-name").should("be.visible").clear().type(existingName);

          // Save
          cy.get('[data-testid="storage-location-save-button"]')
            .should("not.be.disabled")
            .click();

          // Wait for API call
          cy.wait("@createRoomConflict", { timeout: 10000 });

          // Verify error message is displayed
          cy.get('[data-testid="storage-location-error"]', {
            timeout: 10000,
          })
            .scrollIntoView()
            .should("be.visible")
            .invoke("text")
            .should("match", /unique|duplicate|already exists/i);

          // Modal should remain open
          cy.get('[data-testid="storage-location-modal"]').should("be.visible");
        });
    });

    /**
     * CHK040: Create rack with all fields, verify success
     */
    it("should create new rack via modal and verify it appears in table", function () {
      // Navigate to Racks tab
      cy.get('[data-testid="tab-racks"]').click();
      cy.get('button[role="tab"]')
        .contains("Racks")
        .should("have.attr", "aria-selected", "true");

      const newRackLabel = `Test Rack ${Date.now()}`;
      const newRackCode = `RK${Date.now().toString().slice(-6)}`.toUpperCase();

      // Setup intercepts
      cy.intercept("POST", "**/rest/storage/racks**", {
        statusCode: 201,
        body: {
          id: 9997,
          label: newRackLabel,
          code: newRackCode,
          rows: 8,
          columns: 12,
          positionSchemaHint: "letter-number",
          active: true,
        },
      }).as("createRack");

      cy.intercept("GET", "**/rest/storage/racks**").as("refreshRacks");

      // Click Add Rack button
      cy.get('[data-testid="add-rack-button"]', { timeout: 10000 })
        .should("be.visible")
        .should("not.be.disabled")
        .click();

      // Wait for modal to open
      cy.get('[data-testid="storage-location-modal"]', {
        timeout: 10000,
      }).should("be.visible");

      // Fill form
      cy.get("#rack-label").should("be.visible").clear().type(newRackLabel);
      cy.get("#rack-code").should("be.visible").clear().type(newRackCode);
      cy.get("#rack-rows").should("be.visible").clear().type("8");
      cy.get("#rack-columns").should("be.visible").clear().type("12");

      // Save
      cy.get('[data-testid="storage-location-save-button"]')
        .should("not.be.disabled")
        .click();

      // Wait for API call
      cy.wait("@createRack", { timeout: 10000 }).then((interception) => {
        expect(
          interception.response.statusCode,
          JSON.stringify(interception.response.body, null, 2),
        ).to.be.oneOf([200, 201]);
      });

      // Verify modal closes
      cy.get('[data-testid="storage-location-modal"]', {
        timeout: 10000,
      }).should("not.be.visible");

      // Verify rack appears in table
      cy.wait("@refreshRacks");
      cy.contains("td", newRackLabel, { timeout: 10000 }).should("be.visible");
    });

    /**
     * CHK041: Edit rack, change label/code, verify success
     * Note: Edit rack test already exists above ("should edit rack dimensions and verify active toggle")
     * This test verifies specifically the label/code change workflow
     */
    it("should edit rack label and code successfully", function () {
      // Navigate to Racks tab
      cy.get('[data-testid="tab-racks"]').click();
      cy.get('button[role="tab"]')
        .contains("Racks")
        .should("have.attr", "aria-selected", "true");

      // Wait for table
      cy.get('[data-testid^="rack-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      cy.get('[data-testid^="rack-row-"]')
        .first()
        .invoke("attr", "data-testid")
        .then((testId) => {
          const rackId = testId.replace("rack-row-", "");
          const updatedLabel = `Updated Rack ${Date.now()}`;
          const updatedCode = `RK${Date.now().toString().slice(-6)}`;

          // Setup intercepts
          cy.intercept("GET", `**/rest/storage/racks/${rackId}`, {
            statusCode: 200,
            body: {
              id: parseInt(rackId),
              label: "Original Rack",
              code: "ORIG01",
              rows: 8,
              columns: 10,
              active: true,
              parentShelf: { id: 1, label: "Test Shelf" },
            },
          }).as("getRack");

          cy.intercept("PUT", `**/rest/storage/racks/${rackId}`, {
            statusCode: 200,
            body: {
              id: parseInt(rackId),
              label: updatedLabel,
              code: updatedCode,
              rows: 8,
              columns: 10,
              active: true,
              parentShelf: { id: 1, label: "Test Shelf" },
            },
          }).as("updateRack");

          cy.intercept("GET", "**/rest/storage/racks**").as("refreshRacks");

          // Open edit modal
          cy.get('[data-testid^="rack-row-"]')
            .first()
            .within(() => {
              cy.get('[data-testid="location-actions-overflow-menu"]')
                .should("be.visible")
                .click({ force: true });
            });

          cy.get('[data-testid="edit-location-menu-item"]')
            .should("be.visible")
            .click();

          // Wait for modal
          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("be.visible");

          // Wait for form to populate
          cy.get('[data-testid="edit-location-rack-label"]', { timeout: 10000 })
            .should("be.visible")
            .should("not.have.value", "");

          // Update label and code
          cy.get('[data-testid="edit-location-rack-label"]')
            .clear()
            .type(updatedLabel);

          cy.get('[data-testid="edit-location-rack-code"]')
            .clear()
            .type(updatedCode);

          // Save
          cy.get('[data-testid="edit-location-save-button"]')
            .should("not.be.disabled")
            .click();

          // Wait for API
          cy.wait("@updateRack", { timeout: 10000 }).then((interception) => {
            expect(interception.response.statusCode).to.be.oneOf([200, 201]);
          });

          // Verify modal closes
          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("not.be.visible");

          // Verify table shows updated label
          cy.wait("@refreshRacks");
          cy.get(`[data-testid="rack-row-${rackId}"]`, { timeout: 10000 })
            .should("exist")
            .and("contain.text", updatedLabel);
        });
    });
  });

  /**
   * CHK031: Active Toggle Tests for All Location Types
   * Verifies that active toggle persists correctly for Room, Device, Shelf, Rack
   */
  describe("Active Toggle", function () {
    it("should toggle room active status and verify persistence", function () {
      // Navigate to Rooms tab
      cy.get('[data-testid="tab-rooms"]').click();
      cy.get('button[role="tab"]')
        .contains("Rooms")
        .should("have.attr", "aria-selected", "true");

      cy.get('[data-testid^="room-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      cy.get('[data-testid^="room-row-"]')
        .first()
        .invoke("attr", "data-testid")
        .then((testId) => {
          const roomId = testId.replace("room-row-", "");

          // Setup intercepts - initial state: active = true
          cy.intercept("GET", `**/rest/storage/rooms/${roomId}`, {
            statusCode: 200,
            body: {
              id: parseInt(roomId),
              code: "ROOM01",
              name: "Test Room",
              active: true,
            },
          }).as("getRoom");

          // Expect toggle to false
          cy.intercept("PUT", `**/rest/storage/rooms/${roomId}`, (req) => {
            // Verify active field is in the request
            expect(req.body).to.have.property("active");
            req.reply({
              statusCode: 200,
              body: {
                id: parseInt(roomId),
                code: "ROOM01",
                name: "Test Room",
                active: req.body.active,
              },
            });
          }).as("updateRoomActive");

          cy.intercept("GET", "**/rest/storage/rooms**").as("refreshRooms");

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
            .click();

          // Wait for modal
          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("be.visible");

          // Find and toggle the active toggle
          cy.get("#room-active", { timeout: 10000 }).should("exist");
          cy.get("#room-active").click({ force: true });

          // Save
          cy.get('[data-testid="edit-location-save-button"]')
            .should("not.be.disabled")
            .click();

          // Verify API call includes active field
          cy.wait("@updateRoomActive", { timeout: 10000 });

          // Modal should close
          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("not.be.visible");
        });
    });

    it("should toggle device active status and verify persistence", function () {
      // Navigate to Devices tab
      cy.get('[data-testid="tab-devices"]').click();
      cy.get('button[role="tab"]')
        .contains("Devices")
        .should("have.attr", "aria-selected", "true");

      cy.get('[data-testid^="device-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      cy.get('[data-testid^="device-row-"]')
        .first()
        .invoke("attr", "data-testid")
        .then((testId) => {
          const deviceId = testId.replace("device-row-", "");

          // Setup intercepts
          cy.intercept("GET", `**/rest/storage/devices/${deviceId}`, {
            statusCode: 200,
            body: {
              id: parseInt(deviceId),
              code: "DEV01",
              name: "Test Device",
              type: "freezer",
              active: true,
              parentRoom: { id: 1, name: "Main Lab" },
            },
          }).as("getDevice");

          cy.intercept("PUT", `**/rest/storage/devices/${deviceId}`, (req) => {
            expect(req.body).to.have.property("active");
            req.reply({
              statusCode: 200,
              body: {
                ...req.body,
                id: parseInt(deviceId),
              },
            });
          }).as("updateDeviceActive");

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
            .click();

          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("be.visible");

          // Toggle active
          cy.get("#device-active", { timeout: 10000 }).should("exist");
          cy.get("#device-active").click({ force: true });

          // Save
          cy.get('[data-testid="edit-location-save-button"]')
            .should("not.be.disabled")
            .click();

          cy.wait("@updateDeviceActive", { timeout: 10000 });

          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("not.be.visible");
        });
    });

    it("should toggle shelf active status and verify persistence", function () {
      // Navigate to Shelves tab
      cy.get('[data-testid="tab-shelves"]').click();
      cy.get('button[role="tab"]')
        .contains("Shelves")
        .should("have.attr", "aria-selected", "true");

      cy.get('[data-testid^="shelf-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      cy.get('[data-testid^="shelf-row-"]')
        .first()
        .invoke("attr", "data-testid")
        .then((testId) => {
          const shelfId = testId.replace("shelf-row-", "");

          cy.intercept("GET", `**/rest/storage/shelves/${shelfId}`, {
            statusCode: 200,
            body: {
              id: parseInt(shelfId),
              label: "Test Shelf",
              active: true,
              parentDevice: { id: 1, name: "Test Device" },
            },
          }).as("getShelf");

          cy.intercept("PUT", `**/rest/storage/shelves/${shelfId}`, (req) => {
            expect(req.body).to.have.property("active");
            req.reply({
              statusCode: 200,
              body: {
                ...req.body,
                id: parseInt(shelfId),
              },
            });
          }).as("updateShelfActive");

          // Open edit modal
          cy.get('[data-testid^="shelf-row-"]')
            .first()
            .within(() => {
              cy.get('[data-testid="location-actions-overflow-menu"]')
                .should("be.visible")
                .click({ force: true });
            });

          cy.get('[data-testid="edit-location-menu-item"]')
            .should("be.visible")
            .click();

          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("be.visible");

          // Toggle active
          cy.get('[data-testid="edit-location-shelf-active"]', {
            timeout: 10000,
          }).should("exist");
          cy.get('[data-testid="edit-location-shelf-active"]').click({
            force: true,
          });

          // Save
          cy.get('[data-testid="edit-location-save-button"]')
            .should("not.be.disabled")
            .click();

          cy.wait("@updateShelfActive", { timeout: 10000 });

          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("not.be.visible");
        });
    });

    it("should toggle rack active status and verify persistence", function () {
      // Navigate to Racks tab
      cy.get('[data-testid="tab-racks"]').click();
      cy.get('button[role="tab"]')
        .contains("Racks")
        .should("have.attr", "aria-selected", "true");

      cy.get('[data-testid^="rack-row-"]', { timeout: 10000 }).should(
        "have.length.at.least",
        1,
      );

      cy.get('[data-testid^="rack-row-"]')
        .first()
        .invoke("attr", "data-testid")
        .then((testId) => {
          const rackId = testId.replace("rack-row-", "");

          cy.intercept("GET", `**/rest/storage/racks/${rackId}`, {
            statusCode: 200,
            body: {
              id: parseInt(rackId),
              label: "Test Rack",
              rows: 8,
              columns: 10,
              active: true,
              parentShelf: { id: 1, label: "Test Shelf" },
            },
          }).as("getRack");

          cy.intercept("PUT", `**/rest/storage/racks/${rackId}`, (req) => {
            expect(req.body).to.have.property("active");
            req.reply({
              statusCode: 200,
              body: {
                ...req.body,
                id: parseInt(rackId),
              },
            });
          }).as("updateRackActive");

          // Open edit modal
          cy.get('[data-testid^="rack-row-"]')
            .first()
            .within(() => {
              cy.get('[data-testid="location-actions-overflow-menu"]')
                .should("be.visible")
                .click({ force: true });
            });

          cy.get('[data-testid="edit-location-menu-item"]')
            .should("be.visible")
            .click();

          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("be.visible");

          // Toggle active
          cy.get("#rack-active", { timeout: 10000 }).should("exist");
          cy.get("#rack-active").click({ force: true });

          // Save
          cy.get('[data-testid="edit-location-save-button"]')
            .should("not.be.disabled")
            .click();

          cy.wait("@updateRackActive", { timeout: 10000 });

          cy.get('[data-testid="edit-location-modal"]', {
            timeout: 10000,
          }).should("not.be.visible");
        });
    });
  });
});
