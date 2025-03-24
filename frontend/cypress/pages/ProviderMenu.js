class ProviderMenuPage {
  constructor() {
    this.selectors = {
      providerMenuHeader: "[data-cy='providerMenuHeader']",
      providerNameInput: "[data-cy='providerNameInput']",
      providerIdInput: "[data-cy='providerIdInput']",
      addressInput: "[data-cy='addressInput']",
      cityInput: "[data-cy='cityInput']",
      stateInput: "[data-cy='stateInput']",
      zipCodeInput: "[data-cy='zipCodeInput']",
      phoneNumberInput: "[data-cy='phoneNumberInput']",
      emailInput: "[data-cy='emailInput']",
      specializationDropdown: "[data-cy='specializationDropdown']",
      addSpecializationButton: "[data-cy='addSpecializationButton']",
      saveButton: "[data-cy='saveButton']",
      successMessage: "[data-cy='successMessage']",
    };
  }

  verifyProviderMenuPage() {
    cy.get(this.selectors.providerMenuHeader).should("be.visible");
  }

  fillProviderInformation(name = "Dr. John Doe", id = "12345") {
    cy.get(this.selectors.providerNameInput).clear().type(name);
    cy.get(this.selectors.providerIdInput).clear().type(id);
  }

  setProviderAddress(
    address = "123 Main St",
    city = "Sample City",
    state = "CA",
    zip = "12345",
  ) {
    cy.get(this.selectors.addressInput).clear().type(address);
    cy.get(this.selectors.cityInput).clear().type(city);
    cy.get(this.selectors.stateInput).clear().type(state);
    cy.get(this.selectors.zipCodeInput).clear().type(zip);
  }

  setProviderContactInfo(
    phone = "123-456-7890",
    email = "provider@example.com",
  ) {
    cy.get(this.selectors.phoneNumberInput).clear().type(phone);
    cy.get(this.selectors.emailInput).clear().type(email);
  }

  addProviderSpecialization(specialization = "Cardiology") {
    cy.get(this.selectors.specializationDropdown).select(specialization);
    cy.get(this.selectors.addSpecializationButton).click();
  }

  saveProviderInformation() {
    cy.get(this.selectors.saveButton).click();
  }

  verifySaveSuccess() {
    cy.get(this.selectors.successMessage).should(
      "contain",
      "Provider information saved successfully",
    );
  }
}

export default ProviderMenuPage;
