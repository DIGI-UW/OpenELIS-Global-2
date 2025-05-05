class BatchOrderEntry {
  visitSetupPage() {
    cy.get("h2").should("contain.text", "Batch Order Entry Setup");
  }

  checkNextButtonDisabled() {
    cy.get("[data-testid='next-button-BatchOrderEntry']").should("be.disabled");
  }

  selectForm(formTypeRoutine) {
    cy.get("#form-dropdown").select(formTypeRoutine);
  }

  selectSampleType(serumSample) {
    cy.get("#selectSampleType").should("be.visible").select(serumSample);
  }

  checkBilanPanel() {
    cy.contains("span", "Bilan Biochimique").click();
  }

  checkSerologiePanel() {
    cy.contains("span", "Serologie VIH").click();
  }

  checkDenguePCR() {
    cy.contains("span", "DENGUE PCR").click();
  }

  checkHIVViralLoad() {
    cy.contains("span", "HIV VIRAL LOAD").click();
  }

  checkCreatinine() {
    cy.contains("span", "Creatinine").click();
  }

  checkNextLabel() {
    return cy.get("[data-testid='next-button-BatchOrderEntry']");
  }

  //clickSavePatientButton() {
  //cy.contains('button', 'Save').click();
  // }

  clickGenerateButton() {
    cy.get("[data-testid='generate-barcode-btn-BatchOrderEntry']").click();
  }

  selectMethod(method) {
    cy.get("#method-dropdown").select(method);
  }

  checkFacilityCheckbox() {
    cy.get("#facility-checkbox").check({ force: true });
  }

  checkPatientCheckbox() {
    cy.get("#patient-checkbox").check({ force: true });
  }

  enterSiteName(siteName) {
    cy.get("#siteName").should("be.visible").type(siteName);
    cy.contains(".suggestion-active", siteName).click();
  }

  typeLabNumber(labNumber) {
    cy.wait(200);
    cy.get("#labNo").should("be.visible");
    cy.get("#labNo").type(labNumber);
  }

  uniqueHealthIDNum(healthID) {
    cy.get("#subjectNumber").type(healthID);
  }

  nationalID(nationalID) {
    cy.get("#nationalId").type(nationalID);
  }

  firstName(firstName) {
    cy.get("#firstName").type(firstName);
  }

  lastName(lastName) {
    cy.get("#lastName").type(lastName);
  }

  typePatientYears(years) {
    cy.get("#years").type(years);
  }

  typePatientMonths(months) {
    cy.get("#months").type(months);
  }

  typePatientDays(days) {
    cy.get("#days").type(days);
  }

  selectGender() {
    cy.contains("span", "Female").click();
  }
  checkNextButtonEnabled() {
    cy.get("[data-testid='next-button-BatchOrderEntry']").wait(500).click();
  }

  selectDNAPCRTest() {
    cy.get("#eid_dnaPCR").check({ force: true });
  }

  selectTubeSample() {
    cy.contains("span", "Dry Tube").click();
  }

  selectBloodSample() {
    cy.contains("span", "Dry Blood Spot").click();
  }

  clickNewPatientButton() {
    //the #newPatient is not responding
    cy.contains("button", "New Patient").should("be.visible");
    cy.contains("button", "New Patient").click();
  }

  clickSearchPatientButton() {
    //the #searchPatient is not responding
    cy.contains("button", "Search for Patient").should("be.visible");
    cy.contains("button", "Search for Patient").click();
  }

  localSearchButton() {
    cy.get("#local_search").click();
  }

  checkPatientRadio() {
    cy.get("#2").check({ force: true });
  }

  visitBatchOrderEntryPage() {
    cy.get("h2").should("contain.text", "Batch Order Entry");
  }

  clickGenerateAndSaveBarcode() {
    cy.get("[data-testid='generate-barcode-link-BatchOrderEntry']").click();
  }

  saveOrder() {
    cy.get("[data-testid='generate-barcode-btn-BatchOrderEntry']").click();
  }

  clickFinishButton() {
    cy.get("[data-cy='finishButton']").click();
  }
}

export default BatchOrderEntry;
