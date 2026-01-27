class BatchTestReassignmentandCancelationPage {
  constructor() {
    this.selectors = {
      title: "h3",
      span: "span",
      sampleType: "#selectSampleType",
      save: "[data-cy='okButton']",
      cancel: "[data-cy='cancelButton']",
    };
  }

  validatePageTitle() {
    cy.get(this.selectors.title)
      .should("be.visible")
      .and("contain.text", "Batch test reassignment and cancelation");
  }

  selectSampleType() {
    cy.get(this.selectors.sampleType).select("Serum");
  }

  checkBoxes(boxName) {
    cy.get(`[for="${boxName}"]`).click();
  }

  selectTest(testNum) {
    cy.get(`#selectSampleType${testNum}`).select("Amylase");
  }

  clickOk() {
    cy.get(this.selectors.save).should("be.enabled").click();
  }

  clickCancel() {
    cy.get(this.selectors.cancel).click();
  }
}

export default BatchTestReassignmentandCancelationPage;
