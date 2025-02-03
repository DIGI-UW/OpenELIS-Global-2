import HomePage from "./HomePage";

class WorkPlan {
  constructor() {}

  visit() {
    cy.visit("/WorkplanByTest");
  }

  getWorkPlanFilterTitle() {
    return cy.get("h3");
  }

  getTestType(testName) {
    cy.get("#select-1").select(testName);
  }

  getTestPanel(panelType) {
    cy.get("#select-1").select(panelType);
  }

  getTestTypeUnit(unitType) {
    cy.get("#select-1").select(unitType);
  }

  getTestTypePriority(priority) {
    cy.get("#select-1").select(priority);
  }

  getPrintWorkPlanButton() {
    cy.get("#print").should("exist").should("be.visible").click();
  }

  getFinalPrintWorkPlanButton() {
    cy.get("#finalprint", { timeout: 10000 })
      .should("exist")
      .should("be.visible")
      .click();
  }

  checkToRemove() {
    cy.get("input#includedCheck_0", { timeout: 10000 })
      .first()
      .should("exist")
      .should("be.visible")
      .check();
  }
}
export default WorkPlan;
