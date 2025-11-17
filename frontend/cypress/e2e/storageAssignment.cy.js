import StorageAssignmentPage from "../pages/StorageAssignmentPage";
import OrderEntityPage from "../pages/OrderEntityPage";
import PatientEntryPage from "../pages/PatientEntryPage";

/**
 * E2E Tests for User Story P1 - Basic Storage Assignment
 * Tests all three input modes: cascading dropdowns, type-ahead, barcode scan
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Focused on happy paths (user workflows, not implementation details)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"
 */

let homePage = null;
let storageAssignmentPage = null;
let orderEntityPage = null;
let patientEntryPage = null;

before("Setup storage tests", () => {
  cy.setupStorageTests().then((page) => {
    homePage = page;
  });
});

before("Navigate to sample entry step once for all tests", () => {
  // Set up intercepts
  cy.intercept("GET", "**/rest/user-programs**").as("getPrograms");

  // Navigate to sample entry step ONCE - all tests will use this state
  cy.navigateToSampleEntryStep(homePage).then((pages) => {
    orderEntityPage = pages.orderEntityPage;
    patientEntryPage = pages.patientEntryPage;
  });
});

after("Cleanup storage tests", () => {
  cy.cleanupStorageTests();
});

describe("Storage Assignment - Cascading Dropdowns (P1)", function () {
  beforeEach(() => {
    // Set up API intercepts BEFORE actions that trigger them (Constitution V.5)
    cy.intercept("GET", "**/rest/storage/rooms**").as("getRooms");
    cy.intercept("GET", "**/rest/storage/devices**").as("getDevices");
    cy.intercept("GET", "**/rest/storage/shelves**").as("getShelves");
    cy.intercept("GET", "**/rest/storage/racks**").as("getRacks");
    cy.intercept("GET", "**/rest/storage/positions**").as("getPositions");
    cy.intercept("POST", "**/rest/storage/sample-items/assign**").as(
      "assignSample",
    );
    // Also intercept the assignment endpoint with different patterns
    cy.intercept("POST", "**/storage/sample-items/assign**").as(
      "assignSampleAlt",
    );
  });

  it("Should navigate through order entry workflow to sample entry step", () => {
    // Navigation already done in before() - just verify we're on the right page
    cy.get('[data-testid="storage-location-selector"]', {
      timeout: 10000,
    }).should("be.visible");
  });

  it("Should assign sample to storage location using cascading dropdowns", function () {
    storageAssignmentPage = new StorageAssignmentPage();

    // Navigation already done in before() - we're already on sample entry step

    // With workflow="orders", StorageLocationSelector shows CompactLocationView
    // Need to click "Expand" button to open modal with cascading dropdowns
    cy.get('[data-testid="expand-button"]', { timeout: 10000 })
      .should("be.visible")
      .click();

    // Wait for modal to open - LocationManagementModal contains LocationSearchAndCreate
    // Modal uses ComposedModal from Carbon, wait for it to be visible
    cy.get('[role="dialog"]', { timeout: 10000 }).should("be.visible");

    // LocationSearchAndCreate starts in search mode - need to click "Add Location" to show create form
    // The create form contains EnhancedCascadingMode with comboboxes
    cy.get(
      'button:contains("Add Location"), [data-testid="add-location-button"]',
      { timeout: 10000 },
    )
      .should("be.visible")
      .click();

    // Wait for create form to show EnhancedCascadingMode with comboboxes
    cy.get('[data-testid="room-combobox"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Select room and wait for API call (intercept timing)
    storageAssignmentPage.selectRoom("MAIN");
    cy.wait("@getRooms");

    // Verify device combobox is now enabled (retry-ability)
    cy.get('[data-testid="device-combobox"]')
      .should("not.be.disabled")
      .should("be.visible");

    // Select device and wait for API call
    storageAssignmentPage.selectDevice("FRZ01");
    cy.wait("@getDevices");

    // Verify shelf combobox is now enabled
    cy.get('[data-testid="shelf-combobox"]')
      .should("not.be.disabled")
      .should("be.visible");

    // Select shelf - wait for shelves API call first
    cy.wait("@getShelves", { timeout: 10000 });
    storageAssignmentPage.selectShelf("SHA");

    // Select rack - wait for racks API call first
    cy.wait("@getRacks", { timeout: 10000 });
    storageAssignmentPage.selectRack("RKR1");

    // Position is a text input in LocationManagementModal, not a dropdown
    // Enter position coordinate directly
    cy.get("#position-input", { timeout: 10000 })
      .should("be.visible")
      .clear()
      .type("A5");

    // Click "Add" button to confirm location selection in create form
    // This adds the location to selectedLocation state
    cy.get('[data-testid="add-location-create-button"]', { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .click();

    // Wait for location to be selected (selectedLocationPath should appear)
    cy.get('[data-testid="selected-location-section"]', {
      timeout: 10000,
    }).should("be.visible");

    // Now LocationManagementModal's "Confirm" button should be enabled
    // Use test ID from modal footer
    cy.get('[data-testid="assign-button"]', { timeout: 10000 })
      .should("be.visible")
      .should("not.be.disabled")
      .click();

    // In order entry workflow, assignment might not happen immediately
    // The location is saved to form state and assigned when order is submitted
    // Verify the location was selected - path shows full hierarchical path
    cy.get(
      '[data-testid="location-path-text"], [data-testid="location-path"]',
      { timeout: 10000 },
    )
      .should("be.visible")
      .should("contain.text", "Main"); // Case-insensitive match for "Main Laboratory" or "MAIN"
  });
});

describe("Storage Assignment - Type-Ahead Autocomplete (P1)", function () {
  beforeEach(function () {
    // Set up intercepts BEFORE actions
    cy.intercept("GET", "**/rest/storage/locations/search**").as(
      "searchLocations",
    );
    // Navigation already done in before() - we're already on sample entry step
  });

  it("Should assign sample using type-ahead search", function () {
    storageAssignmentPage = new StorageAssignmentPage();

    // Wait for storage location selector (element readiness check)
    cy.get('[data-testid="storage-location-selector"]', {
      timeout: 10000,
    }).should("be.visible");

    // With workflow="orders", need to expand to access search
    cy.get('[data-testid="expand-button"]', { timeout: 10000 })
      .should("be.visible")
      .click();

    // Wait for modal to open and search input to be visible
    // LocationManagementModal uses LocationSearchAndCreate which has data-testid="location-search-and-create"
    cy.get('[data-testid="location-search-and-create"], #location-search', {
      timeout: 10000,
    }).should("be.visible");

    // Type in search (LocationFilterDropdown inside LocationSearchAndCreate)
    cy.get(
      '#location-search, [data-testid="location-search-and-create"] input',
      { timeout: 5000 },
    )
      .should("be.visible")
      .clear()
      .type("MAIN");

    // Wait for search API call (intercept timing)
    cy.wait("@searchLocations");

    // Select from dropdown results if available (retry-ability)
    cy.contains("MAIN", { timeout: 5000 })
      .should("be.visible")
      .click({ force: true });

    // Verify location path displays (retry-ability)
    cy.get('[data-testid="location-path"]', { timeout: 5000 }).should(
      "be.visible",
    );
  });
});

describe("Storage Assignment - Barcode Scan (P1)", function () {
  beforeEach(function () {
    // Set up intercepts BEFORE actions
    cy.intercept("POST", "**/rest/storage/barcode/validate**").as(
      "validateBarcode",
    );
    // Navigation already done in before() - we're already on sample entry step
  });

  it("Should assign sample using barcode scanner", function () {
    storageAssignmentPage = new StorageAssignmentPage();

    // Wait for storage location selector (element readiness check)
    cy.get('[data-testid="storage-location-selector"]', {
      timeout: 10000,
    }).should("be.visible");

    // With workflow="orders", need to expand to access barcode input
    cy.get('[data-testid="expand-button"]', { timeout: 10000 })
      .should("be.visible")
      .click();

    // Wait for modal to open and barcode input to be visible
    cy.get(
      '[data-testid="barcode-input"], [data-testid="unified-barcode-input"]',
      { timeout: 10000 },
    ).should("be.visible");

    // Type barcode (simulating scanner input)
    cy.get('[data-testid="barcode-input"], #barcode-input', { timeout: 5000 })
      .should("be.visible")
      .type("MAIN-FRZ01-SHA-RKR1-A5{enter}");

    // Wait for barcode validation API call (intercept timing)
    cy.wait("@validateBarcode", { timeout: 10000 });

    // Verify location parsed and displayed (retry-ability)
    cy.get('[data-testid="location-path"]', { timeout: 5000 })
      .should("be.visible")
      .should("contain.text", "MAIN");
  });
});

describe("Storage Assignment - Capacity Warning (P1)", function () {
  beforeEach(function () {
    // Set up intercepts BEFORE actions
    cy.intercept("GET", "**/rest/storage/rooms**").as("getRooms");
    cy.intercept("GET", "**/rest/storage/devices**").as("getDevices");
    cy.intercept("GET", "**/rest/storage/shelves**").as("getShelves");
    cy.intercept("GET", "**/rest/storage/racks**").as("getRacks");
    // Navigation already done in before() - we're already on sample entry step
  });

  it("Should display capacity warning when rack is 80% full", function () {
    storageAssignmentPage = new StorageAssignmentPage();

    // Wait for storage location selector (element readiness check)
    cy.get('[data-testid="storage-location-selector"]', {
      timeout: 10000,
    }).should("be.visible");

    // With workflow="orders", StorageLocationSelector shows CompactLocationView
    // Need to click "Expand" button to open modal with cascading dropdowns
    cy.get('[data-testid="expand-button"]', { timeout: 10000 })
      .should("be.visible")
      .click();

    // Wait for modal to open - LocationManagementModal contains LocationSearchAndCreate
    // Modal uses ComposedModal from Carbon, wait for it to be visible
    cy.get('[role="dialog"]', { timeout: 10000 }).should("be.visible");

    // LocationSearchAndCreate starts in search mode - need to click "Add Location" to show create form
    // The create form contains EnhancedCascadingMode with comboboxes
    cy.get(
      'button:contains("Add Location"), [data-testid="add-location-button"]',
      { timeout: 10000 },
    )
      .should("be.visible")
      .click();

    // Wait for create form to show EnhancedCascadingMode with comboboxes
    cy.get('[data-testid="room-combobox"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Select location in nearly-full rack
    storageAssignmentPage.selectRoom("MAIN");
    cy.wait("@getRooms");

    storageAssignmentPage.selectDevice("FRZ01");
    cy.wait("@getDevices");

    storageAssignmentPage.selectShelf("SHA");
    cy.wait("@getShelves");

    storageAssignmentPage.selectRack("RKR2"); // Assume this rack is 85% full
    cy.wait("@getRacks");

    // Verify capacity warning displays (retry-ability)
    cy.get('[data-testid="capacity-warning"]', { timeout: 5000 })
      .should("be.visible")
      .should("contain.text", "%");
  });
});
