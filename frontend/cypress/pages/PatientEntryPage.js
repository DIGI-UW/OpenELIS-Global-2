class PatientEntryPage {
  subjectNumber = "input#subjectNumber";
  nationalId = "input#nationalId";
  firstNameSelector = "#firstName";
  lastNameSelector = "#lastName";
  personContactLastName = "#patientContact\\.person\\.lastName";
  personContactFirstName = "#patientContact\\.person\\.firstName";
  personContactPrimaryPhone = "#patientContact\\.person\\.primaryPhone";
  personContactEmail = "input#patientContact\\.person\\.email";
  patientIdSelector = "#patientId";
  labNoSelector = "#labNumber";
  city = "input#city";
  primaryPhone = "#primaryPhone";
  dateOfBirth = "#date-picker-default-id";
  savePatientBtn = "#submit";
  previousLabNo = "#display_labNumber";

  constructor() {}

  visit() {
    cy.visit("/PatientManagement");
  }

  clickSearchPatientBtn() {
    cy.get("#root > div > div.cds--white.cds--layer-one > main > div.orderLegendBody > div > div:nth-child(1) > button").click();
  }

  clickSearchBtn(){
    cy.get("#local_search").click();
  }

  clickNewPatientBtn() {
    cy.get('[data-cy="new-patient-button"]').click();
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
    cy.getElement("#submit").click();
  }

  clickSavePatientButton() {
    this.getSubmitButton().click();
  }

  getMaleGenderRadioButton() {
    cy.get("#search-radio-1").check();
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
  getSubmitButton() {
    return cy.getElement(this.savePatientBtn);
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
    this.getPatientSearchResultsTable()
      .find("tr")
      .last()
      .find("td:nth-child(3)")
      .invoke("text")
      .then((cellText) => {
        const trimmedText = cellText.trim();
        expect(trimmedText).to.contain(actualName);
        expect(trimmedText).not.eq(inValidName);
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
      .click();
  }
} 

export default PatientEntryPage;
