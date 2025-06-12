class BarcodeConfigPage {
  constructor() {
    this.selectors = {
      order: "#order",
      specimen: "#specimen",
      maxOrder: "#maxOrder",
      maxSpecimen: "#maxSpecimen",
      collectionDateCheck: "#collectionDateCheck",
      collectedBy: "#collectedBy",
      tests: "#tests",
      patientsexfull: "#patientsexfull",
      checkBox: "#checkBox",
      heightOrder: "#height-order",
      heightSpecimen: "#height-specimen",
      widthOrder: "#width-order",
      widthSpecimen: "#width-specimen",
      heightBlock: "#height-block",
      heightSlide: "#height-slide",
      widthBlock: "#width-block",
      widthSlide: "#width-slide",
      saveButton: "button:contains('Save')",
      sampleType: "select#selectSampleType",
      prePrintButton: "button[data-cy='pre-Print']",
      submitButton: "button[data-cy='submitButton']",
    };
  }

  /**
   * Sets the default order value.
   * @param {string} value - The value to set for the default order (default: "2").
   */
  captureDefaultOrder(value = "2") {
    cy.log(`Setting default order to ${value}`);
    cy.get(this.selectors.order).clear().type(value);
  }

  /**
   * Sets the default specimen value.
   * @param {string} value - The value to set for the default specimen (default: "1").
   */
  captureDefaultSpecimen(value = "1") {
    cy.log(`Setting default specimen to ${value}`);
    cy.get(this.selectors.specimen).clear().type(value);
  }

  /**
   * Sets the max order value.
   * @param {string} value - The value to set for the max order (default: "10").
   */
  captureMaxOrder(value = "10") {
    cy.log(`Setting max order to ${value}`);
    cy.get(this.selectors.maxOrder).clear().type(value);
  }

  /**
   * Sets the max specimen value.
   * @param {string} value - The value to set for the max specimen (default: "1").
   */
  captureMaxSpecimen(value = "1") {
    cy.log(`Setting max specimen to ${value}`);
    cy.get(this.selectors.maxSpecimen).clear().type(value);
  }

  /**
   * Toggles a checkbox.
   * @param {string} selector - The selector for the checkbox.
   * @param {boolean} check - Whether to check (true) or uncheck (false) the checkbox.
   */
  toggleCheckbox(selector, check = true) {
    const action = check ? "check" : "uncheck";
    cy.log(
      `${action === "check" ? "Checking" : "Unchecking"} checkbox: ${selector}`,
    );
    cy.get(selector)
      [action]({ force: true })
      .should(check ? "be.checked" : "not.be.checked");
  }

  checkCheckBoxes() {
    this.toggleCheckbox(this.selectors.collectionDateCheck, true);
    this.toggleCheckbox(this.selectors.collectedBy, true);
    this.toggleCheckbox(this.selectors.tests, true);
    this.toggleCheckbox(this.selectors.patientsexfull, true);
    this.toggleCheckbox(this.selectors.checkBox, true);
  }

  uncheckCheckBoxes() {
    this.toggleCheckbox(this.selectors.collectionDateCheck, false);
    this.toggleCheckbox(this.selectors.collectedBy, false);
    this.toggleCheckbox(this.selectors.tests, false);
    this.toggleCheckbox(this.selectors.patientsexfull, false);
    this.toggleCheckbox(this.selectors.checkBox, false);
  }

  dimensionsBarCodeLabel() {
    cy.get(this.selectors.heightOrder).clear().type("25.4");
    cy.get(this.selectors.heightSpecimen).clear().type("25.4");
    cy.get(this.selectors.widthOrder).clear().type("76.2");
    cy.get(this.selectors.widthSpecimen).clear().type("76.2");
    cy.get(this.selectors.heightBlock).clear().type("25.4");
    cy.get(this.selectors.heightSlide).clear().type("25.4");
    cy.get(this.selectors.widthBlock).clear().type("76.4");
    cy.get(this.selectors.widthSlide).clear().type("76.4");
  }

  saveChanges() {
    cy.log("Saving changes");
    cy.get(this.selectors.saveButton).should("be.visible").click();
  }

  verifySaveSuccess() {
    cy.contains("BarCode Configurations has been saved").should("be.visible");
  }

  validatePage() {
    cy.contains("h3", "Print Bar Code Labels");
  }

  selectSampleTypeOption(value) {
    cy.get(this.selectors.sampleType).select(value);
  }

  clickPrePrintButton() {
    cy.get(this.selectors.prePrintButton).should("be.enabled");
  }

  clickSubmitButton() {
    cy.get(this.selectors.submitButton).should("be.enabled").click();
  }
}

export default BarcodeConfigPage;
