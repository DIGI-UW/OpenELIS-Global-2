class PatientMenuConfigPage {
  constructor() {}

  // This method is used to visit the page
  visit() {
    cy.visit("/administration#patientMenuManagement");
  }

  turnOffToggleSwitch() {
    cy.get("div.cds--toggle__switch").click();
  }

  turnOnToggleSwitch() {
    cy.get("div.cds--toggle label div > div").should("be.visible").click();
  }

  submitButton() {
    cy.contains("button", "Submit").click();
  }
}

export default PatientMenuConfigPage;
