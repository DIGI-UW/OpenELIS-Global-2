import HomePage from "./HomePage";

class WorkPlan {
  constructor() {}

  visit() {
    cy.visit("/WorkplanByTest");
  }

  getWorkPlanFilterTitle() {
    return cy.get("h3");
  }

  getTestTypeOrPanelSelector() {
    return cy.get("#select-1");
  }

  getPrintWorkPlanButton() {
    return cy.get("#print");
  }
  checkAndPrint() {
    cy.get("#includedCheck_0").check({ force: true });
  }
}
export default WorkPlan;
