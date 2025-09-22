class TestManagementPage {
  constructor() {
    this.selectors = {
      title: "h2",
      span: "span",
      testName: "#TestRenameEntry",
      testCatalog: "#TestCatalog",
      toggleSwitch: "div.cds--toggle__switch",
      selectTests: "#carbon-multiselect-example-3-input",
      checkAll: "#carbon-multiselect-example-3-item-0-item",
    };
  }

  validatePageTitle(title) {
    cy.get(this.selectors.title)
      .should("be.visible")
      .and("contain.text", title);
  }

  clickTestName() {
    cy.get(this.selectors.testName).click();
  }

  clickTestCatalog() {
    cy.get(this.selectors.testCatalog).click();
  }

  toggleSwitch() {
    cy.get(this.selectors.toggleSwitch).click();
  }

  clickButton(index) {
    cy.get(`#button-${index}`).click();
  }

  selectTests() {
    cy.get(this.selectors.selectTests).click();
    cy.get(this.selectors.checkAll).click();
  }

  button(buttonType) {
    cy.contains("button", buttonType).click();
  }
}

export default TestManagementPage;
