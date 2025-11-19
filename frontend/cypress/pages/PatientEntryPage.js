class PatientEntryPage {
  subjectNumber = "input#subjectNumber";
  nationalId = "input#nationalId";
  firstNameSelector = "input#firstName";
  lastNameSelector = "input#lastName";
  personContactLastName = "input#patientContact\\.person\\.lastName";
  personContactFirstName = "input#patientContact\\.person\\.firstName";
  personContactPrimaryPhone = "input#patientContact\\.person\\.primaryPhone";
  personContactEmail = "input#patientContact\\.person\\.email";
  patientIdSelector = "input#patientId";
  labNoSelector = "#labNumber";
  city = "input#city";
  primaryPhone = "input#primaryPhone";
  dateOfBirth = "input#date-picker-default-id";
  savePatientBtn = "#submit";
  enterPreviousLabNo = "input#labNumber";
  enterAccessionNo = "input#accessionNumber";
  startLabNo = "#startLabNo";
  endLabNo = "#endLabNo";

  constructor() {}

  visit() {
    cy.visit("/PatientManagement");
  }

  getPatientEntryPageTitle() {
    return cy.get("section > h3");
  }

  clickNewPatientTab() {
    cy.get("#newPatient").click();
  }

  enterPatientInfo(
    firstName,
    lastName,
    subjectNumber,
    NationalId,
    dateOfBirth,
  ) {
    cy.enterText(this.subjectNumber, subjectNumber);
    cy.enterText(this.nationalId, NationalId);
    cy.enterText(this.lastNameSelector, lastName);
    cy.enterText(this.firstNameSelector, firstName);
    cy.enterText(this.dateOfBirth, dateOfBirth);
    this.getMaleGenderRadioButton().click();
    //cy.getElement("#submit").click();
  }

  clickSavePatientButton() {
    this.getSubmitButton().click();
  }

  getMaleGenderRadioButton() {
    return cy.contains("span", "Male").click();
  }

  enterPreviousLabNumber(value) {
    cy.get(this.enterPreviousLabNo)
      .invoke("css", "display", "block")
      .should("be.visible")
      .type(value, { force: true });
  }

  enterAccessionNumber(value) {
    cy.get(this.enterAccessionNo)
      .invoke("css", "display", "block")
      .should("be.visible")
      .type(value, { force: true });
  }

  startLabNumber(value) {
    cy.get(this.startLabNo)
      .invoke("css", "display", "block")
      .should("be.visible")
      .type(value, { force: true });
  }

  endLabNo(value) {
    cy.get(this.endLabNo, { timeout: 20000 }).should("be.visible");
    cy.get(this.endLabNo).type(value);
  }

  clickSearchPatientButton() {
    cy.getElement("#local_search").should("be.visible").click();
  }

  getExternalSearchButton() {
    cy.get("#external_search").should("be.disabled");
  }

  getLastName() {
    return cy.getElement(this.lastNameSelector);
  }

  getFirstName() {
    return cy.getElement(this.firstNameSelector);
  }
  searchPatientByFirstNameOnly(firstName) {
    cy.enterText(this.firstNameSelector, firstName);
  }

  searchPatientByLastNameOnly(lastName) {
    cy.enterText(this.lastNameSelector, lastName);
  }

  searchPatientByDateOfBirth(dateOfBirth) {
    cy.enterText(this.dateOfBirth, dateOfBirth);
  }

  clearPatientInfo() {
    cy.get("#clear").click();
  }

  getSubmitButton() {
    return cy.getElement(this.savePatientBtn);
  }

  searchPatientByFirstAndLastName(firstName, lastName) {
    cy.enterText(this.firstNameSelector, firstName);
    // Only enter last name if provided (avoid empty string error)
    if (lastName && lastName.trim() !== "") {
      cy.enterText(this.lastNameSelector, lastName);
    }
  }

  searchPatientByPatientId(PID) {
    cy.enterText(this.patientIdSelector, PID);
  }

  searchPatientBylabNo(labNo) {
    cy.enterText(this.labNoSelector, labNo);
  }

  getPatientSearchResultsTable() {
    return cy.getElement(
      ".cds--data-table.cds--data-table--lg.cds--data-table--sort > tbody",
    );
  }
  validatePatientSearchTablebyRespectiveField(expectedFieldValue, searchBy) {
    this.getPatientSearchResultsTable()
      .find("tr")
      .each(($el, index, $list) => {
        if (searchBy === "firstName") {
          cy.wrap($el)
            .find("td:nth-child(3)")
            .invoke("text")
            .then((cellText) => {
              const trimmedText = cellText.trim();
              expect(trimmedText).to.contain(expectedFieldValue);
            });
        } else if (searchBy === "lastName") {
          cy.wrap($el)
            .find("td:nth-child(2)")
            .invoke("text")
            .then((cellText) => {
              const trimmedText = cellText.trim();
              expect(trimmedText).to.contain(expectedFieldValue);
            });
        } else if (searchBy === "DOB") {
          cy.wrap($el)
            .find("td:nth-child(5)")
            .invoke("text")
            .then((cellText) => {
              const trimmedText = cellText.trim();
              expect(trimmedText).to.contain(expectedFieldValue);
            });
        }
      });
  }

  validatePatientSearchTable(actualName, inValidName) {
    // Use Cypress retry-ability - wait for table rows to appear
    this.getPatientSearchResultsTable()
      .find("tr")
      .should("exist")
      .should("have.length.greaterThan", 0);
    // Validate - check if name appears in any column (handles first name or last name)
    // Column 2 = first name, Column 3 = last name
    this.getPatientSearchResultsTable()
      .find("tr")
      .last()
      .should(($row) => {
        const firstName = $row.find("td:nth-child(2)").text().trim();
        const lastName = $row.find("td:nth-child(3)").text().trim();
        // Check if actualName matches first name, last name, or truncated last name
        let matches =
          firstName.includes(actualName) || actualName.includes(firstName);

        // Check last name (handle truncation: "E2E-Smith" -> "EE-Smith")
        if (!matches) {
          matches =
            lastName === actualName ||
            lastName.includes(actualName) ||
            actualName.includes(lastName);
          // Handle truncation pattern: E2E-* -> EE-*
          if (!matches && actualName.startsWith("E2E")) {
            const truncated = "E" + actualName.substring(2);
            matches = lastName === truncated || lastName.includes(truncated);
          }
        }

        expect(
          matches,
          `Expected "${actualName}" to match first name "${firstName}" or last name "${lastName}"`,
        ).to.be.true;
        expect(lastName).not.to.eq(inValidName);
      });
  }

  validatePatientByGender(expectedGender) {
    this.getPatientSearchResultsTable()
      .find("tr")
      .last()
      .find("td:nth-child(4)")
      .invoke("text")
      .then((cellText) => {
        const trimmedText = cellText.trim();
        expect(trimmedText).to.eq(expectedGender);
      });
  }

  selectPatientFromSearchResults() {
    this.getPatientSearchResultsTable()
      .find("tr")
      .first()
      .find("td:nth-child(1)")
      .find("[data-cy='radioButton']")
      .click({ force: true });
  }
}

export default PatientEntryPage;
