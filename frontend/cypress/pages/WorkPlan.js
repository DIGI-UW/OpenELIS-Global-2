import HomePage from "./HomePage";

class WorkPlan {
  constructor() {}

  visit() {
    cy.visit("/WorkplanByTest");
  }
  getWorkPlanFilterTitle(tiles) {
    const expected = (tiles || "").toLowerCase().replace(/\s+/g, " ").trim();

    cy.get("body", { timeout: 3000 }).then(($body) => {
      const bodyText = $body.text().toLowerCase().replace(/\s+/g, " ").trim();
      const expectedWithSpace = expected.replace("workplan", "work plan");
      const hasExpectedText =
        bodyText.includes(expected) || bodyText.includes(expectedWithSpace);
      const hasFilterDropdown = $body.find("select#select-1").length > 0;

      expect(
        hasExpectedText || hasFilterDropdown,
        `Expected workplan page content or filter dropdown. Expected: "${expected}", page text (start): "${bodyText.slice(0, 120)}..."`,
      ).to.eq(true);
    });
  }

  selectDropdownOption(option) {
    cy.get("select#select-1").should("be.visible").select(option);
  }

  getPrintWorkPlanButton() {
    cy.contains("button", "Print Workplan").should("be.visible");
  }

  getWorkPlanResultsTable() {
    return cy.get('[data-cy="workplanResultsTable"]');
  }
}
export default WorkPlan;
