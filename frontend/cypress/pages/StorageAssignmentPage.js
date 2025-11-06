/**
 * Page Object for Storage Assignment workflows
 * Provides reusable methods for Cypress E2E tests
 */
class StorageAssignmentPage {
  getStorageLocationSelector() {
    return cy.get('[data-testid="storage-location-selector"]');
  }

  getRoomDropdown() {
    return cy.get('[data-testid="room-dropdown"]');
  }

  getDeviceDropdown() {
    return cy.get('[data-testid="device-dropdown"]');
  }

  getShelfDropdown() {
    return cy.get('[data-testid="shelf-dropdown"]');
  }

  getRackDropdown() {
    return cy.get('[data-testid="rack-dropdown"]');
  }

  getPositionDropdown() {
    return cy.get('[data-testid="position-dropdown"]');
  }

  selectRoom(roomName) {
    this.getRoomDropdown().click();
    cy.wait(500);
    // Try exact match first, then partial match
    cy.get("body").then(($body) => {
      if ($body.text().includes(roomName)) {
        cy.contains(roomName).click();
      } else if (
        roomName === "MAIN" &&
        $body.text().includes("Main Laboratory")
      ) {
        cy.contains("Main Laboratory").click();
      } else {
        cy.log(`Room "${roomName}" not found in dropdown`);
      }
    });
    return this;
  }

  selectDevice(deviceName) {
    this.getDeviceDropdown().click();
    cy.contains(deviceName).click();
    return this;
  }

  selectShelf(shelfLabel) {
    this.getShelfDropdown().click();
    cy.contains(shelfLabel).click();
    return this;
  }

  selectRack(rackLabel) {
    this.getRackDropdown().click();
    cy.contains(rackLabel).click();
    return this;
  }

  selectPosition(coordinate) {
    this.getPositionDropdown().click();
    cy.contains(coordinate).click();
    return this;
  }

  enterPositionManually(coordinate) {
    cy.get('[data-testid="position-input"]').type(coordinate);
    return this;
  }

  clickSave() {
    cy.get('[data-testid="save-button"]').click();
    return this;
  }

  getHierarchicalPath() {
    return cy.get('[data-testid="location-path"]');
  }

  getCapacityWarning() {
    return cy.get('[data-testid="capacity-warning"]');
  }
}

export default StorageAssignmentPage;
