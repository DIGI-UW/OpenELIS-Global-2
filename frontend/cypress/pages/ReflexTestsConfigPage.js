class ReflexTestsConfigPage {
  constructor() {
    this.selectors = {
      calcPage: "[data-cy='calculatedValue']",
      reflexMgnt: "[data-cy='reflex']",
      ruleName: '[id="0_rulename"]',
      toggleSwitch: ".cds--toggle__switch",
      validateToggle: ".cds--toggle__text",
      overAllOption: '[id="0_overall"]',
      selectSample: "[data-cy='addSample']",
      searchTest: '[id="0_0_conditionTestId"]',
      relation: '[id="0_0_relation"]',
      numericValue: '[id="0_0_value"]',
      addIntNote: '[id="0_0_inote"]',
      addExtNote: '[id="0_0_xnote"]',
      reflexTest: '[id="0_0_reflexTestId"]',
      submit: '[id="submit_0"]',
      secondSample: "[data-cy='selectSample']",
      addRule: "[data-cy='rule']",
      autosuggestion: ".suggestion-active",
      calcName: '[id="0_name"]',
      removeOperation: '[id="0_removeoperation"]',
      searchNumTest: '[id="0_0_testresult"]',
      insertOperation: '[id="0_2_addoperation"]',
      mathFunctionButton: '[id="0_mathfunction"]',
      mathFunction: '[id="0_1_mathfunction"]',
      secMathFunction: '[id="0_3_mathfunction"]',
      insertSecOperation: '[id="0_1_addoperation"]',
      integerButton: '[id="0_integer"]',
      integer: '[id="0_4_integer"]',
      insertThiOperation: '[id="0_2_addoperation"]',
      patientAttributeButton: '[id="0_patientattribute"]',
      patientAttribute: '[id="0_2_patientattribute"]',
      insertFouOperation: '[id="0_3_addoperation"]',
      thirdSample: '[id="0_sample"]',
      fourthSample: "[data-cy='add-sample']",
      finalResult: '[id="0_finalresult"]',
      addNote: '[id="0_note"]',
    };
  }

  reloadAndWait() {
    cy.reload();
    cy.wait(200);
  }

  verifyPageLoads(value) {
    cy.contains("h2", value).should("be.visible");
  }
  //Reflex Tests Management

  enterRuleName(value) {
    cy.get(this.selectors.ruleName).type(value);
  }

  validateRuleName(value) {
    cy.get(this.selectors.ruleName).should("have.value", value);
  }

  validateToggleStatus(value) {
    cy.contains(this.selectors.validateToggle, value);
  }

  selectOverAllOptions(value) {
    cy.get(this.selectors.overAllOption).select(value);
  }

  selectSample(value) {
    cy.get(this.selectors.selectSample).select(value);
  }

  searchTest(value) {
    cy.get(this.selectors.searchTest).type(value);
    cy.contains(this.selectors.autosuggestion, value).click();
  }

  selectRelation(value) {
    cy.get(this.selectors.relation).select(value);
  }

  enterNumericValue(value) {
    cy.get(this.selectors.numericValue).type(value);
  }

  selectSecondSample(value) {
    cy.get(this.selectors.secondSample).select(value);
  }

  searchReflexTest(value) {
    cy.get(this.selectors.reflexTest).type(value);
  }

  addInternatNote(value) {
    cy.get(this.selectors.addIntNote).type(value);
  }

  addExternatNote(value) {
    cy.get(this.selectors.addExtNote).type(value);
  }

  submitButton() {
    cy.get(this.selectors.submit).click();
  }

  addRule() {
    cy.get(this.selectors.addRule).click();
  }

  //Calculated Value Tests Management

  enterCalcName(value) {
    cy.get(this.selectors.calcName).type(value);
  }

  validateCalcName(value) {
    cy.get(this.selectors.calcName).should("have.value", value);
  }

  selectMathFunction(value) {
    cy.get(this.selectors.insertOperation).select(value);
  }

  verifyRemoveOperationButton() {
    cy.get(this.selectors.removeOperation).should("be.visible");
  }

  searchNumTest(value) {
    cy.get(this.selectors.searchNumTest).type(value);
    cy.contains(this.selectors.autosuggestion, value).click();
  }

  mathFunction(value) {
    cy.get(this.selectors.mathFunction).select(value);
  }

  secMathFunction(value) {
    cy.get(this.selectors.secMathFunction).select(value);
  }

  clickMathFunctionButton() {
    cy.get(this.selectors.mathFunctionButton).click();
  }

  clickIntegerButton() {
    cy.get(this.selectors.integerButton).click();
  }

  enterInteger(value) {
    cy.get(this.selectors.integer).type(value);
  }

  clickPatientAttributeButton() {
    cy.get(this.selectors.patientAttributeButton).click();
  }

  selectPatientAttribute(value) {
    cy.get(this.selectors.patientAttribute).select(value);
  }

  selectThirdSample(value) {
    cy.get(this.selectors.thirdSample).select(value);
  }

  selectFourthSample(value) {
    cy.get(this.selectors.fourthSample).select(value);
  }

  enterFinalResult(value) {
    cy.get(this.selectors.finalResult).type(value);
    cy.contains(this.selectors.autosuggestion, value).click();
  }

  addFinalExternatNote(value) {
    cy.get(this.selectors.addNote).type(value);
  }
}
export default ReflexTestsConfigPage;
