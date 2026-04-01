class Validation {
  checkForHeading() {
    cy.get("section > h3", { timeout: 15000 }).should(
      "contain.text",
      "Validation",
    );
  }

  selectTestUnit(unitType) {
    cy.get("#unitType").select(unitType);
  }

  validateTestUnit(unitType) {
    cy.get("[data-testid='sampleInfo']").should("contain.text", unitType);
  }

  enterLabNumberAndSearch(labNo) {
    cy.get("#accessionNumber").type(labNo);
    cy.get("[data-testid='Search-btn']").click();
  }

  saveResults() {
    // cy.get("[data-testid='Checkbox']").click();
    //cy.get("#resultList0\\.note").type(note);
    cy.get("[data-testid='Save-btn']").click();
  }
}

export default Validation;
