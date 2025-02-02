class DashBoardPage {
  //Updates

  checkFilters() {
    cy.get("#filterMyCases").check({ force: true });
  }

  selectSecreening() {
    cy.get("#statusFilter").select("Screening");
  }

  selectReadyForPathology() {
    cy.get("#statusFilter").select("Ready for Pathology");
  }

  selectPreparingSlides() {
    cy.get("#statusFilter").select("Preparing Slides");
  }
  typeLabNo() {
    cy.get("#search-input-41").type("1234");
  }

}

export default DashBoardPage;
