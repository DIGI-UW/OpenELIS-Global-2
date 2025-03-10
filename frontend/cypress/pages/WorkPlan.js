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
    return cy.get("select#select-1");
  }

  getPrintWorkPlanButton() {
    return cy.get("button#print");
  }
  getWorkPlanResultsTable() {
    return cy.get('[data-cy="workplanResultsTable"]');
  }
}
export default WorkPlan;
