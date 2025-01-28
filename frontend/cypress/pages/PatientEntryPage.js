class PatientEntryPage {
  subjectNumber = "#subjectNumber";
  nationalId = "#nationalId";
  firstNameSelector = "#firstName";
  lastNameSelector = "#lastName";
  personContactLastName = "#patientContact.person.lastName";
  personContactFirstName = "#patientContact.person.firstName";
  personContactPrimaryPhone = "#patientContact.person.primaryPhone";
  personContactEmail = "#patientContact.person.email";
  patientIdSelector = "#patientId";
  labNoSelector = "#labNumber";
  town = "#city";
  street = "#streetAddress";
  camp = "#commune";
  region = "#health_region";
  district = "#health_district";
  education = "#education";
  maritalStatus = "#maritialStatus";
  nationality = "#nationality";
  otherNationality = "#otherNationality";
  primaryPhone = "#primaryPhone";
  dateOfBirth = "#date-picker-default-id";
  savePatientBtn = "#submit";
  previousLabNo = "#display_labNumber";

  constructor() {}

  visit() {
    cy.visit("/PatientManagement");
  }

  clickSearchPatientBtn() {
    cy.get("#search-patient-button").click();
  }

  clickSearchBtn() {
    cy.get("#local_search").click();
  }

  clickNewPatientBtn() {
    cy.get("#new-patient-button").click();
  }

  clickSavePatientButton() {
    cy.get(this.savePatientBtn).click();
  }

  getMaleGenderRadioButton() {
    cy.get("#search-radio-1").check({ force: true });
  }

  selectMaleGenderRadioButton() {
    cy.get("#radio-1").click({ force: true });
  }

  getExternalSearchButton() {
    cy.get("#external_search").should("be.disabled");
  }

  patientFirstName(firstName) {
    cy.enterText(this.firstNameSelector, firstName);
  }

  patientLastName(lastName) {
    cy.enterText(this.lastNameSelector, lastName);
  }

  patientDateOfBirth(dOB) {
    cy.get(this.dateOfBirth).find("input").clear().type(dOB);
  }
  getSubmitButton() {
    cy.get(this.savePatientBtn).click();
  }

  searchPatientByPatientId(PID) {
    cy.enterText(this.patientIdSelector, PID);
  }

  searchPatientBylabNo(labNo) {
    cy.enterText(this.previousLabNo, labNo);
  }

  enterUniquePatientNo(uniquePatientID) {
    cy.enterText(this.subjectNumber, uniquePatientID);
  }

  enterNationalID(nationalId) {
    cy.enterText(this.nationalId, nationalId);
  }

  enterPersonContactPrimaryPhone(personContactPrimaryPhone) {
    cy.enterText(this.personContactPrimaryPhone, personContactPrimaryPhone);
  }
  getPatientSearchResultsTable() {
    return cy.getElement(
      ".cds--data-table.cds--data-table--lg.cds--data-table--sort > tbody",
    );
  }

  emergencyDropDown() {
    cy.get("#emergency-contact").click();
  }

  emergencyContactLastName(personContactLastName) {
    cy.enterText(this.personContactLastName, personContactLastName);
  }

  emergencyContactFirstName(personContactFirstName) {
    cy.enterText(this.personContactLastName, personContactFirstName);
  }

  emergencyContactPhone(personContactPrimaryPhone) {
    cy.enterText(this.personContactPrimaryPhone, personContactPrimaryPhone);
  }

  emergencyContactEmail(personContactEmail) {
    cy.enterText(this.personContactEmail, personContactEmail);
  }

  additionalInformationDropDown() {
    cy.get("#additional-info").click();
  }

  enterTown(town) {
    cy.enterText(this.town, town);
  }

  enterStreet(street) {
    cy.enterText(this.street, street);
  }

  enterCamp(camp) {
    cy.enterText(this.camp, camp);
  }

  selectRegion(region) {
    cy.get(this.region).select(region);
  }

  selectDistrict(district) {
    cy.get(this.district).select(district);
  }

  selectEducation(education) {
    cy.get(this.education).select(education);
  }

  selectMaritalStatus(maritalStatus) {
    cy.get(this.maritalStatus).select(maritalStatus);
  }

  selectNationality(nationality) {
    cy.get(this.nationality).select(nationality);
  }

  selectOtherNationality(otherNationality) {
    cy.enterText(this.otherNationality, otherNationality);
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
