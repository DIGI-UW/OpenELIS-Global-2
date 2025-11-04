import LoginPage from "../pages/LoginPage";
import StorageAssignmentPage from "../pages/StorageAssignmentPage";
import OrderEntityPage from "../pages/OrderEntityPage";
import PatientEntryPage from "../pages/PatientEntryPage";

/**
 * E2E Tests for User Story P1 - Basic Storage Assignment
 * Tests all three input modes: cascading dropdowns, type-ahead, barcode scan
 */

let homePage = null;
let loginPage = null;
let storageAssignmentPage = null;
let orderEntityPage = null;
let patientEntryPage = null;

before("Login and Load Test Fixtures", () => {
  loginPage = new LoginPage();
  loginPage.visit();
  homePage = loginPage.goToHomePage();
  
  // Load storage test fixtures (includes patients, samples, and storage hierarchy)
  cy.loadStorageFixtures();
});

after("Clean up test fixtures", () => {
  // Clean up test fixtures after all tests complete (optional, controlled by CYPRESS_CLEANUP_FIXTURES env var)
  // Set CYPRESS_CLEANUP_FIXTURES=false to keep fixtures for manual testing
  // Default: true (cleanup enabled)
  if (Cypress.env("CLEANUP_FIXTURES")) {
    cy.cleanStorageFixtures();
  } else {
    cy.log("Skipping fixture cleanup (CYPRESS_CLEANUP_FIXTURES=false) - fixtures preserved for manual testing");
  }
});

describe("Storage Assignment - Cascading Dropdowns (P1)", function () {
  it("Should navigate through order entry workflow to sample entry step", () => {
    // Navigate to order entry page
    orderEntityPage = homePage.goToOrderPage();
    cy.wait(1000);
    
    // Navigate to patient entry step
    patientEntryPage = orderEntityPage.getPatientPage();
    cy.wait(1000);
    
    // Search for E2E test patient
    cy.fixture("Patient").then((patient) => {
      // Use E2E test patient (Smith)
      patientEntryPage.searchPatientByFirstAndLastName("John", "Smith");
      patientEntryPage.clickSearchPatientButton();
      cy.wait(2000);
      
      // Select patient from search results
      patientEntryPage.selectPatientFromSearchResults();
      cy.wait(300);
      
      // Verify patient is selected
      patientEntryPage.getFirstName().should("have.value", "John");
      patientEntryPage.getLastName().should("have.value", "Smith");
    });
    
    // Proceed to program selection
    orderEntityPage.clickNextButton();
    cy.wait(1000);
    
    // Select program (Cytology or any available program)
    orderEntityPage.selectCytology();
    cy.wait(200);
    orderEntityPage.clickNextButton();
    cy.wait(1000);
    
    // Now we should be on the sample entry step where StorageLocationSelector is visible
    // Verify we can see the storage location selector
    cy.get('[data-testid="storage-location-selector"]', { timeout: 10000 })
      .should("be.visible");
  });

  it("Should assign sample to storage location using cascading dropdowns", function () {
    storageAssignmentPage = new StorageAssignmentPage();
    
    // Navigate through workflow (reuse from previous test)
    orderEntityPage = homePage.goToOrderPage();
    cy.wait(1000);
    patientEntryPage = orderEntityPage.getPatientPage();
    cy.wait(1000);
    
    // Search and select E2E test patient
    patientEntryPage.searchPatientByFirstAndLastName("John", "Smith");
    patientEntryPage.clickSearchPatientButton();
    cy.wait(2000);
    patientEntryPage.selectPatientFromSearchResults();
    cy.wait(300);
    orderEntityPage.clickNextButton();
    cy.wait(1000);
    orderEntityPage.selectCytology();
    cy.wait(200);
    orderEntityPage.clickNextButton();
    cy.wait(1000);

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
    // Navigate through order entry workflow to sample entry step
    orderEntityPage = homePage.goToOrderPage();
    cy.wait(1000);
    patientEntryPage = orderEntityPage.getPatientPage();
    cy.wait(1000);
    
    // Search and select E2E test patient
    patientEntryPage.searchPatientByFirstAndLastName("John", "Smith");
    patientEntryPage.clickSearchPatientButton();
    cy.wait(2000);
    patientEntryPage.selectPatientFromSearchResults();
    cy.wait(300);
    orderEntityPage.clickNextButton();
    cy.wait(1000);
    orderEntityPage.selectCytology();
    cy.wait(200);
    orderEntityPage.clickNextButton();
    cy.wait(1000);
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
  beforeEach(function () {
    // Navigate through order entry workflow to sample entry step
    orderEntityPage = homePage.goToOrderPage();
    cy.wait(1000);
    patientEntryPage = orderEntityPage.getPatientPage();
    cy.wait(1000);
    
    // Search and select E2E test patient
    patientEntryPage.searchPatientByFirstAndLastName("John", "Smith");
    patientEntryPage.clickSearchPatientButton();
    cy.wait(2000);
    patientEntryPage.selectPatientFromSearchResults();
    cy.wait(300);
    orderEntityPage.clickNextButton();
    cy.wait(1000);
    orderEntityPage.selectCytology();
    cy.wait(200);
    orderEntityPage.clickNextButton();
    cy.wait(1000);
  });
  
  it("Should assign sample using barcode scanner", function () {
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
  beforeEach(function () {
    // Navigate through order entry workflow to sample entry step
    orderEntityPage = homePage.goToOrderPage();
    cy.wait(1000);
    patientEntryPage = orderEntityPage.getPatientPage();
    cy.wait(1000);
    
    // Search and select E2E test patient
    patientEntryPage.searchPatientByFirstAndLastName("John", "Smith");
    patientEntryPage.clickSearchPatientButton();
    cy.wait(2000);
    patientEntryPage.selectPatientFromSearchResults();
    cy.wait(300);
    orderEntityPage.clickNextButton();
    cy.wait(1000);
    orderEntityPage.selectCytology();
    cy.wait(200);
    orderEntityPage.clickNextButton();
    cy.wait(1000);
  });
  
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
  beforeEach(function () {
    // Navigate through order entry workflow to sample entry step
    orderEntityPage = homePage.goToOrderPage();
    cy.wait(1000);
    patientEntryPage = orderEntityPage.getPatientPage();
    cy.wait(1000);
    
    // Search and select E2E test patient
    patientEntryPage.searchPatientByFirstAndLastName("John", "Smith");
    patientEntryPage.clickSearchPatientButton();
    cy.wait(2000);
    patientEntryPage.selectPatientFromSearchResults();
    cy.wait(300);
    orderEntityPage.clickNextButton();
    cy.wait(1000);
    orderEntityPage.selectCytology();
    cy.wait(200);
    orderEntityPage.clickNextButton();
    cy.wait(1000);
  });
  
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
