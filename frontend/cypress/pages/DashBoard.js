class DashBoardPage {
  //Updates

  checkFilters() {
    cy.get("#filterMyCases").check({ force: true });
  }

  selectSecreening() {
    cy.get("#statusFilter").select("Grossing");
  }

  selectReadyForPathologist() {
    cy.get("#statusFilter").select("Ready for Pathologist");
  }

  selectPreparingSlides() {
    cy.get("#statusFilter").select("Preparing slides");
  }
  typeLabNo() {
    cy.get("#search-input-41").type("1234");
  }

}

export default DashBoardPage;
