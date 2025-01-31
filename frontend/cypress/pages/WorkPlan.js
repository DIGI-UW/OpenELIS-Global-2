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
<<<<<<< HEAD
  getWorkPlanResultsTable() {
    return cy.get('[data-set-id="workplanResultsTable"]');
=======
  checkAndPrint() {
    cy.get("#includedCheck_0").check({ force: true });
>>>>>>> 9cd47466b (made modifications)
  }
}
export default WorkPlan;
