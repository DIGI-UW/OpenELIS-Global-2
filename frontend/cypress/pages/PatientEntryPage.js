class PatientEntryPage {
  subjectNumber = "#subjectNumber";
  nationalId = "#nationalId";
  firstNameSelector = "#firstName";
  lastNameSelector = "#lastName";
  patientPhoneNumb = "#primaryPhone";
  personContactLastName = "#emergency-lastname";
  personContactFirstName = "#emergency-firstname";
  personContactPrimaryPhone = "#emergency-phone";
  personContactEmail = "#emergency-email";
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
  previousLabNo = "[data-testid='prevLabNumber']";

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
    cy.get(this.firstNameSelector).type(firstName);
  }

  patientLastName(lastName) {
    cy.get(this.lastNameSelector).type(lastName);
  }

  patientPhoneNumber(patientPhone) {
    cy.get(this.patientPhoneNumber).type(patientPhone);
  }

  patientDateOfBirth(dOB) {
    cy.get(this.dateOfBirth).find("input").clear().type(dOB);
  }
  getSubmitButton() {
    cy.get(this.savePatientBtn).click();
  }

  searchPatientByPatientId(uniqueID) {
    cy.get(this.patientIdSelector).type(uniqueID);
  }

  searchPatientBylabNo(labNo) {
    cy.get(this.previousLabNo).type(labNo);
  }

  enterUniquePatientNo(uniqueID) {
    cy.get(this.subjectNumber).type(uniqueID);
  }

  enterNationalID(nationalId) {
    cy.get(this.nationalId).type(nationalId);
  }

  patientPhoneNumber(patientPhone) {
    cy.get(this.patientPhoneNumb).type(patientPhone);
  }

  emergencyDropDown() {
    cy.get("#emergency-contact").click();
  }

  emergencyContactLastName(personContactLastName) {
    cy.get(this.personContactLastName).type(personContactLastName);
  }

  emergencyContactFirstName(personContactFirstName) {
    cy.get(this.personContactFirstName).type(personContactFirstName);
  }

  emergencyContactPhone(personContactPrimaryPhone) {
    cy.get(this.personContactPrimaryPhone).type(personContactPrimaryPhone);
  }

  emergencyContactEmail(personContactEmail) {
    cy.get(this.personContactEmail).type(personContactEmail);
  }

  additionalInformationDropDown() {
    cy.get("#additional-info").click();
  }

  enterTown(town) {
    cy.get(this.town).type(town);
  }

  enterStreet(street) {
    cy.get(this.street).type(street);
  }

  enterCamp(camp) {
    cy.get(this.camp).type(camp);
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
    cy.get(this.otherNationality).type(otherNationality);
  }

  selectPatient() {
    cy.get("input#73").check();
    }
}

export default PatientEntryPage;
