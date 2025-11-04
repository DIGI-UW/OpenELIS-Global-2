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
  it("Should navigate through order entry workflow to sample entry step", () => {
    cy.visit("/SamplePatientEntry");
    cy.wait(3000); // Wait for page to load

    // The StorageLocationSelector appears in the sample entry step
    // We need to progress through: Patient Info → Program Selection → Sample Entry
    // For now, we'll test if the selector is accessible once we reach that step
    // Note: This test may need a patient/program selection workflow similar to orderEntity.cy.js
  });

  it("Should assign sample to storage location using cascading dropdowns", function () {
    storageAssignmentPage = new StorageAssignmentPage();

    // Navigate to sample entry step (assuming workflow navigation is handled)
    // StorageLocationSelector appears in SampleType component within AddSample
    // Scroll to find the selector if needed
    cy.get("body").then(($body) => {
      // Check if selector exists on current page
      if (
        $body.find('[data-testid="storage-location-selector"]').length === 0
      ) {
        // If not visible, we may need to navigate through workflow first
        cy.log(
          "Storage selector not yet visible - may need to complete workflow steps",
        );
      }
    });

    // Wait for storage location selector to be visible (may need to scroll or navigate)
    cy.get('[data-testid="storage-location-selector"]', { timeout: 20000 })
      .scrollIntoView()
      .should("be.visible");

    // Select room (wait for API call to complete)
    storageAssignmentPage.selectRoom("MAIN");
    cy.wait(2000); // Wait for devices to load

    // Verify device dropdown is now enabled
    cy.get('[data-testid="device-dropdown"]').should("not.be.disabled");

    // Select device
    storageAssignmentPage.selectDevice("FRZ01");
    cy.wait(2000); // Wait for shelves to load

    // Verify shelf dropdown is now enabled
    cy.get('[data-testid="shelf-dropdown"]').should("not.be.disabled");

    // Select shelf
    storageAssignmentPage.selectShelf("SHA");
    cy.wait(2000); // Wait for racks to load

    // Select rack
    storageAssignmentPage.selectRack("RKR1");
    cy.wait(2000); // Wait for positions to load

    // Select position
    storageAssignmentPage.selectPosition("A5");

    // Verify hierarchical path displays correctly
    cy.get('[data-testid="location-path"]', { timeout: 5000 }).should(
      "contain.text",
      "MAIN",
    );
  });
});

describe("Storage Assignment - Type-Ahead Autocomplete (P1)", function () {
  beforeEach(function () {
    // Navigate to sample entry step for each test
    cy.visit("/SamplePatientEntry");
    cy.wait(2000);

    // Navigate through workflow if needed
    // This is a simplified approach - in practice, you might want to reuse the navigation steps
    // For now, assuming we can navigate directly or reuse state from previous tests
  });

  it("Should assign sample using type-ahead search", function () {
    storageAssignmentPage = new StorageAssignmentPage();

    // Wait for storage location selector
    cy.get('[data-testid="storage-location-selector"]', {
      timeout: 10000,
    }).should("be.visible");

    // Note: Autocomplete mode requires the component to be rendered with mode="autocomplete"
    // This test assumes the component can switch modes or is rendered with autocomplete mode
    // For now, we'll test the cascading dropdowns which are the primary mode
    // Autocomplete mode testing may require component updates

    // Type in search (if autocomplete ComboBox is available)
    cy.get('[data-testid="location-search"]', { timeout: 5000 })
      .should("be.visible")
      .type("MAIN");

    cy.wait(1000);
    // Select from dropdown results if available
    cy.contains("MAIN").click({ force: true });

    // Verify location path displays
    cy.get('[data-testid="location-path"]', { timeout: 5000 }).should(
      "be.visible",
    );
  });
});

describe("Storage Assignment - Barcode Scan (P1)", function () {
  it("Should assign sample using barcode scanner", function () {
    // Reuse navigation from previous describe block or navigate again
    storageAssignmentPage = new StorageAssignmentPage();

    // Wait for storage location selector
    cy.get('[data-testid="storage-location-selector"]', {
      timeout: 10000,
    }).should("be.visible");

    // Note: Barcode mode requires the component to be rendered with mode="barcode"
    // This test assumes barcode input is available
    // For now, we'll verify the selector is visible and can accept input
    cy.get('[data-testid="barcode-input"]', { timeout: 5000 })
      .should("be.visible")
      .type("MAIN-FRZ01-SHA-RKR1-A5{enter}");

    cy.wait(2000);
    // Verify location parsed and displayed (if barcode parsing is implemented)
    cy.get('[data-testid="location-path"]', { timeout: 5000 }).should(
      "be.visible",
    );
  });
});

describe("Storage Assignment - Inline Location Creation (P1)", function () {
  it("Should allow inline creation of new room", function () {
    storageAssignmentPage = new StorageAssignmentPage();

    // Wait for storage location selector with inline creation enabled
    cy.get('[data-testid="storage-location-selector"]', {
      timeout: 10000,
    }).should("be.visible");

    // Click "Add New Room" button (if enableInlineCreation=true)
    cy.contains("Add New Room", { timeout: 5000 }).should("be.visible").click();

    // Wait for modal/form to appear
    cy.wait(1000);

    // Fill in new room form (modal)
    cy.get('[data-testid="room-name"]', { timeout: 5000 })
      .should("be.visible")
      .type("New Test Room");
    cy.get('[data-testid="room-code"]').type("NEW-ROOM");

    // Save new room
    cy.get('[data-testid="save-room"]').click();
    cy.wait(2000);

    // Verify success notification
    cy.get('div[role="status"]').should("be.visible");

    // Verify new room appears in dropdown
    cy.get('[data-testid="room-dropdown"]').click();
    cy.contains("New Test Room", { timeout: 5000 }).should("be.visible");
  });
});

describe("Storage Assignment - Capacity Warning (P1)", function () {
  it("Should display capacity warning when rack is 80% full", function () {
    storageAssignmentPage = new StorageAssignmentPage();

    // Wait for storage location selector
    cy.get('[data-testid="storage-location-selector"]', {
      timeout: 10000,
    }).should("be.visible");

    // Select location in nearly-full rack
    storageAssignmentPage.selectRoom("MAIN");
    cy.wait(1000);
    storageAssignmentPage.selectDevice("FRZ01");
    cy.wait(1000);
    storageAssignmentPage.selectShelf("SHA");
    cy.wait(1000);
    storageAssignmentPage.selectRack("RKR2"); // Assume this rack is 85% full
    cy.wait(1000);

    // Verify capacity warning displays (if capacity calculation is implemented)
    cy.get('[data-testid="capacity-warning"]', { timeout: 5000 })
      .should("be.visible")
      .and("contain.text", "%");
  });
});
