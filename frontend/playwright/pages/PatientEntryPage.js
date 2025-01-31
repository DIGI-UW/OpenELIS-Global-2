import { expect } from "@playwright/test";

class PatientEntryPage {
  constructor(page) {
    this.page = page;
    this.subjectNumber = "input#subjectNumber";
    this.nationalId = "input#nationalId";
    this.firstNameSelector = "input#firstName";
    this.lastNameSelector = "input#lastName";
    this.personContactLastName = "input#patientContact\\.person\\.lastName";
    this.personContactFirstName = "input#patientContact\\.person\\.firstName";
    this.personContactPrimaryPhone =
      "input#patientContact\\.person\\.primaryPhone";
    this.personContactEmail = "input#patientContact\\.person\\.email";
    this.patientIdSelector = "input#patientId";
    this.labNoSelector = "#labNumber";
    this.city = "input#city";
    this.primaryPhone = "input#primaryPhone";
    this.dateOfBirth = "input#date-picker-default-id";
    this.savePatientBtn = "#submit";
  }

  async visit() {
    await this.page.goto("/PatientManagement");
  }

  async getPatientEntryPageTitle() {
    return this.page.locator("section section h3");
  }

  async clickNewPatientTab() {
    await this.page
      .locator(":nth-child(1) > :nth-child(2) > .cds--btn")
      .click();
  }

  async getMaleGenderRadioButton() {
    return this.page.locator("label").filter({ hasText: /^Male$/ });
  }
  async enterPatientInfo(
    firstName,
    lastName,
    subjectNumber,
    NationalId,
    dateOfBirth,
  ) {
    await this.page.locator(this.subjectNumber).fill(subjectNumber);
    await this.page.locator(this.nationalId).fill(NationalId);
    await this.page.locator(this.lastNameSelector).fill(lastName);
    await this.page.locator(this.firstNameSelector).fill(firstName);
    await this.page.locator(this.dateOfBirth).fill(dateOfBirth);
    await (await this.getMaleGenderRadioButton()).click();
    await this.page.locator("#submit").click();
  }

  async clickSavePatientButton() {
    await (await this.getSubmitButton()).click();
  }
  async clickSearchPatientButton() {
    await this.page.locator("#local_search").click();
  }

  async getExternalSearchButton() {
    await expect(this.page.locator("#external_search")).toBeDisabled();
  }

  async getLastName() {
    return this.page.locator(this.lastNameSelector);
  }

  async getFirstName() {
    return this.page.locator(this.firstNameSelector);
  }

  async searchPatientByFirstNameOnly(firstName) {
    await this.page.locator(this.firstNameSelector).fill(firstName);
  }

  async searchPatientByLastNameOnly(lastName) {
    await this.page.locator(this.lastNameSelector).fill(lastName);
  }

  async searchPatientByDateOfBirth(dateOfBirth) {
    await this.page.locator(this.dateOfBirth).fill(dateOfBirth);
  }

  async getSubmitButton() {
    return this.page.locator(this.savePatientBtn);
  }

  async searchPatientByFirstAndLastName(firstName, lastName) {
    await this.page.locator(this.firstNameSelector).fill(firstName);
    await this.page.locator(this.lastNameSelector).fill(lastName);
  }

  async searchPatientBylabNo(labNo) {
    await this.page
      .getByRole("textbox", { name: "Previous Lab Number" })
      .fill(labNo);
  }
  async searchPatientByPatientId(PID) {
    await this.page.locator(this.patientIdSelector).fill(PID);
  }

  async getPatientSearchResultsTable() {
    return this.page.locator(
      ".cds--data-table.cds--data-table--lg.cds--data-table--sort > tbody",
    );
  }

  async validatePatientSearchTablebyRespectiveField(
    expectedFieldValue,
    searchBy,
  ) {
    const rows = await this.page
      .locator(
        ".cds--data-table.cds--data-table--lg.cds--data-table--sort > tbody > tr",
      )
      .all();
    for (const row of rows) {
      if (searchBy === "firstName") {
        const cellText = await row.locator("td:nth-child(3)").textContent();
        expect(cellText.trim()).toContain(expectedFieldValue);
      } else if (searchBy === "lastName") {
        const cellText = await row.locator("td:nth-child(2)").textContent();
        expect(cellText.trim()).toContain(expectedFieldValue);
      } else if (searchBy === "DOB") {
        const cellText = await row.locator("td:nth-child(5)").textContent();
        expect(cellText.trim()).toContain(expectedFieldValue);
      }
    }
  }

  async validatePatientSearchTable(actualName, inValidName) {
    const lastRow = await this.page
      .locator(
        ".cds--data-table.cds--data-table--lg.cds--data-table--sort > tbody > tr",
      )
      .last();
    const cellText = await lastRow.locator("td:nth-child(3)").textContent();
    expect(cellText.trim()).toContain(actualName);
    expect(cellText.trim()).not.toEqual(inValidName);
  }

  async validatePatientByGender(expectedGender) {
    const lastRow = await this.page
      .locator(
        ".cds--data-table.cds--data-table--lg.cds--data-table--sort > tbody > tr",
      )
      .last();
    const cellText = await lastRow.locator("td:nth-child(4)").textContent();
    expect(cellText.trim()).toEqual(expectedGender);
  }

  async selectPatientFromSearchResults() {
    const firstRow = await this.page
      .locator(
        ".cds--data-table.cds--data-table--lg.cds--data-table--sort > tbody > tr",
      )
      .first();
    await firstRow.locator("td:nth-child(1)").click();
  }
}

export default PatientEntryPage;
