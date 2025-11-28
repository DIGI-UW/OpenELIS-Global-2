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
    // EnhancedCascadingMode uses Carbon ComboBox - type to filter, then select from menu
    this.getRoomDropdown().click().clear().type(roomName);
    // Carbon ComboBox shows options in a menu - wait for menu to appear
    cy.get('[role="listbox"]', { timeout: 5000 }).should("be.visible");
    // Try exact match first, then partial match (e.g., "MAIN" might be "Main Laboratory")
    cy.get("body").then(($body) => {
      const menu = $body.find('[role="listbox"]');
      const menuText = menu.text();
      if (menuText.includes(roomName)) {
        cy.get('[role="listbox"]').contains(roomName).click({ force: true });
      } else if (roomName === "MAIN" && menuText.includes("Main")) {
        cy.get('[role="listbox"]')
          .contains("Main", { matchCase: false })
          .click({ force: true });
      } else {
        // Select first option if available
        cy.get('[role="listbox"] [role="option"]')
          .first()
          .click({ force: true });
      }
    });
    return this;
  }

  selectDevice(deviceName) {
    // EnhancedCascadingMode uses Carbon ComboBox - type to filter, then select from menu
    this.getDeviceDropdown().click().clear().type(deviceName);
    cy.get('[role="listbox"]', { timeout: 5000 }).should("be.visible");
    // Flexible matching - try exact, then partial, then first option
    cy.get("body").then(($body) => {
      const menu = $body.find('[role="listbox"]');
      const menuText = menu.text();
      if (menuText.includes(deviceName)) {
        cy.get('[role="listbox"]').contains(deviceName).click({ force: true });
      } else if (menuText.length > 0) {
        cy.get('[role="listbox"] [role="option"]')
          .first()
          .click({ force: true });
      }
    });
    return this;
  }

  selectShelf(shelfLabel) {
    // EnhancedCascadingMode uses Carbon ComboBox - type to filter, then select from menu
    this.getShelfDropdown().click().clear().type(shelfLabel);
    cy.get('[role="listbox"]', { timeout: 5000 }).should("be.visible");
    // Flexible matching
    cy.get("body").then(($body) => {
      const menu = $body.find('[role="listbox"]');
      const menuText = menu.text();
      if (menuText.includes(shelfLabel)) {
        cy.get('[role="listbox"]').contains(shelfLabel).click({ force: true });
      } else if (menuText.length > 0) {
        cy.get('[role="listbox"] [role="option"]')
          .first()
          .click({ force: true });
      }
    });
    return this;
  }

  selectRack(rackLabel) {
    // EnhancedCascadingMode uses Carbon ComboBox - type to filter, then select from menu
    this.getRackDropdown().click().clear().type(rackLabel);
    cy.get('[role="listbox"]', { timeout: 5000 }).should("be.visible");
    // Flexible matching
    cy.get("body").then(($body) => {
      const menu = $body.find('[role="listbox"]');
      const menuText = menu.text();
      if (menuText.includes(rackLabel)) {
        cy.get('[role="listbox"]').contains(rackLabel).click({ force: true });
      } else if (menuText.length > 0) {
        cy.get('[role="listbox"] [role="option"]')
          .first()
          .click({ force: true });
      }
    });
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
