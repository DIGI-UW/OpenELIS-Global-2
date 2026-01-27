/**
 * Page Object for Storage Assignment workflows
 * Provides reusable methods for Cypress E2E tests
 */
class StorageAssignmentPage {
  getStorageLocationSelector() {
    return cy.get('[data-testid="storage-location-selector"]');
  }

  getRoomDropdown() {
    // LocationManagementModal uses EnhancedCascadingMode with comboboxes
    return cy.get(
      '[data-testid="room-combobox"], [data-testid="room-dropdown"]',
    );
  }

  getDeviceDropdown() {
    // LocationManagementModal uses EnhancedCascadingMode with comboboxes
    return cy.get(
      '[data-testid="device-combobox"], [data-testid="device-dropdown"]',
    );
  }

  getShelfDropdown() {
    // LocationManagementModal uses EnhancedCascadingMode with comboboxes
    return cy.get(
      '[data-testid="shelf-combobox"], [data-testid="shelf-dropdown"]',
    );
  }

  getRackDropdown() {
    // LocationManagementModal uses EnhancedCascadingMode with comboboxes
    return cy.get(
      '[data-testid="rack-combobox"], [data-testid="rack-dropdown"]',
    );
  }

  getPositionDropdown() {
    return cy.get('[data-testid="position-dropdown"]');
  }

  selectRoom(roomName) {
    cy.intercept("GET", "**/rest/storage/devices**").as("waitForDevices");

    // Type exact room name to trigger selection (typing partial name creates new)
    this.getRoomDropdown().clear().type(roomName, { delay: 50 });

    cy.wait("@waitForDevices", { timeout: 5000 });
    return this;
  }

  selectDevice(deviceName) {
    cy.intercept("GET", "**/rest/storage/shelves**").as("waitForShelves");

    // Type exact device name to trigger selection
    this.getDeviceDropdown().clear().type(deviceName, { delay: 50 });

    cy.wait("@waitForShelves", { timeout: 5000 });
    return this;
  }

  selectShelf(shelfLabel) {
    cy.intercept("GET", "**/rest/storage/racks**").as("waitForRacks");

    // Type exact shelf label to trigger selection
    this.getShelfDropdown().clear().type(shelfLabel, { delay: 50 });

    cy.wait("@waitForRacks", { timeout: 5000 });
    return this;
  }

  selectRack(rackLabel) {
    // Type exact rack label to trigger selection
    this.getRackDropdown().clear().type(rackLabel, { delay: 50 });

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
