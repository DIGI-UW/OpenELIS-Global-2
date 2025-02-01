import { expect } from "@playwright/test";

class Result {
  constructor(page) {
    this.page = page;
  }

  async getResultTitle() {
    return this.page.locator("section > h3");
  }

  async selectUnitType(unitType) {
    await this.page.selectOption("#unitType", unitType);
  }

  async acceptSample(index = 0) {
    

    await this.page.locator(".cds--checkbox-label").nth(index).click();
  }

  async acceptResult() {
    await (
      await this.page
        .getByRole("row", { name: "Smith John M 05/12/2001" })
        .locator("label span")
    ).click();
  }

  async enableAndCheckReferTestCheckbox() {
    // Enable the checkbox by removing the "disabled" attribute
    await this.page.evaluate(() => {
      document
        .querySelectorAll("input[type='checkbox']")
        .forEach((checkbox) => {
          checkbox.removeAttribute("disabled");
        });
    });

    // Now check the checkbox
    await this.page
      .locator(".cds--checkbox-label")
      .filter({ hasText: "Refer test to a reference lab" })
      .check();
  }

  async expandSampleDetails(index = 0) {
    await this.page.locator(`[data-testid="expander-button-${index}"]`).click();
  }

  async selectTestMethod(index = 0, method) {
    await this.page.selectOption(`#testMethod${index}`, method);
  }

  async selectPatient() {
    await this.page
      .locator(
        "tbody  :nth-child(1)  :nth-child(1) .cds--radio-button-wrapper  .cds--radio-button__label  .cds--radio-button__appearance",
      )
      .click();
  }

  async search() {
    await this.page
      .locator(":nth-child(1) > :nth-child(5) > .cds--btn")
      .click();
  }

  async searchByTest() {
    await this.page.locator(":nth-child(8) > #submit").click();
  }

  async validatePatientResult(patient) {
    await expect(
      this.page.locator("tbody > :nth-child(1) > :nth-child(2)"),
    ).toContainText(patient.lastName);
    await expect(
      this.page.locator("tbody > :nth-child(1) > :nth-child(3)"),
    ).toContainText(patient.firstName);
  }

  async referSample(index = 0, reason, institute) {
    await this.page.selectOption(`#referralReason${index}`, reason);
    await this.page.selectOption(`#institute${index}`, institute);
  }

  async selectRefferedTest() {
    await this.page
      .locator(
        "tbody > tr > .cds--table-column-checkbox > .cds--checkbox--inline > .cds--checkbox-label",
      )
      .click();
  }

  async selectAnalysisStatus(status) {
    await this.page.selectOption("#analysisStatus", status);
  }

  async selectTestName(testName) {
    await this.page.selectOption("#testName", testName);
  }

  async printReport() {
    await this.page
      .locator(":nth-child(6) > :nth-child(2) > .cds--btn")
      .click();
  }

  async setResultValue(index = 0, value) {
    await this.page.selectOption(`#resultValue${index}`, value);
  }

  async submitResults() {
    await this.page.getByRole("button", { name: "Save" }).click();
  }
}

export default Result;
