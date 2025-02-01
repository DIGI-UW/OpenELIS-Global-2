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
     cy.get("#print").click();
  }
  checkAndPrint() {
    cy.get("#includedCheck_0").check({ force: true });
  }
}
export default WorkPlan;
