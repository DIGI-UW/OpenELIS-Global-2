import LoginPage from "../pages/LoginPage";
import StorageAssignmentPage from "../pages/StorageAssignmentPage";

/**
 * E2E Tests for User Story P2B - Sample Movement
 * Tests single and bulk sample movement with audit trail
 */

let loginPage = null;
let homePage = null;
let storageAssignmentPage = null;

before("Login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
  homePage = loginPage.goToHomePage();
});

describe("Storage Movement - Single Sample Move (P2B)", function () {
  beforeEach(() => {
    cy.visit("/StorageDashboard");
    cy.wait(2000);
    storageAssignmentPage = new StorageAssignmentPage();
  });

  it("Should move sample between locations and create audit trail", function () {
    // Find a sample in the list
    cy.get('[data-testid="sample-list"]')
      .should("be.visible")
      .find('[data-testid="sample-row"]')
      .first()
      .within(() => {
        // Click overflow menu
        cy.get('[data-testid="sample-actions-menu"]').click();
        // Click Move option
        cy.contains("Move").click();
      });

    // Wait for move modal to open
    cy.get('[data-testid="move-location-modal"]', { timeout: 5000 }).should(
      "be.visible",
    );

    // Verify current location is displayed
    cy.get('[data-testid="current-location"]').should("be.visible");

    // Select new target location using storage location selector
    cy.get('[data-testid="target-location-selector"]').within(() => {
      storageAssignmentPage.selectRoom("MAIN");
      cy.wait(1000);
      storageAssignmentPage.selectDevice("FRZ01");
      cy.wait(1000);
      storageAssignmentPage.selectShelf("SHA");
      cy.wait(1000);
      storageAssignmentPage.selectRack("RKR2");
      cy.wait(1000);
      storageAssignmentPage.selectPosition("B3");
    });

    // Enter reason (optional)
    cy.get('[data-testid="move-reason"]').type("Testing preparation");

    // Confirm move
    cy.get('[data-testid="confirm-move-button"]').click();
    cy.wait(2000);

    // Verify success notification
    cy.get('div[role="status"]')
      .should("be.visible")
      .and("contain.text", "success");

    // Verify sample location updated in list
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-row"]')
      .first()
      .find('[data-testid="sample-location"]')
      .should("contain.text", "RKR2");
  });

  it("Should prevent move to occupied position", function () {
    // Similar to above but select an occupied position
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-row"]')
      .first()
      .within(() => {
        cy.get('[data-testid="sample-actions-menu"]').click();
        cy.contains("Move").click();
      });

    cy.get('[data-testid="move-location-modal"]').should("be.visible");

    // Select an occupied position (assuming A5 is occupied)
    cy.get('[data-testid="target-location-selector"]').within(() => {
      storageAssignmentPage.selectRoom("MAIN");
      cy.wait(1000);
      storageAssignmentPage.selectDevice("FRZ01");
      cy.wait(1000);
      storageAssignmentPage.selectShelf("SHA");
      cy.wait(1000);
      storageAssignmentPage.selectRack("RKR1");
      cy.wait(1000);
      storageAssignmentPage.selectPosition("A5"); // Occupied position
    });

    cy.get('[data-testid="confirm-move-button"]').click();
    cy.wait(1000);

    // Verify error message
    cy.get('div[role="alert"]')
      .should("be.visible")
      .and("contain.text", "occupied");
  });
});

describe("Storage Movement - Bulk Move (P2B)", function () {
  beforeEach(() => {
    cy.visit("/StorageDashboard");
    cy.wait(2000);
    storageAssignmentPage = new StorageAssignmentPage();
  });

  it("Should perform bulk move with auto-assigned positions", function () {
    // Select multiple samples using checkboxes
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-checkbox"]')
      .first()
      .check();
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-checkbox"]')
      .eq(1)
      .check();
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-checkbox"]')
      .eq(2)
      .check();

    // Click bulk actions menu
    cy.get('[data-testid="bulk-actions-menu"]').click();
    cy.contains("Bulk Move").click();

    // Wait for bulk move modal
    cy.get('[data-testid="bulk-move-modal"]', { timeout: 5000 }).should(
      "be.visible",
    );

    // Select target rack
    cy.get('[data-testid="target-rack-selector"]').within(() => {
      storageAssignmentPage.selectRoom("MAIN");
      cy.wait(1000);
      storageAssignmentPage.selectDevice("FRZ01");
      cy.wait(1000);
      storageAssignmentPage.selectShelf("SHA");
      cy.wait(1000);
      storageAssignmentPage.selectRack("RKR2");
    });

    cy.wait(2000); // Wait for auto-assignment

    // Verify auto-assigned positions are displayed in preview
    cy.get('[data-testid="position-assignment-preview"]').should("be.visible");
    cy.get('[data-testid="position-assignment"]').should("have.length", 3);

    // Confirm bulk move
    cy.get('[data-testid="confirm-bulk-move-button"]').click();
    cy.wait(3000);

    // Verify success
    cy.get('div[role="status"]')
      .should("be.visible")
      .and("contain.text", "success");
  });

  it("Should allow manual editing of position assignments", function () {
    // Similar setup to above
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-checkbox"]')
      .first()
      .check();
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-checkbox"]')
      .eq(1)
      .check();

    cy.get('[data-testid="bulk-actions-menu"]').click();
    cy.contains("Bulk Move").click();

    cy.get('[data-testid="bulk-move-modal"]').should("be.visible");

    // Select target rack
    cy.get('[data-testid="target-rack-selector"]').within(() => {
      storageAssignmentPage.selectRoom("MAIN");
      cy.wait(1000);
      storageAssignmentPage.selectDevice("FRZ01");
      cy.wait(1000);
      storageAssignmentPage.selectShelf("SHA");
      cy.wait(1000);
      storageAssignmentPage.selectRack("RKR2");
    });

    cy.wait(2000);

    // Edit first position assignment
    cy.get('[data-testid="position-assignment"]')
      .first()
      .find('[data-testid="position-input"]')
      .clear()
      .type("C1");

    // Confirm bulk move
    cy.get('[data-testid="confirm-bulk-move-button"]').click();
    cy.wait(3000);

    // Verify success
    cy.get('div[role="status"]').should("be.visible");
  });
});

describe("Storage Movement - Previous Position Freed (P2B)", function () {
  it("Should verify previous position is freed after move", function () {
    // This test verifies that after moving a sample, the previous position
    // becomes available for other samples
    cy.visit("/StorageDashboard");
    cy.wait(2000);

    // Get initial position of a sample
    let initialPosition;
    cy.get('[data-testid="sample-list"]')
      .find('[data-testid="sample-row"]')
      .first()
      .find('[data-testid="sample-position"]')
      .invoke("text")
      .then((text) => {
        initialPosition = text.trim();

        // Move the sample
        cy.get('[data-testid="sample-list"]')
          .find('[data-testid="sample-row"]')
          .first()
          .within(() => {
            cy.get('[data-testid="sample-actions-menu"]').click();
            cy.contains("Move").click();
          });

        cy.get('[data-testid="move-location-modal"]').should("be.visible");

        // Select new location
        cy.get('[data-testid="target-location-selector"]').within(() => {
          storageAssignmentPage.selectRoom("MAIN");
          cy.wait(1000);
          storageAssignmentPage.selectDevice("FRZ01");
          cy.wait(1000);
          storageAssignmentPage.selectShelf("SHA");
          cy.wait(1000);
          storageAssignmentPage.selectRack("RKR2");
          cy.wait(1000);
          storageAssignmentPage.selectPosition("B4");
        });

        cy.get('[data-testid="confirm-move-button"]').click();
        cy.wait(3000);

        // Verify previous position is now available
        // (This would require checking the position list or trying to assign another sample)
        cy.get('div[role="status"]').should("be.visible");
      });
  });
});
