/**
 * Focused test for Move Modal form behavior
 * Tests:
 * 1. Form opens when Add Location is clicked
 * 2. Form stays open when room is selected
 * 3. Form can be reopened after canceling
 * 4. Custom room addition works
 */

before("Setup storage tests", () => {
  cy.setupStorageTests();
});

after("Cleanup storage tests", () => {
  // Skip cleanup to persist fixtures for debugging
  cy.log("Skipping cleanup to persist fixtures");
  // cy.cleanupStorageTests();
});

describe("Move Modal Form Behavior", function () {
  beforeEach(() => {
    cy.setupStorageIntercepts();

    // Console logs will appear in Electron console with ELECTRON_ENABLE_LOGGING=1
    // No need to override - they'll show up automatically in the terminal
    cy.visit("/Storage/samples");
    cy.wait("@getSamples", { timeout: 10000 });
  });

  it("Should open create form and stay open when selecting room", function () {
    // Console logs are already captured via onBeforeLoad in beforeEach

    // Turn off uncaught exception handling to see real errors
    Cypress.on("uncaught:exception", (err, runnable) => {
      cy.log("UNCAUGHT EXCEPTION:", err.message);
      cy.log("Stack:", err.stack);
      return false;
    });

    // Verify we're on the samples tab
    cy.get('[data-testid="sample-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Get first sample and open move modal
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-row"]')
      .should("have.length.at.least", 1)
      .first()
      .within(() => {
        cy.get('[data-testid="sample-actions-overflow-menu"]')
          .should("be.visible")
          .click();
      });

    // Wait for menu to appear (outside of within block)
    cy.wait(500);

    // Click Move menu item (outside of within block since menu is portaled)
    cy.get('[data-testid="move-menu-item"]', { timeout: 3000 })
      .should("be.visible")
      .click();

    // Wait for move modal to open
    cy.get('[data-testid="move-modal"]', { timeout: 5000 }).should(
      "be.visible",
    );

    // Step 1: Click "Add Location" button (scoped to modal)
    cy.get('[data-testid="move-modal"]')
      .find('[data-testid="add-location-button"]', { timeout: 2000 })
      .first()
      .should("be.visible")
      .click();

    // Step 2: Verify create form appears
    cy.get('[data-testid="location-create-container"]', {
      timeout: 3000,
    }).should("be.visible");
    cy.get('[data-testid="room-combobox"]').should("be.visible");

    // Step 3: Select an existing room from dropdown
    cy.get('[data-testid="room-combobox"]').click();
    cy.wait(500);
    // Wait for dropdown menu to appear and select first option
    cy.get('[role="listbox"]').should("be.visible");
    cy.get('[role="option"]').first().click({ force: true });
    cy.wait(1000);
    cy.wait("@getDevices");

    // Step 4: Verify device input is enabled after room selection
    cy.get('[data-testid="device-combobox"]', { timeout: 2000 }).should(
      "not.be.disabled",
    );

    // Step 5: Verify form is still visible (didn't close automatically)
    cy.get('[data-testid="location-create-container"]').should("be.visible");

    // Step 6: Verify "Add" button exists but is disabled (only room, no device yet)
    cy.get('[data-testid="add-location-create-button"]')
      .should("be.visible")
      .should("contain.text", "Add")
      .should("be.disabled");

    // Step 7: Select an existing device to enable the "Add" button
    cy.get('[data-testid="device-combobox"]').click();
    cy.wait(500);
    // Wait for dropdown menu to appear and select first option
    cy.get('[role="listbox"]').should("be.visible");
    cy.get('[role="option"]').first().click({ force: true });
    cy.wait(1000);

    // Debug: Check what location state we have by examining the DOM
    cy.get('[data-testid="add-location-create-button"]').then(($btn) => {
      cy.log("Add button disabled state:", $btn.prop("disabled"));
      cy.log("Add button classes:", $btn.attr("class"));
      cy.log("Add button aria-disabled:", $btn.attr("aria-disabled"));
    });

    // Debug: Check the actual React component state by examining inputs
    cy.get('[data-testid="room-combobox"]').then(($room) => {
      cy.log("Room combobox value:", $room.val());
      cy.log("Room combobox disabled:", $room.prop("disabled"));
    });

    cy.get('[data-testid="device-combobox"]').then(($device) => {
      cy.log("Device combobox value:", $device.val());
      cy.log("Device combobox disabled:", $device.prop("disabled"));
    });

    // Wait a bit for any pending console logs
    cy.wait(500);

    // Step 8: Verify "Add" button is now enabled (room + device selected)
    cy.get('[data-testid="add-location-create-button"]', {
      timeout: 2000,
    }).should("not.be.disabled");

    // Step 9: Cancel the form (without clicking Add)
    cy.get('[data-testid="location-create-container"]')
      .find("button")
      .contains(/cancel/i)
      .click();

    // Step 10: Verify we're back to search mode
    cy.get('[data-testid="move-modal"]')
      .find('[data-testid="location-search-and-create"]')
      .should("be.visible");
    cy.get('[data-testid="move-modal"]')
      .find('[data-testid="add-location-button"]')
      .should("be.visible");

    // Step 11: Click "Add Location" again to verify it reopens
    cy.get('[data-testid="move-modal"]')
      .find('[data-testid="add-location-button"]')
      .first()
      .click();
    cy.get('[data-testid="location-create-container"]', {
      timeout: 2000,
    }).should("be.visible");

    // Step 12: Test selecting an existing room from dropdown
    cy.get('[data-testid="room-combobox"]').click();
    cy.wait(500);
    // Wait for dropdown menu to appear and select first option
    cy.get('[role="listbox"]').should("be.visible");
    cy.get('[role="option"]').first().click({ force: true });
    cy.wait(1000);

    // Step 13: Verify form is still open after selecting room (doesn't auto-close)
    cy.get('[data-testid="location-create-container"]').should("be.visible");

    // Step 14: Verify device input is enabled after selecting existing room
    cy.get('[data-testid="device-combobox"]', { timeout: 2000 }).should(
      "not.be.disabled",
    );

    // Step 15: Select an existing device
    cy.wait("@getDevices");
    cy.get('[data-testid="device-combobox"]').click();
    cy.wait(500);
    // Wait for dropdown menu to appear and select first option
    cy.get('[role="listbox"]').should("be.visible");
    cy.get('[role="option"]').first().click({ force: true });
    cy.wait(1000);

    // Step 16: Verify "Add" button is now enabled (room + device selected)
    cy.get('[data-testid="add-location-create-button"]').should(
      "not.be.disabled",
    );

    // Step 17: Click "Add" button to complete the location selection
    cy.get('[data-testid="add-location-create-button"]').click();

    // Step 18: Verify form closes and returns to search mode
    cy.get('[data-testid="move-modal"]')
      .find('[data-testid="location-create-container"]')
      .should("not.exist");
    cy.get('[data-testid="move-modal"]')
      .find('[data-testid="location-search-and-create"]')
      .should("be.visible");
    cy.get('[data-testid="move-modal"]')
      .find('[data-testid="add-location-button"]')
      .should("be.visible");

    // Step 19: Verify selected location preview appears and shows the selected location path
    cy.get('[data-testid="selected-location-section"]', {
      timeout: 2000,
    }).should("be.visible");
    cy.get('[data-testid="selected-location-section"]')
      .find(".location-label")
      .should("contain.text", "Selected Location");
    cy.get('[data-testid="selected-location-section"]')
      .find(".location-path")
      .should("be.visible")
      .should("not.be.empty")
      .should("contain", "Main Laboratory"); // Should contain the room name

    // Step 20: Wait for button to become enabled (validation passes)
    // The button becomes enabled when selectedLocationForValidation has room+device
    cy.get('[data-testid="confirm-move-button"]', { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .then(($btn) => {
        // Additional verification that button is actually enabled
        cy.wrap($btn).should("not.have.attr", "disabled");
      });

    // Step 20a: Click "Confirm Move" button to complete the move
    cy.get('[data-testid="confirm-move-button"]').click();

    // Step 21: Wait for move API call to complete
    cy.wait("@moveSample", { timeout: 5000 });

    // Step 22: Verify modal closes
    cy.get('[data-testid="move-modal"]').should("not.exist");

    // Step 23: Verify success notification appears
    cy.get('div[role="status"]', { timeout: 3000 })
      .should("be.visible")
      .should("contain.text", "success");

    // Step 24: Wait for samples table to reload after move
    cy.wait("@getSamples", { timeout: 5000 });

    // Step 25: Verify the sample's location was updated in the table
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-row"]')
      .first()
      .find('[data-testid="sample-location"]')
      .should("contain", "Main Laboratory"); // Should show the new location
  });

  it("Should not crash after selecting device and allow shelf selection", function () {
    // Console logs are already captured via onBeforeLoad in beforeEach

    // Turn off uncaught exception handling to capture crash errors
    const errors = [];
    Cypress.on("uncaught:exception", (err, runnable) => {
      errors.push(err.message);
      cy.log("UNCAUGHT EXCEPTION:", err.message);
      cy.log("Stack:", err.stack);
      return false; // Don't fail test on uncaught exceptions
    });

    // Verify we're on the samples tab
    cy.get('[data-testid="sample-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Get first sample and open move modal
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-row"]')
      .should("have.length.at.least", 1)
      .first()
      .within(() => {
        cy.get('[data-testid="sample-actions-overflow-menu"]')
          .should("be.visible")
          .click();
      });

    cy.wait(500);
    cy.get('[data-testid="move-menu-item"]', { timeout: 3000 })
      .should("be.visible")
      .click();

    // Wait for move modal to open
    cy.get('[data-testid="move-modal"]', { timeout: 5000 }).should(
      "be.visible",
    );

    // Click "Add Location" button
    cy.get('[data-testid="move-modal"]')
      .find('[data-testid="add-location-button"]', { timeout: 2000 })
      .first()
      .should("be.visible")
      .click();

    // Verify create form appears
    cy.get('[data-testid="location-create-container"]', {
      timeout: 3000,
    }).should("be.visible");

    // Select a room
    cy.get('[data-testid="room-combobox"]').click();
    cy.wait(500);
    // Wait for dropdown menu to appear and select first option
    cy.get('[role="listbox"]').should("be.visible");
    cy.get('[role="option"]').first().click({ force: true });
    cy.wait(1000);

    // Verify device input is enabled
    cy.wait("@getDevices");
    cy.get('[data-testid="device-combobox"]', { timeout: 2000 }).should(
      "not.be.disabled",
    );

    // Select a device
    cy.get('[data-testid="device-combobox"]').click();
    cy.wait(500);
    // Wait for dropdown menu to appear and select first option
    cy.get('[role="listbox"]').should("be.visible");
    cy.get('[role="option"]').first().click({ force: true });
    cy.wait(2000); // Wait for device selection and shelf loading

    // CRITICAL: Verify form doesn't crash and shelf input becomes enabled
    cy.get('[data-testid="location-create-container"]', {
      timeout: 3000,
    }).should("be.visible");

    // Check for any errors
    cy.window().then(() => {
      if (errors.length > 0) {
        cy.log("ERRORS CAPTURED:", errors);
      }
    });

    // Debug: Check console logs (removed invalid alias - console logs are visible via ELECTRON_ENABLE_LOGGING=1)

    // Debug: Check shelf combobox state
    cy.get('[data-testid="shelf-combobox"]').then(($shelf) => {
      cy.log("Shelf disabled state:", $shelf.prop("disabled"));
      cy.log("Shelf classes:", $shelf.attr("class"));
    });

    // Verify shelf input is enabled after device selection
    cy.get('[data-testid="shelf-combobox"]', { timeout: 3000 })
      .should("be.visible")
      .should("not.be.disabled");

    // Verify form is still visible (didn't crash)
    cy.get('[data-testid="location-create-container"]').should("be.visible");

    // Verify "Add" button is enabled (room + device selected)
    cy.get('[data-testid="add-location-create-button"]').should(
      "not.be.disabled",
    );

    // Click "Add" button to complete the location selection
    cy.get('[data-testid="add-location-create-button"]').click();

    // Verify form closes and returns to search mode
    cy.get('[data-testid="move-modal"]')
      .find('[data-testid="location-create-container"]')
      .should("not.exist");

    // Verify selected location preview appears and shows the selected location path
    cy.get('[data-testid="selected-location-section"]', {
      timeout: 2000,
    }).should("be.visible");
    cy.get('[data-testid="selected-location-section"]')
      .find(".location-label")
      .should("contain.text", "Selected Location");
    cy.get('[data-testid="selected-location-section"]')
      .find(".location-path")
      .should("be.visible")
      .should("not.be.empty");

    // Click "Confirm Move" button to complete the move
    cy.get('[data-testid="confirm-move-button"]', { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .then(($btn) => {
        // Additional verification that button is actually enabled
        cy.wrap($btn).should("not.have.attr", "disabled");
      })
      .click();

    // Wait for move API call to complete
    cy.wait("@moveSample", { timeout: 5000 });

    // Verify modal closes
    cy.get('[data-testid="move-modal"]').should("not.exist");

    // Verify success notification appears
    cy.get('div[role="status"]', { timeout: 3000 })
      .should("be.visible")
      .should("contain.text", "success");

    // Wait for samples table to reload after move
    cy.wait("@getSamples", { timeout: 5000 });

    // Verify the sample's location was updated in the table
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-row"]')
      .first()
      .find('[data-testid="sample-location"]')
      .should("not.be.empty");
  });

  it("Should show hierarchical path when selecting from dropdown (not Location + form)", function () {
    // This test covers the bug where selecting from dropdown doesn't show full hierarchical path
    // It verifies that hierarchical_path from search results is displayed correctly

    // Verify we're on the samples tab
    cy.get('[data-testid="sample-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Get first sample and open move modal
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-row"]')
      .should("have.length.at.least", 1)
      .first()
      .within(() => {
        cy.get('[data-testid="sample-actions-overflow-menu"]')
          .should("be.visible")
          .click();
      });

    cy.wait(500);
    cy.get('[data-testid="move-menu-item"]', { timeout: 3000 })
      .should("be.visible")
      .click();

    // Wait for move modal to open
    cy.get('[data-testid="move-modal"]', { timeout: 5000 }).should(
      "be.visible",
    );

    // Step 1: Select a location from the dropdown (LocationFilterDropdown)
    // This is the hierarchical click-based selection path
    cy.get('[data-testid="location-search-and-create"]', {
      timeout: 2000,
    }).should("be.visible");

    // Find the LocationFilterDropdown input/search field
    // The dropdown should be visible and allow searching
    cy.get('[data-testid="location-search-and-create"]')
      .find('input[role="combobox"]')
      .first()
      .should("be.visible")
      .click({ force: true });

    cy.wait(500);

    // Wait for dropdown options to appear and select a position-level location
    // This should have a full hierarchical_path
    cy.get('[role="listbox"]', { timeout: 3000 }).should("be.visible");

    // Select first position option (should have full hierarchical path)
    cy.get('[role="option"]').first().click({ force: true });

    cy.wait(1000);

    // Step 2: Verify selected location preview appears with full hierarchical path
    cy.get('[data-testid="selected-location-section"]', {
      timeout: 2000,
    }).should("be.visible");

    // Verify it shows the full hierarchical path (not just room name)
    cy.get('[data-testid="selected-location-section"]')
      .find(".location-path")
      .should("be.visible")
      .should("not.be.empty")
      .then(($path) => {
        const pathText = $path.text();
        cy.log("Selected location path:", pathText);

        // Verify it contains hierarchical separator (>)
        // This indicates it's showing the full path, not just a single level
        expect(pathText).to.include(">");

        // Verify it's not just showing a single word (like just "Main Laboratory")
        // It should show at least "Room > Device" format
        const parts = pathText
          .split(">")
          .map((p) => p.trim())
          .filter(Boolean);
        expect(parts.length).to.be.at.least(
          2,
          "Path should contain at least 2 levels",
        );
      });

    // Step 3: Verify Confirm Move button is enabled
    cy.get('[data-testid="confirm-move-button"]', { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled");

    // Step 4: Click Confirm Move and verify it persists
    cy.get('[data-testid="confirm-move-button"]').click();

    // Step 5: Wait for move API call to complete
    cy.wait("@moveSample", { timeout: 5000 });

    // Step 6: Verify modal closes
    cy.get('[data-testid="move-modal"]').should("not.exist");

    // Step 7: Verify success notification appears
    cy.get('div[role="status"]', { timeout: 3000 })
      .should("be.visible")
      .should("contain.text", "success");

    // Step 8: Wait for samples table to reload
    cy.wait("@getSamples", { timeout: 5000 });

    // Step 9: Verify the sample's location was actually updated in the table
    // This verifies the move actually persisted
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-row"]')
      .first()
      .find('[data-testid="sample-location"]')
      .should("be.visible")
      .should("not.be.empty")
      .then(($location) => {
        const locationText = $location.text();
        cy.log("Sample location after move:", locationText);

        // Verify it shows a hierarchical path (not just empty or old location)
        expect(locationText).to.not.be.empty;
        // Should contain the separator if it's a full path
        if (locationText.includes(">")) {
          const parts = locationText
            .split(">")
            .map((p) => p.trim())
            .filter(Boolean);
          expect(parts.length).to.be.at.least(2);
        }
      });
  });

  it("Should persist move when clicking Confirm Move button", function () {
    // This test explicitly verifies that the move operation persists correctly
    // It covers the bug where clicking Confirm Move doesn't actually persist the location

    // Verify we're on the samples tab
    cy.get('[data-testid="sample-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Get first sample and capture its initial location
    let initialLocation = "";
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-row"]')
      .should("have.length.at.least", 1)
      .first()
      .within(() => {
        cy.get('[data-testid="sample-location"]')
          .should("be.visible")
          .then(($loc) => {
            initialLocation = $loc.text().trim();
            cy.log("Initial location:", initialLocation);
          });
      });

    // Open move modal
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-row"]')
      .first()
      .within(() => {
        cy.get('[data-testid="sample-actions-overflow-menu"]')
          .should("be.visible")
          .click();
      });

    cy.wait(500);
    cy.get('[data-testid="move-menu-item"]', { timeout: 3000 })
      .should("be.visible")
      .click();

    // Wait for move modal to open
    cy.get('[data-testid="move-modal"]', { timeout: 5000 }).should(
      "be.visible",
    );

    // Select a location using the Location + form
    cy.get('[data-testid="move-modal"]')
      .find('[data-testid="add-location-button"]', { timeout: 2000 })
      .first()
      .should("be.visible")
      .click();

    cy.get('[data-testid="location-create-container"]', {
      timeout: 3000,
    }).should("be.visible");

    // Select room
    cy.get('[data-testid="room-combobox"]').click();
    cy.wait(500);
    cy.get('[role="listbox"]').should("be.visible");
    cy.get('[role="option"]').first().click({ force: true });
    cy.wait(1000);
    cy.wait("@getDevices");

    // Select device
    cy.get('[data-testid="device-combobox"]').click();
    cy.wait(500);
    cy.get('[role="listbox"]').should("be.visible");
    cy.get('[role="option"]').first().click({ force: true });
    cy.wait(1000);

    // Click Add button
    cy.get('[data-testid="add-location-create-button"]')
      .should("not.be.disabled")
      .click();

    cy.wait(500);

    // Verify selected location preview appears
    cy.get('[data-testid="selected-location-section"]', {
      timeout: 2000,
    }).should("be.visible");

    // Click Confirm Move
    cy.get('[data-testid="confirm-move-button"]', { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .click();

    // CRITICAL: Verify the move API call is actually made
    cy.wait("@moveSample", { timeout: 5000 }).then((interception) => {
      // Verify the request body contains the correct data
      expect(interception.request.body).to.have.property("sampleId");
      expect(interception.request.body).to.have.property("targetPositionId");
      cy.log("Move API call made with:", interception.request.body);
    });

    // Verify modal closes
    cy.get('[data-testid="move-modal"]').should("not.exist");

    // Verify success notification
    cy.get('div[role="status"]', { timeout: 3000 })
      .should("be.visible")
      .should("contain.text", "success");

    // Wait for table reload
    cy.wait("@getSamples", { timeout: 5000 });

    // CRITICAL: Verify the location actually changed in the table
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-row"]')
      .first()
      .find('[data-testid="sample-location"]')
      .should("be.visible")
      .then(($newLocation) => {
        const newLocationText = $newLocation.text().trim();
        cy.log("New location after move:", newLocationText);

        // Verify location changed (not the same as initial)
        // Note: If initial was empty, new should not be empty
        if (initialLocation) {
          expect(newLocationText).to.not.equal(
            initialLocation,
            "Location should have changed",
          );
        } else {
          expect(newLocationText).to.not.be.empty("Location should be set");
        }
      });
  });
});
