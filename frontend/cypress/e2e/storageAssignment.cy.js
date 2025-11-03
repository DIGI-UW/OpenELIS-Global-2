import LoginPage from "../pages/LoginPage";
import StorageAssignmentPage from "../pages/StorageAssignmentPage";

/**
 * E2E Tests for User Story P1 - Basic Storage Assignment
 * Tests all three input modes: cascading dropdowns, type-ahead, barcode scan
 */

let homePage = null;
let loginPage = null;
let storageAssignmentPage = null;

before("Login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
  homePage = loginPage.goToHomePage();
});

describe("Storage Assignment - Cascading Dropdowns (P1)", function () {
  it("Should navigate to Sample Entry Page", () => {
    cy.visit("/SampleBatchEntrySetup");
  });

  it("Should assign sample to storage location using cascading dropdowns", function () {
    storageAssignmentPage = new StorageAssignmentPage();

    // Fill in sample information
    cy.get('[data-testid="accession-number"]').type("S-2025-TEST-001");

    // Locate storage location selector
    cy.get('[data-testid="storage-location-selector"]').should("be.visible");

    // Select room
    storageAssignmentPage.selectRoom("Main Laboratory");

    // Verify device dropdown is now enabled
    cy.get('[data-testid="device-dropdown"]').should("not.be.disabled");

    // Select device
    storageAssignmentPage.selectDevice("Freezer Unit 1");

    // Verify shelf dropdown is now enabled
    cy.get('[data-testid="shelf-dropdown"]').should("not.be.disabled");

    // Select shelf
    storageAssignmentPage.selectShelf("Shelf-A");

    // Select rack
    storageAssignmentPage.selectRack("Rack R1");

    // Select position
    storageAssignmentPage.selectPosition("A5");

    // Verify hierarchical path displays correctly
    cy.get('[data-testid="location-path"]').should(
      "contain.text",
      "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5",
    );

    // Save sample
    storageAssignmentPage.clickSave();

    // Verify success notification
    cy.get('div[role="status"]')
      .should("be.visible")
      .and("contain.text", "success");
  });
});

describe("Storage Assignment - Type-Ahead Autocomplete (P1)", function () {
  it("Should assign sample using type-ahead search", function () {
    cy.visit("/SampleBatchEntrySetup");
    storageAssignmentPage = new StorageAssignmentPage();

    // Fill in sample information
    cy.get('[data-testid="accession-number"]').type("S-2025-TEST-002");

    // Switch to autocomplete mode (if widget supports mode switching)
    // For now, assume default mode or mode prop can be set

    // Type in autocomplete search
    cy.get('[data-testid="location-search"]').type("MAIN-FRZ01");

    // Select from dropdown results
    cy.contains("Main Laboratory > Freezer Unit 1").click();

    // Save sample
    storageAssignmentPage.clickSave();

    // Verify success
    cy.get('div[role="status"]').should("be.visible");
  });
});

describe("Storage Assignment - Barcode Scan (P1)", function () {
  it("Should assign sample using barcode scanner", function () {
    cy.visit("/SampleBatchEntrySetup");
    storageAssignmentPage = new StorageAssignmentPage();

    // Fill in sample information
    cy.get('[data-testid="accession-number"]').type("S-2025-TEST-003");

    // Switch to barcode mode
    // Simulate barcode scan (rapid keyboard input)
    cy.get('[data-testid="barcode-input"]').type(
      "MAIN-FRZ01-SHA-RKR1-A5{enter}",
    );

    // Verify location parsed and displayed
    cy.get('[data-testid="location-path"]').should("contain.text", "A5");

    // Save sample
    storageAssignmentPage.clickSave();

    // Verify success
    cy.get('div[role="status"]').should("be.visible");
  });
});

describe("Storage Assignment - Inline Location Creation (P1)", function () {
  it("Should allow inline creation of new room", function () {
    cy.visit("/SampleBatchEntrySetup");

    // Click "Add New Room" button
    cy.contains("Add New Room").click();

    // Fill in new room form (modal)
    cy.get('[data-testid="room-name"]').type("New Test Room");
    cy.get('[data-testid="room-code"]').type("NEW-ROOM");

    // Save new room
    cy.get('[data-testid="save-room"]').click();

    // Verify new room appears in dropdown
    cy.get('[data-testid="room-dropdown"]').click();
    cy.contains("New Test Room").should("be.visible");
  });
});

describe("Storage Assignment - Capacity Warning (P1)", function () {
  it("Should display capacity warning when rack is 80% full", function () {
    cy.visit("/SampleBatchEntrySetup");
    storageAssignmentPage = new StorageAssignmentPage();

    // Select location in nearly-full rack
    storageAssignmentPage
      .selectRoom("Main Laboratory")
      .selectDevice("Freezer Unit 1")
      .selectShelf("Shelf-A")
      .selectRack("Rack R2"); // Assume this rack is 85% full

    // Verify capacity warning displays
    cy.get('[data-testid="capacity-warning"]')
      .should("be.visible")
      .and("contain.text", "85%");
  });
});
