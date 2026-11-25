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

  chooseFromSearchableList(selector, name) {
    cy.get(selector).clear().type(name);
    cy.contains('[role="option"]', new RegExp(`^${name}$`)).click();
    cy.get(selector).should("have.value", name);
  }

  selectSampleType() {
    this.chooseFromSearchableList(this.selectors.sampleType, "Serum");
  }

  checkBoxes(boxName) {
    cy.get(`[for="${boxName}"]`).click();
  }

  selectTest(testNum) {
    cy.get(`#selectSampleType${testNum}`).clear().type("Amylase");
    cy.contains('[role="option"]', /^Amylase$/).click();
  }

  clickOk() {
    cy.get(this.selectors.save).should("be.enabled").click();
  }

  clickCancel() {
    cy.get(this.selectors.cancel).click();
  }
}

export default BatchTestReassignmentandCancelationPage;
