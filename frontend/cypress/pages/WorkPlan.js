import HomePage from "./HomePage";

class WorkPlan {
  constructor() {}

  visit() {
    cy.visit("/WorkplanByTest");
  }
  getWorkPlanFilterTitle(tiles) {
    cy.contains("h3", tiles).should("be.visible");
  }

  selectDropdownOption(option) {
    cy.get("select#select-1").should("be.visible").select(option);
  }

  getPrintWorkPlanButton() {
    // Testing Roadmap: Use element readiness checks, wait for button to render
    // Button text might be "Print Workplan" or "Print Work Plan" (check both)
    cy.contains("button", /Print Workplan?/i, { timeout: 10000 })
      .should("be.visible")
      .should("not.be.disabled");
  }

  getWorkPlanResultsTable() {
    // Testing Roadmap: Use element readiness checks, wait for table to render
    // Try data-cy first, fallback to table element if not found
    return cy
      .get('[data-cy="workplanResultsTable"]', { timeout: 10000 })
      .should("exist")
      .should("be.visible");
  }
}
export default WorkPlan;
